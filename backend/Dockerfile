FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
# Install native dependencies for OpenCV/JavaCV (glibc environment)
RUN apt-get update && apt-get install -y \
    libgomp1 \
    libquadmath0 \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Copy the cascade classifier to the root so the service can find it
COPY --from=build /app/src/main/resources/haarcascade_frontalface_default.xml .

EXPOSE 8080
# Use environment variables for JVM settings to avoid OOM on Render's 512MB RAM
ENTRYPOINT ["java", "-Xmx300m", "-Xms128m", "-jar", "app.jar"]