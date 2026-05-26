# ─────────────────────────────────────────────────────
# Base: NVIDIA CUDA 12.2 runtime sobre Ubuntu 22.04
# La variante "runtime" incluye las libs CUDA necesarias
# para ejecutar kernels sin el toolkit completo (~3 GB menos)
# ─────────────────────────────────────────────────────
FROM nvidia/cuda:12.2.0-runtime-ubuntu22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# ─────────────────────────────────────────────────────
# Sistema: Python 3.10, pip, compilador nvcc (para PyCUDA),
# OpenCV headless y curl (para el healthcheck)
# ─────────────────────────────────────────────────────
RUN apt-get update && apt-get install -y --no-install-recommends \
        python3.10 \
        python3.10-dev \
        python3-pip \
        gcc \
        g++ \
        cuda-nvcc-12-2 \
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
# 0.0.0.0 es obligatorio dentro de Docker
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
