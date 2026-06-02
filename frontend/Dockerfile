FROM node:20-alpine

WORKDIR /app

# Copiar archivos de dependencias
COPY package*.json ./

# Instalar dependencias
RUN npm install

# Copiar el resto del código
COPY . .

# Exponer el puerto 8100 (puerto por defecto de Ionic/Angular)
EXPOSE 8100

# Arrancar el servidor de desarrollo escuchando en todas las interfaces y sin verificar hosts
CMD ["npx", "ng", "serve", "--host", "0.0.0.0", "--port", "8100", "--disable-host-check"]
