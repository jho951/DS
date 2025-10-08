# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# Gradle Wrapper/빌드파일/소스만 복사 (캐시 최적화)
COPY gradlew /workspace/gradlew
COPY gradle /workspace/gradle
COPY settings.gradle /workspace/settings.gradle
COPY build.gradle /workspace/build.gradle
COPY src /workspace/src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
