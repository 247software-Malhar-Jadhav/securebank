# syntax=docker/dockerfile:1
#
# Multi-stage build for the SecureBank frontend.
#
# Stage 1 (build): install deps and run the Vite production build, producing static
# assets in /app/dist.
# Stage 2 (serve): copy ONLY those static assets into a tiny nginx image. The final
# image carries no Node runtime or node_modules — just nginx + the built files —
# which keeps it small and reduces attack surface.

# ---- Stage 1: build ---------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app

# Copy manifests first so Docker can cache the (slow) dependency install layer
# whenever only source files change.
COPY package*.json ./
RUN npm ci

# Now copy the rest of the source and build.
COPY . .
RUN npm run build

# ---- Stage 2: serve ---------------------------------------------------------
FROM nginx:1.27-alpine AS serve

# Our SPA + reverse-proxy config (see nginx.conf).
COPY nginx.conf /etc/nginx/conf.d/default.conf

# The compiled static assets.
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
# nginx in the foreground is the correct PID 1 for a container.
CMD ["nginx", "-g", "daemon off;"]
