# ------------------------------------------------------------------------------
# Build Stage
# ------------------------------------------------------------------------------
FROM node:22-alpine AS builder
WORKDIR /app

# Copy dependency manifests for layer caching
COPY package*.json ./
RUN npm ci

# Copy source files and build production SPA bundle
COPY . .
RUN npm run build

# ------------------------------------------------------------------------------
# Runtime Stage
# ------------------------------------------------------------------------------
FROM nginx:alpine AS runner

# Copy compiled static assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration for SPA routing and API reverse-proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

