# ---- 1. asama: derleme (Node) ----
FROM node:22-alpine AS build
WORKDIR /app

# Once sadece bagimlilik dosyalari: kod degisince npm ci cache'ten gelir
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- 2. asama: sunum (Nginx) ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
