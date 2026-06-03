"""
UPSGlam 3.0 — Servicio de filtros GPU (PyCUDA)
===============================================
Filtros implementados:
  1. Promedio       — convolución promediadora (box blur)
  2. Nitidez        — sharpening kernel personalizado
  3. Laplaciano     — detección de bordes laplaciana
  4. Mediana        — filtro de mediana (orden estadístico, preserva bordes)
  5. Sobel          — gradientes direccionales X/Y combinados
  6. Marca de agua  — blending con colores institucionales UPS (azul/amarillo)

Cada filtro devuelve un dict con las métricas técnicas requeridas por la rúbrica:
  filtro, tamaño_imagen, block_dim, grid_dim, total_hilos,
  tiempo_kernel_ms, estado
"""

import math
import time
import numpy as np
import cv2
import pycuda.driver as cuda
import pycuda.autoinit               # noqa: F401 — inicializa el contexto CUDA
from pycuda.compiler import SourceModule

# ──────────────────────────────────────────────────────────────────────────────
# KERNEL CUDA — un solo módulo con todos los kernels
# ──────────────────────────────────────────────────────────────────────────────
_KERNELS_SRC = r"""
/* ============================================================
   1. PROMEDIO (Box blur) — 3 canales, 1-D thread mapping
   ============================================================ */
__global__ void kernel_promedio(
    const unsigned char* input,
    unsigned char*       output,
    int width, int height,
    const float*         mask,
    int dim)
{
    int tid    = blockIdx.x * blockDim.x + threadIdx.x;
    int total  = width * height * 3;
    if (tid >= total) return;

    int c        = tid % 3;
    int pixel_id = tid / 3;
    int x        = pixel_id % width;
    int y        = pixel_id / width;
    int offset   = dim / 2;
    float suma   = 0.0f;

    for (int my = 0; my < dim; my++) {
        for (int mx = 0; mx < dim; mx++) {
            int ix = x + mx - offset;
            int iy = y + my - offset;
            if (ix >= 0 && ix < width && iy >= 0 && iy < height) {
                int vid = (iy * width * 3) + (ix * 3) + c;
                suma += input[vid] * mask[my * dim + mx];
            }
        }
    }
    int val  = (int)suma;
    if (val > 255) val = 255;
    if (val < 0)   val = 0;
    output[tid] = (unsigned char)val;
}

/* ============================================================
   2. NITIDEZ (Sharpening) — 3 canales, 2-D thread mapping
   ============================================================ */
__global__ void kernel_nitidez(
    const uchar3* entrada,
    uchar3*       salida,
    int filas, int cols,
    int K)
{
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;
    if (x >= cols || y >= filas) return;

    int offset = K / 2;
    float3 suma = make_float3(0.0f, 0.0f, 0.0f);

    for (int ky = -offset; ky <= offset; ky++) {
        for (int kx = -offset; kx <= offset; kx++) {
            float peso = (ky == 0 && kx == 0) ? (float)(K * K) : -1.0f;
            int cy = max(0, min(filas - 1, y + ky));
            int cx = max(0, min(cols  - 1, x + kx));
            uchar3 p = entrada[cy * cols + cx];
            suma.x += (float)p.x * peso;
            suma.y += (float)p.y * peso;
            suma.z += (float)p.z * peso;
        }
    }
    salida[y * cols + x].x = (unsigned char)fminf(fmaxf(suma.x, 0.0f), 255.0f);
    salida[y * cols + x].y = (unsigned char)fminf(fmaxf(suma.y, 0.0f), 255.0f);
    salida[y * cols + x].z = (unsigned char)fminf(fmaxf(suma.z, 0.0f), 255.0f);
}

/* ============================================================
   3. LAPLACIANO — escala de grises, 1-D thread mapping
   ============================================================ */
__global__ void kernel_laplaciano(
    const unsigned char* input,
    float*               output,
    int width, int height,
    const float*         mask,
    int ms)
{
    int tid = blockIdx.x * blockDim.x + threadIdx.x;
    if (tid >= width * height) return;

    int col    = tid % width;
    int row    = tid / width;
    int radius = ms / 2;
    float sum  = 0.0f;

    for (int ky = -radius; ky <= radius; ky++) {
        for (int kx = -radius; kx <= radius; kx++) {
            int r  = row + ky;
            int c  = col + kx;
            float px = 0.0f;
            if (r >= 0 && r < height && c >= 0 && c < width)
                px = (float)input[r * width + c];
            sum += px * mask[(ky + radius) * ms + (kx + radius)];
        }
    }
    output[tid] = sum;
}

/* ============================================================
   4. MEDIANA — ventana NxN en escala de grises con bubble-sort
      in-register (ventanas hasta 25 elementos / 5x5 recomendado)
      Para ventanas mayores se usa ventana_tam <= MAX_WIN
   ============================================================ */
#define MAX_WIN 121   /* máximo 11x11 */

__device__ void swap_f(float* a, float* b) {
    float t = *a; *a = *b; *b = t;
}

__global__ void kernel_mediana(
    const unsigned char* input,
    unsigned char*       output,
    int width, int height,
    int radio)
{
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;
    if (x >= width || y >= height) return;

    int   dim  = 2 * radio + 1;
    int   n    = dim * dim;
    float win[MAX_WIN];
    int   cnt  = 0;

    for (int ky = -radio; ky <= radio; ky++) {
        for (int kx = -radio; kx <= radio; kx++) {
            int r = y + ky;
            int c = x + kx;
            if (r >= 0 && r < height && c >= 0 && c < width)
                win[cnt++] = (float)input[r * width + c];
            else
                win[cnt++] = 0.0f;
        }
    }

    /* Ordenamiento burbuja in-register */
    for (int i = 0; i < cnt - 1; i++)
        for (int j = 0; j < cnt - 1 - i; j++)
            if (win[j] > win[j + 1])
                swap_f(&win[j], &win[j + 1]);

    output[y * width + x] = (unsigned char)win[cnt / 2];
}

/* ============================================================
   5. SOBEL — detección de bordes por gradiente (escala de grises)
      Gx y Gy calculados en paralelo, magnitud: sqrt(Gx^2+Gy^2)
   ============================================================ */
__global__ void kernel_sobel(
    const unsigned char* input,
    unsigned char*       output,
    int width, int height)
{
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;
    if (x >= width || y >= height) return;

    /* Kernels Sobel 3x3 fijos */
    const float Kx[3][3] = { {-1,  0,  1},
                               {-2,  0,  2},
                               {-1,  0,  1} };
    const float Ky[3][3] = { {-1, -2, -1},
                               { 0,  0,  0},
                               { 1,  2,  1} };
    float gx = 0.0f, gy = 0.0f;

    for (int ky = -1; ky <= 1; ky++) {
        for (int kx = -1; kx <= 1; kx++) {
            int r = max(0, min(height - 1, y + ky));
            int c = max(0, min(width  - 1, x + kx));
            float px = (float)input[r * width + c];
            gx += px * Kx[ky + 1][kx + 1];
            gy += px * Ky[ky + 1][kx + 1];
        }
    }
    float mag = sqrtf(gx * gx + gy * gy);
    output[y * width + x] = (unsigned char)fminf(mag, 255.0f);
}

/* ============================================================
   6. MARCA DE AGUA UPS — Superpone el logo institucional
      de la universidad (RGBA) con transparencia alfa sobre la
      imagen original en la esquina inferior derecha.
      alpha: opacidad global del logo (0.0 – 1.0, típico 0.85)
   ============================================================ */
__global__ void kernel_marca_agua_logo(
    const uchar3* input,
    uchar3*       output,
    const uchar4* logo,
    int width, int height,
    int logo_w, int logo_h,
    int logo_x, int logo_y,
    float alpha)
{
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;
    if (x >= width || y >= height) return;

    uchar3 src = input[y * width + x];
    uchar3 dst = src;

    if (x >= logo_x && x < logo_x + logo_w && y >= logo_y && y < logo_y + logo_h) {
        int lx = x - logo_x;
        int ly = y - logo_y;
        uchar4 logo_pixel = logo[ly * logo_w + lx];
        float a = (float)logo_pixel.w / 255.0f * alpha;
        dst.x = (unsigned char)fminf(a * logo_pixel.x + (1.0f - a) * src.x, 255.0f);
        dst.y = (unsigned char)fminf(a * logo_pixel.y + (1.0f - a) * src.y, 255.0f);
        dst.z = (unsigned char)fminf(a * logo_pixel.z + (1.0f - a) * src.z, 255.0f);
    }

    output[y * width + x] = dst;
}
"""

# Compilar una sola vez al importar el módulo
_mod = SourceModule(_KERNELS_SRC)

_fn_promedio    = _mod.get_function("kernel_promedio")
_fn_nitidez     = _mod.get_function("kernel_nitidez")
_fn_laplaciano  = _mod.get_function("kernel_laplaciano")
_fn_mediana     = _mod.get_function("kernel_mediana")
_fn_sobel       = _mod.get_function("kernel_sobel")
_fn_marca_agua  = _mod.get_function("kernel_marca_agua_logo")


# ──────────────────────────────────────────────────────────────────────────────
# UTILIDADES
# ──────────────────────────────────────────────────────────────────────────────
def _normalize_minmax(raw: np.ndarray) -> np.ndarray:
    vmin, vmax = raw.min(), raw.max()
    rng = vmax - vmin
    if rng < 1e-6:
        return np.zeros_like(raw, dtype=np.uint8)
    return np.clip((raw - vmin) / rng * 255.0, 0, 255).astype(np.uint8)


def _build_laplacian_mask(size: int) -> np.ndarray:
    total = size * size
    mask = np.full(total, -1.0, dtype=np.float32)
    mask[total // 2] = float(total - 1)
    return mask


def _gpu_time(evt_start: cuda.Event, evt_end: cuda.Event) -> float:
    """Devuelve el tiempo en milisegundos entre dos CUDA Events."""
    return evt_start.time_till(evt_end)


def _metrics(nombre: str, img: np.ndarray, block_dim: tuple, grid_dim: tuple, ms: float, ok: bool) -> dict:
    h, w = img.shape[:2]
    bx, by, bz = block_dim
    gx, gy, gz = grid_dim
    return {
        "filtro":              nombre,
        "tamaño_imagen":       f"{w}x{h}",
        "block_dim":           f"({bx},{by},{bz})",
        "grid_dim":            f"({gx},{gy},{gz})",
        "total_hilos":         bx * by * bz * gx * gy * gz,
        "tiempo_kernel_ms":    round(ms, 4),
        "estado":              "OK" if ok else "ERROR",
    }


# ──────────────────────────────────────────────────────────────────────────────
# FILTRO 1 — PROMEDIO
# ──────────────────────────────────────────────────────────────────────────────
def aplicar_promedio(img_bgr: np.ndarray, dim_mascara: int = 15) -> tuple[np.ndarray, dict]:
    """
    Box blur promediador sobre imagen BGR.
    dim_mascara: tamaño impar de la máscara (ej. 5, 15, 65).
    """
    assert img_bgr.ndim == 3 and img_bgr.shape[2] == 3
    h, w, c = img_bgr.shape
    total = w * h * c

    h_in   = np.ascontiguousarray(img_bgr.flatten(), dtype=np.uint8)
    h_out  = np.zeros_like(h_in)
    peso   = 1.0 / (dim_mascara ** 2)
    h_mask = np.full(dim_mascara ** 2, peso, dtype=np.float32)

    BLOCK = 256
    GRID  = math.ceil(total / BLOCK)
    block_dim = (BLOCK, 1, 1)
    grid_dim  = (GRID,  1, 1)

    ev0, ev1 = cuda.Event(), cuda.Event()
    cuda.Context.synchronize()
    ev0.record()

    _fn_promedio(
        cuda.In(h_in), cuda.Out(h_out),
        np.int32(w), np.int32(h),
        cuda.In(h_mask), np.int32(dim_mascara),
        block=block_dim, grid=grid_dim
    )

    ev1.record(); ev1.synchronize()
    ms = _gpu_time(ev0, ev1)

    resultado = h_out.reshape(h, w, c)
    return resultado, _metrics("Promedio", img_bgr, block_dim, grid_dim, ms, True)


# ──────────────────────────────────────────────────────────────────────────────
# FILTRO 2 — NITIDEZ
# ──────────────────────────────────────────────────────────────────────────────
def aplicar_nitidez(img_bgr: np.ndarray, K: int = 5) -> tuple[np.ndarray, dict]:
    """
    Sharpening con kernel KxK. K impar (ej. 3, 5, 7).
    Internamente trabaja con uchar3 (RGB).
    """
    assert img_bgr.ndim == 3 and img_bgr.shape[2] == 3
    img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
    h, w    = img_rgb.shape[:2]

    h_in  = np.ascontiguousarray(img_rgb, dtype=np.uint8)
    h_out = np.empty_like(h_in)

    BLOCK = (16, 16, 1)
    GRID  = (math.ceil(w / 16), math.ceil(h / 16), 1)

    d_in  = cuda.mem_alloc(h_in.nbytes)
    d_out = cuda.mem_alloc(h_out.nbytes)
    cuda.memcpy_htod(d_in, h_in)

    ev0, ev1 = cuda.Event(), cuda.Event()
    cuda.Context.synchronize()
    ev0.record()

    _fn_nitidez(
        d_in, d_out,
        np.int32(h), np.int32(w), np.int32(K),
        block=BLOCK, grid=GRID
    )

    ev1.record(); ev1.synchronize()
    ms = _gpu_time(ev0, ev1)
    cuda.memcpy_dtoh(h_out, d_out)
    d_in.free(); d_out.free()

    resultado_rgb = h_out.reshape(h, w, 3)
    resultado_bgr = cv2.cvtColor(resultado_rgb, cv2.COLOR_RGB2BGR)
    return resultado_bgr, _metrics("Nitidez", img_bgr, BLOCK, GRID, ms, True)


# ──────────────────────────────────────────────────────────────────────────────
# FILTRO 3 — LAPLACIANO
# ──────────────────────────────────────────────────────────────────────────────
def aplicar_laplaciano(img_bgr: np.ndarray, ms: int = 5) -> tuple[np.ndarray, dict]:
    """
    Detección de bordes laplaciana en escala de grises.
    ms: tamaño de la máscara (impar).
    """
    img_gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    h, w     = img_gray.shape
    npix     = w * h

    mask_flat = _build_laplacian_mask(ms)

    d_in  = cuda.mem_alloc(img_gray.nbytes)
    d_out = cuda.mem_alloc(npix * np.dtype(np.float32).itemsize)
    cuda.memcpy_htod(d_in,  img_gray)

    d_mask = cuda.mem_alloc(mask_flat.nbytes)
    cuda.memcpy_htod(d_mask, mask_flat)

    BLOCK = (256, 1, 1)
    GRID  = (math.ceil(npix / 256), 1, 1)

    ev0, ev1 = cuda.Event(), cuda.Event()
    cuda.Context.synchronize()
    ev0.record()

    _fn_laplaciano(
        d_in, d_out,
        np.int32(w), np.int32(h),
        d_mask, np.int32(ms),
        block=BLOCK, grid=GRID
    )

    ev1.record(); ev1.synchronize()
    tiempo_ms = _gpu_time(ev0, ev1)

    raw = np.empty(npix, dtype=np.float32)
    cuda.memcpy_dtoh(raw, d_out)
    raw = raw.reshape(h, w)
    d_in.free(); d_out.free(); d_mask.free()

    gray_out  = _normalize_minmax(raw)
    resultado = cv2.cvtColor(gray_out, cv2.COLOR_GRAY2BGR)
    return resultado, _metrics("Laplaciano", img_bgr, BLOCK, GRID, tiempo_ms, True)


# ──────────────────────────────────────────────────────────────────────────────
# FILTRO 4 — MEDIANA
# ──────────────────────────────────────────────────────────────────────────────
def aplicar_mediana(img_bgr: np.ndarray, radio: int = 2) -> tuple[np.ndarray, dict]:
    """
    Filtro de mediana (preserva bordes) con ventana (2*radio+1)x(2*radio+1).
    radio=2 → ventana 5x5 (25 elementos). Máx radio=5 → 11x11=121 elem.
    Se aplica canal a canal para conservar color.
    """
    assert radio >= 1 and (2 * radio + 1) ** 2 <= 121, "radio máximo = 5"
    h, w, c = img_bgr.shape

    canales_out = []
    BLOCK = (16, 16, 1)
    GRID  = (math.ceil(w / 16), math.ceil(h / 16), 1)

    tiempos = []
    for ch in range(c):
        canal    = np.ascontiguousarray(img_bgr[:, :, ch], dtype=np.uint8)
        canal_out = np.empty_like(canal)

        d_in  = cuda.mem_alloc(canal.nbytes)
        d_out = cuda.mem_alloc(canal_out.nbytes)
        cuda.memcpy_htod(d_in, canal)

        ev0, ev1 = cuda.Event(), cuda.Event()
        cuda.Context.synchronize()
        ev0.record()

        _fn_mediana(
            d_in, d_out,
            np.int32(w), np.int32(h), np.int32(radio),
            block=BLOCK, grid=GRID
        )

        ev1.record(); ev1.synchronize()
        tiempos.append(_gpu_time(ev0, ev1))

        cuda.memcpy_dtoh(canal_out, d_out)
        d_in.free(); d_out.free()
        canales_out.append(canal_out)

    resultado = np.stack(canales_out, axis=2)
    ms_total  = sum(tiempos)
    return resultado, _metrics("Mediana", img_bgr, BLOCK, GRID, ms_total, True)


# ──────────────────────────────────────────────────────────────────────────────
# FILTRO 5 — SOBEL
# ──────────────────────────────────────────────────────────────────────────────
def aplicar_sobel(img_bgr: np.ndarray) -> tuple[np.ndarray, dict]:
    """
    Gradientes Sobel en escala de grises.
    Devuelve imagen BGR (3 canales) para uniformidad de la API.
    """
    img_gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    h, w     = img_gray.shape

    h_in  = np.ascontiguousarray(img_gray, dtype=np.uint8)
    h_out = np.empty_like(h_in)

    d_in  = cuda.mem_alloc(h_in.nbytes)
    d_out = cuda.mem_alloc(h_out.nbytes)
    cuda.memcpy_htod(d_in, h_in)

    BLOCK = (16, 16, 1)
    GRID  = (math.ceil(w / 16), math.ceil(h / 16), 1)

    ev0, ev1 = cuda.Event(), cuda.Event()
    cuda.Context.synchronize()
    ev0.record()

    _fn_sobel(
        d_in, d_out,
        np.int32(w), np.int32(h),
        block=BLOCK, grid=GRID
    )

    ev1.record(); ev1.synchronize()
    ms = _gpu_time(ev0, ev1)

    cuda.memcpy_dtoh(h_out, d_out)
    d_in.free(); d_out.free()

    resultado = cv2.cvtColor(h_out.reshape(h, w), cv2.COLOR_GRAY2BGR)
    return resultado, _metrics("Sobel", img_bgr, BLOCK, GRID, ms, True)


# ──────────────────────────────────────────────────────────────────────────────
# FILTRO 6 — MARCA DE AGUA UPS
# ──────────────────────────────────────────────────────────────────────────────
def aplicar_marca_agua_ups(img_bgr: np.ndarray,
                            alpha: float = 0.85) -> tuple[np.ndarray, dict]:
    
    import cairosvg
    import io
    from PIL import Image

    assert img_bgr.ndim == 3 and img_bgr.shape[2] == 3
    img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
    h, w    = img_rgb.shape[:2]

    # 1. Renderizar y redimensionar el logotipo SVG
    try:
        png_data = cairosvg.svg2png(url="logoupscolor.svg")
        logo_pil = Image.open(io.BytesIO(png_data)).convert("RGBA")
    except Exception as e:
        print(f"Error al cargar SVG: {e}, usando fallback")
        # Fallback si no encuentra el archivo: logo en blanco transparente
        logo_pil = Image.new("RGBA", (100, 100), (255, 255, 255, 128))

    # Redimensionar el logo al 25% del ancho de la imagen destino
    logo_w = max(10, int(w * 0.25))
    logo_h = max(10, int(logo_w * logo_pil.height / logo_pil.width))
    logo_pil = logo_pil.resize((logo_w, logo_h), Image.Resampling.LANCZOS)
    
    # Convertir logo a numpy array de tipo uint8 (RGBA)
    h_logo = np.ascontiguousarray(logo_pil, dtype=np.uint8)

    # 2. Definir coordenadas de la esquina inferior derecha con un pequeño margen
    margin = 16
    logo_x = max(0, w - logo_w - margin)
    logo_y = max(0, h - logo_h - margin)

    # 3. Preparar memoria CUDA
    h_in  = np.ascontiguousarray(img_rgb, dtype=np.uint8)
    h_out = np.empty_like(h_in)

    d_in   = cuda.mem_alloc(h_in.nbytes)
    d_out  = cuda.mem_alloc(h_out.nbytes)
    d_logo = cuda.mem_alloc(h_logo.nbytes)

    cuda.memcpy_htod(d_in, h_in)
    cuda.memcpy_htod(d_logo, h_logo)

    BLOCK = (16, 16, 1)
    GRID  = (math.ceil(w / 16), math.ceil(h / 16), 1)

    ev0, ev1 = cuda.Event(), cuda.Event()
    cuda.Context.synchronize()
    ev0.record()

    # Llamar al nuevo kernel
    _fn_marca_agua(
        d_in, d_out, d_logo,
        np.int32(w), np.int32(h),
        np.int32(logo_w), np.int32(logo_h),
        np.int32(logo_x), np.int32(logo_y),
        np.float32(alpha),
        block=BLOCK, grid=GRID
    )

    ev1.record(); ev1.synchronize()
    ms = _gpu_time(ev0, ev1)

    cuda.memcpy_dtoh(h_out, d_out)
    d_in.free(); d_out.free(); d_logo.free()

    resultado_rgb = h_out.reshape(h, w, 3)
    resultado_bgr = cv2.cvtColor(resultado_rgb, cv2.COLOR_RGB2BGR)
    return resultado_bgr, _metrics("MarcaAguaUPS", img_bgr, BLOCK, GRID, ms, True)


# ──────────────────────────────────────────────────────────────────────────────
# DEMO / PRUEBA LOCAL
# ──────────────────────────────────────────────────────────────────────────────
FILTROS = {
    "promedio":    lambda img: aplicar_promedio(img, dim_mascara=15),
    "nitidez":     lambda img: aplicar_nitidez(img, K=5),
    "laplaciano":  lambda img: aplicar_laplaciano(img, ms=5),
    "mediana":     lambda img: aplicar_mediana(img, radio=2),
    "sobel":       lambda img: aplicar_sobel(img),
    "marca_agua":  lambda img: aplicar_marca_agua_ups(img, alpha=0.85),
}

if __name__ == "__main__":
    import sys, os

    ruta = sys.argv[1] if len(sys.argv) > 1 else "flor.jpg"
    img  = cv2.imread(ruta, cv2.IMREAD_COLOR)
    if img is None:
        print(f"Error: no se pudo leer '{ruta}'"); sys.exit(1)

    h, w = img.shape[:2]
    print("=" * 70)
    print(f"  UPSGlam — Ejecución de los 6 filtros GPU sobre: {ruta} ({w}x{h})")
    print("=" * 70)
    print(f"{'Filtro':<15} {'Block':<14} {'Grid':<14} {'Hilos':>12} {'ms':>10}  Estado")
    print("-" * 70)

    for nombre, fn in FILTROS.items():
        resultado, m = fn(img)
        out_path = f"resultado_{nombre}.jpg"
        cv2.imwrite(out_path, resultado)
        print(
            f"{m['filtro']:<15} {m['block_dim']:<14} {m['grid_dim']:<14} "
            f"{m['total_hilos']:>12,} {m['tiempo_kernel_ms']:>10.3f}  {m['estado']}"
        )

    print("-" * 70)
    print("Imágenes guardadas como resultado_<filtro>.jpg")