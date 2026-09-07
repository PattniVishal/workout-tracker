# syntax=docker/dockerfile:1

FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

FROM nginx:1.27-alpine AS runtime

ENV NGINX_BACKEND_HOST=backend
ENV NGINX_BACKEND_PORT=8080
ENV NGINX_LISTEN_PORT=80

COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.d/10-backend-upstream.sh /docker-entrypoint.d/10-backend-upstream.sh
RUN sed -i 's/\r$//' /docker-entrypoint.d/10-backend-upstream.sh \
    && chmod +x /docker-entrypoint.d/10-backend-upstream.sh

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD sh -c 'wget -q --spider "http://127.0.0.1:${PORT:-${NGINX_LISTEN_PORT:-80}}/actuator/health" || exit 1'
