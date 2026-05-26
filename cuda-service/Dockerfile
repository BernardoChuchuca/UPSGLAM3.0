# ─────────────────────────────────────────────────────
# CAMBIO CLAVE: Usamos 'devel' en lugar de 'runtime'.
# Esto incluye nvcc, cudaProfiler.h y todo el toolkit 
# necesario para instalar y ejecutar PyCUDA.
# ─────────────────────────────────────────────────────
FROM nvidia/cuda:12.2.0-devel-ubuntu22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1

# Variables de entorno para que PyCUDA encuentre las librerías al compilar
ENV PATH=/usr/local/cuda/bin:${PATH}
ENV LD_LIBRARY_PATH=/usr/local/cuda/lib64:${LD_LIBRARY_PATH}
ENV CPATH=/usr/local/cuda/include:${CPATH}

WORKDIR /app

# ─────────────────────────────────────────────────────
# Sistema: Python 3.10, pip, OpenCV headless y curl.
# Ya no necesitamos instalar gcc, g++ ni nvcc manualmente
# porque la imagen 'devel' ya los incluye.
# ─────────────────────────────────────────────────────
RUN apt-get update && apt-get install -y --no-install-recommends \
        python3.10 \
        python3.10-dev \
        python3-pip \
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
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]