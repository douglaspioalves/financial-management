# Build stage
FROM node:20-alpine AS builder

WORKDIR /app

# Copiar manifesto de dependências
COPY package.json package-lock.json* ./

# Instalar dependências (usa npm ci se lock file existir, npm install caso contrário)
RUN npm install --legacy-peer-deps

# Copiar código-fonte e compilar para produção
COPY . .
RUN npm run build -- --configuration production

# Runtime stage — nginx para servir o build estático
FROM nginx:alpine

# Remover configuração padrão do nginx
RUN rm /etc/nginx/conf.d/default.conf

# Copiar configuração customizada do nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar artefatos da build Angular
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 4200

CMD ["nginx", "-g", "daemon off;"]
