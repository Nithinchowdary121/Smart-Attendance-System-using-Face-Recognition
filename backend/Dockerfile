FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -Djavacpp.platform=linux-x86_64
COPY src ./src
RUN mvn clean package -DskipTests -Djavacpp.platform=linux-x86_64

FROM eclipse-temurin:21-jre
ENV DEBIAN_FRONTEND=noninteractive
# Install essential native dependencies for OpenCV
# libglib2.0-0, libsm6, libxext6, libxrender1 are required by the highgui module 
# even if not explicitly used, as JavaCV loads it by default.
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    libquadmath0 \
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
# Adjusted for Render's 512MB limit: 
# -Xmx256m leaves room for native memory used by OpenCV.
# UseSerialGC is more efficient for single-core/low-memory environments.
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseSerialGC", "-XX:MaxDirectMemorySize=128m", "-jar", "app.jar"]