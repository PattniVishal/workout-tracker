#!/bin/sh
set -eu

# Defaults match Docker Compose (service name + internal port).
export NGINX_BACKEND_PROTOCOL="${NGINX_BACKEND_PROTOCOL:-http}"
export NGINX_BACKEND_HOST="${NGINX_BACKEND_HOST:-backend}"
export NGINX_BACKEND_PORT="${NGINX_BACKEND_PORT:-8080}"

# Render and other PaaS hosts inject PORT; Compose uses 80 inside the container.
export NGINX_LISTEN_PORT="${PORT:-${NGINX_LISTEN_PORT:-80}}"
