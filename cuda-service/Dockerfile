# ─────────────────────────────────────────────────────
<<<<<<< HEAD
# Base: NVIDIA CUDA 12.2 runtime sobre Ubuntu 22.04
# La variante "runtime" incluye las libs CUDA necesarias
# para ejecutar kernels sin el toolkit completo (~3 GB menos)
# ─────────────────────────────────────────────────────
FROM nvidia/cuda:12.2.0-runtime-ubuntu22.04
=======
# CAMBIO CLAVE: Usamos 'devel' en lugar de 'runtime'.
# Esto incluye nvcc, cudaProfiler.h y todo el toolkit 
# necesario para instalar y ejecutar PyCUDA.
# ─────────────────────────────────────────────────────
FROM nvidia/cuda:12.2.0-devel-ubuntu22.04
>>>>>>> 661659fe00c77f5d8cfa7b89579d9480e250b2ad

ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1

<<<<<<< HEAD
WORKDIR /app

# ─────────────────────────────────────────────────────
# Sistema: Python 3.10, pip, compilador nvcc (para PyCUDA),
# OpenCV headless y curl (para el healthcheck)
=======
# Variables de entorno para que PyCUDA encuentre las librerías al compilar
ENV PATH=/usr/local/cuda/bin:${PATH}
ENV LD_LIBRARY_PATH=/usr/local/cuda/lib64:${LD_LIBRARY_PATH}
ENV CPATH=/usr/local/cuda/include:${CPATH}

WORKDIR /app

# ─────────────────────────────────────────────────────
# Sistema: Python 3.10, pip, OpenCV headless y curl.
# Ya no necesitamos instalar gcc, g++ ni nvcc manualmente
# porque la imagen 'devel' ya los incluye.
>>>>>>> 661659fe00c77f5d8cfa7b89579d9480e250b2ad
# ─────────────────────────────────────────────────────
RUN apt-get update && apt-get install -y --no-install-recommends \
        python3.10 \
        python3.10-dev \
        python3-pip \
<<<<<<< HEAD
        gcc \
        g++ \
        cuda-nvcc-12-2 \
=======
>>>>>>> 661659fe00c77f5d8cfa7b89579d9480e250b2ad
        libglib2.0-0 \
        libgl1 \
        curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Alias python
RUN ln -s /usr/bin/python3.10 /usr/bin/python

# ─────────────────────────────────────────────────────
# Dependencias Python
# ─────────────────────────────────────────────────────
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ─────────────────────────────────────────────────────
# Código fuente del servicio
# ─────────────────────────────────────────────────────
COPY . .

EXPOSE 8001

# Uvicorn arranca FastAPI en el puerto 8001
<<<<<<< HEAD
# 0.0.0.0 es obligatorio dentro de Docker
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
=======
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
>>>>>>> 661659fe00c77f5d8cfa7b89579d9480e250b2ad
