# =============================================================================
# Nombre: Dockerfile (frontend)
# Descripción: Compila la SPA React y la sirve con Nginx sobre HTTPS (TLS 1.2/1.3).
#              En producción reemplace el certificado autofirmado montando
#              /etc/nginx/certs/sigif.crt y sigif.key.
# Fecha de creación: 21/10/2026
# Última actualización: 21/10/2026
# Autor: Equipo SIGIF
# =============================================================================
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
RUN apk add --no-cache openssl \
 && mkdir -p /etc/nginx/certs \
 && openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
      -subj "/CN=localhost/O=SIGIF" \
      -keyout /etc/nginx/certs/sigif.key -out /etc/nginx/certs/sigif.crt
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80 443
