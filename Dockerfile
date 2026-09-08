FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /app

COPY gradle ./gradle
COPY gradlew .
COPY build.gradle .

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src ./src

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]