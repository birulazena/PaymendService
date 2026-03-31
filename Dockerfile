FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

RUN chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon

RUN --mount=type=cache,target=/root/.gradle ./gradlew clean build -x test --no-daemon

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --from=builder /app/build/libs/PaymentService-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]