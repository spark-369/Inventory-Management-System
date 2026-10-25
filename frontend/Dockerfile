# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage — type-checks and bundles the SPA with Vite.
# ---------------------------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /app

# Copy the manifests first so `npm ci` is cached until dependencies change.
COPY package.json package-lock.json ./
RUN npm ci

# Then the sources and configs the build needs. `public/` holds files copied
# verbatim into the bundle (e.g. favicon.svg referenced by index.html).
COPY tsconfig.json tsconfig.app.json tsconfig.node.json vite.config.ts index.html ./
COPY public/ public/
COPY src/ src/

# `npm run build` runs `tsc -b && vite build`, so the image fails to build on a
# type error rather than shipping a broken bundle.
RUN npm run build

# ---------------------------------------------------------------------------
# Runtime stage — static files served by nginx, with SPA routing + API proxying.
# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# The server config is written inline (heredoc) to keep this integration to a
# single Dockerfile per app. It does three things:
#   1. serves the built SPA and falls back to index.html for client-side routes;
#   2. proxies /api to the backend container, so the browser stays on one origin
#      and no CORS preflight is needed in production;
#   3. caches hashed assets aggressively while never caching index.html.
RUN <<'NGINX_CONF' cat > /etc/nginx/conf.d/default.conf
server {
    listen       80;
    server_name  _;

    root  /usr/share/nginx/html;
    index index.html;

    # Compress text assets on the fly.
    gzip              on;
    gzip_vary         on;
    gzip_min_length   1024;
    gzip_proxied      any;
    gzip_types        text/plain text/css application/javascript application/json
                      image/svg+xml application/wasm;

    # Vite emits content-hashed filenames, so these can be cached forever.
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        try_files $uri =404;
    }

    # The API is proxied to the backend service. The container name/IP is
    # resolved by Docker's embedded DNS on the shared network.
    location /api/ {
        proxy_pass         http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    # Client-side routing: unknown paths must return the app shell, never a 404.
    location / {
        try_files $uri $uri/ /index.html;
        add_header Cache-Control "no-cache";
    }
}
NGINX_CONF

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
