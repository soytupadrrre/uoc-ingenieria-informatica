FROM oven/bun:1.2.4

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el contenido completo de la carpeta app dentro del contenedor
COPY app/ .

# Instalamos dependencias con Bun
RUN bun install

# Si la app escucha en un puerto, exponerlo (ajusta si necesario)
EXPOSE 3000

# Ejecutar la aplicación
CMD ["bun", "run", "src/app.ts"]
