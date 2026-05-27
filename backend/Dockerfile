# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar arquivos de configuração do Maven primeiro (cache de dependências)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Baixar dependências (cache layer separado)
RUN ./mvnw dependency:go-offline -B

# Copiar código-fonte e compilar
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Criar usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
