FROM maven:3.8.4-openjdk-21-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Copy the cascade file to the root of the app
COPY haarcascade_frontalface_default.xml .
# Create face_data directory
RUN mkdir -p face_data
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar"]
