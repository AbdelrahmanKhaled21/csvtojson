FROM gradle:8-jdk17 AS build
    WORKDIR /app
    COPY . .
    RUN gradle build --no-daemon -x test
    
FROM eclipse-temurin:17-jre
    WORKDIR /app
    COPY --from=build /app/build/libs/*.jar app.jar
    EXPOSE 8080
    CMD ["java", "-jar", "app.jar"]
