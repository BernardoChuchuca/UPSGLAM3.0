"""
UPSGlam 3.0 — Servicio API REST reactivo (FastAPI + PyCUDA)
===========================================================
Este módulo expone los servicios de procesamiento digital de imágenes en GPU.
Se integra de manera nativa con el Healthcheck de Docker Compose y mapea
las peticiones entrantes hacia el archivo de lógica de hardware kernel.py.
"""

import base64
from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Query
import numpy as np
import cv2
import kernel

app = FastAPI(
    title="UPSGlam 3.0 - Motor de Procesamiento GPU",
    description="API REST de alta velocidad para la aplicación de filtros mediante kernels paralelos en CUDA"
)

# ──────────────────────────────────────────────────────────────────────────────
# ENDPOINT DE SALUD (Requerido para el Healthcheck de Docker Compose)
# ──────────────────────────────────────────────────────────────────────────────
@app.get("/health")
def health_check():
    """
    Ruta de control utilizada por el orquestador de contenedores.
    Retorna estado HTTP 200 cuando el contexto CUDA y FastAPI están activos.
    """
    return {"status": "healthy", "service": "upsglam-cuda-service"}


# ──────────────────────────────────────────────────────────────────────────────
# LOGICA DE PROCESAMIENTO COMUN
# ──────────────────────────────────────────────────────────────────────────────
def procesar_logica_comun(
    img_bgr: np.ndarray, 
    filtro_nombre: str, 
    param_dim: int = None, 
    radio: int = 2, 
    alpha: float = 0.40, 
    banda_pct: float = 0.12
):
    # 1. Normalizar y verificar la existencia del filtro solicitado
    filtro_key = filtro_nombre.lower().strip()
    if filtro_key == "marca_agua_ups":
        filtro_key = "marca_agua"

    if filtro_key not in kernel.FILTROS:
        raise HTTPException(
            status_code=404, 
            detail=f"Filtro '{filtro_nombre}' no soportado. Filtros válidos: {list(kernel.FILTROS.keys())} o 'marca_agua_ups'"
        )

    # 2. Mapear dinámicamente el flujo hacia las funciones de hardware de tu kernel.py
    try:
        if filtro_key == "promedio":
            dim = param_dim if param_dim is not None else 15
            resultado, metricas = kernel.aplicar_promedio(img_bgr, dim_mascara=dim)
            
        elif filtro_key == "nitidez":
            dim = param_dim if param_dim is not None else 5
            resultado, metricas = kernel.aplicar_nitidez(img_bgr, K=dim)
            
        elif filtro_key == "laplaciano":
            dim = param_dim if param_dim is not None else 5
            resultado, metricas = kernel.aplicar_laplaciano(img_bgr, ms=dim)
            
        elif filtro_key == "mediana":
            resultado, metricas = kernel.aplicar_mediana(img_bgr, radio=radio)
            
        elif filtro_key == "sobel":
            resultado, metricas = kernel.aplicar_sobel(img_bgr)
            
        elif filtro_key == "marca_agua":
            resultado, metricas = kernel.aplicar_marca_agua_ups(img_bgr, alpha=alpha)
            
        else:
            resultado, metricas = kernel.FILTROS[filtro_key](img_bgr)

        # 3. Codificar la imagen procesada de salida de OpenCV a un búfer JPEG
        success, encoded_img = cv2.imencode(".jpg", resultado)
        if not success:
            raise HTTPException(status_code=500, detail="Error crítico al codificar la matriz de píxeles a JPEG.")
        
        # Convertir a cadena de caracteres Base64 estándar para transferencia segura
        base64_data = base64.b64encode(encoded_img).decode("utf-8")
        h, w = img_bgr.shape[:2]

        # 4. Responder con la estructura plana limpia que espera CudaProcessingResponse.java
        return {
            "image_base64": base64_data,
            "filter_name": metricas["filtro"],
            "width": w,
            "height": h,
            "block_dim": metricas["block_dim"],
            "grid_dim": metricas["grid_dim"],
            "total_threads": metricas["total_hilos"],
            "kernel_time_ms": metricas["tiempo_kernel_ms"],
            "status": "success" if metricas["estado"] == "OK" else "error"
        }

    except Exception as error:
        raise HTTPException(
            status_code=500, 
            detail=f"Fallo crítico en la segmentación o ejecución en hardware Device: {str(error)}"
        )


# ──────────────────────────────────────────────────────────────────────────────
# ENDPOINT MULTIPART CON RUTA PATH (Para pruebas generales)
# ──────────────────────────────────────────────────────────────────────────────
@app.post("/procesar/{filtro_nombre}")
async def procesar_imagen(
    filtro_nombre: str, 
    file: UploadFile = File(...),
    param_dim: int = Query(None, description="Dimensión impar para máscaras de convolución (ej: 15, 65, 129)"),
    radio: int = Query(2, description="Radio de la ventana para el filtro de Mediana (máximo 5)"),
    alpha: float = Query(0.40, description="Intensidad de opacidad (alpha-blending) para colores UPS"),
    banda_pct: float = Query(0.12, description="Fracción de altura ocupada por la banda institucional")
):
    try:
        contents = await file.read()
        nparr = np.frombuffer(contents, np.uint8)
        img_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    except Exception:
        raise HTTPException(
            status_code=400, 
            detail="El payload binario es inválido o no se pudo decodificar como imagen."
        )

    if img_bgr is None:
        raise HTTPException(status_code=400, detail="Formato de imagen corrupto o no soportado por OpenCV.")

    return procesar_logica_comun(img_bgr, filtro_nombre, param_dim, radio, alpha, banda_pct)


# ──────────────────────────────────────────────────────────────────────────────
# ENDPOINT DE COMPATIBILIDAD CON EL BACKEND (CudaService.java)
# ──────────────────────────────────────────────────────────────────────────────
@app.post("/filter")
async def filter_image(
    file: UploadFile = File(...),
    filter_name: str = Form(...)
):
    """
    Ruta requerida por CudaService.java que envía la imagen y el nombre del filtro
    en formato multipart form-data.
    """
    try:
        contents = await file.read()
        nparr = np.frombuffer(contents, np.uint8)
        img_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    except Exception:
        raise HTTPException(
            status_code=400, 
            detail="El payload binario es inválido o no se pudo decodificar como imagen."
        )

    if img_bgr is None:
        raise HTTPException(status_code=400, detail="Formato de imagen corrupto o no soportado por OpenCV.")

    return procesar_logica_comun(img_bgr, filter_name)