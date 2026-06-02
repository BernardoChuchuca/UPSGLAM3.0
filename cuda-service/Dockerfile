# Base: NVIDIA CUDA 12.2 devel sobre Ubuntu 22.04
FROM nvidia/cuda:12.8.0-devel-ubuntu22.04

# Forzar a que APT y el instalador corran en modo silencioso (Sin preguntas de S/N)
ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1

# Banderas de escape para que g++ ignore advertencias obsoletas dentro de Docker
ENV CFLAGS="-Wno-deprecated-declarations -Wno-error"
ENV CXXFLAGS="-Wno-deprecated-declarations -Wno-error"

# Variables de entorno para que PyCUDA encuentre las librerías al compilar
ENV PATH=/usr/local/cuda/bin:${PATH}
ENV LD_LIBRARY_PATH=/usr/local/cuda/lib64:${LD_LIBRARY_PATH}
ENV CPATH=/usr/local/cuda/include:${CPATH}

WORKDIR /app

# Sistema: Python 3.10, pip, OpenCV headless y curl.
# Usamos -y obligatorio para aceptar todo automáticamente
RUN apt-get update && apt-get install -y --no-install-recommends \
        python3.10 \
        python3.10-dev \
        python3-pip \
        libglib2.0-0 \
        libgl1 \
        curl \
        libcairo2 \
        pkg-config \
        libcairo2-dev \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Alias python
RUN ln -s /usr/bin/python3.10 /usr/bin/python

# Dependencias Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Código fuente del servicio
COPY . .

EXPOSE 8001

# Uvicorn arranca FastAPI en el puerto 8001
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]