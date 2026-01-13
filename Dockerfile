# Build Stage
FROM gradle:jdk21-jammy AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Run Stage
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
