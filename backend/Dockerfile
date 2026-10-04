# ---- 1. asama: derleme (JDK + Maven Wrapper) ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

# Once sadece bagimlilik dosyalari: src degisince bu katman cache'ten gelir
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---- 2. asama: calistirma (sadece JRE) ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S wallet && adduser -S wallet -G wallet
USER wallet

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
