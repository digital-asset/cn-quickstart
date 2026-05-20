# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS build
WORKDIR /app
COPY backend-js/package.json backend-js/package-lock.json ./
COPY backend-js/generated/ ./generated/
RUN npm ci
# Vendored token-standard specs live under backend/. Copy them into a stable in-image path
# so prebuild can find them; the host script uses the same relative path via a symlink.
COPY backend/src/main/resources/vendored/ ./vendored/
COPY backend-js/ ./
RUN VENDORED_DIR=./vendored npm run build

FROM node:22-alpine AS runtime
WORKDIR /app
RUN apk add --no-cache wget
COPY --from=build /app/package.json /app/package-lock.json ./
COPY --from=build /app/generated/ ./generated/
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist
COPY docker/backend-js/start.sh /app/start.sh
RUN chmod +x /app/start.sh
EXPOSE 8080
CMD ["/app/start.sh"]
