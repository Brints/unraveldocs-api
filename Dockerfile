# Build stage
FROM openjdk:21-jdk-slim AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests && ls -la /app/target/

# Runtime stage
FROM openjdk:21-jdk-slim
WORKDIR /app

# Install Tesseract OCR and language data
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr tesseract-ocr-eng \
  && rm -rf /var/lib/apt/lists/*

# Copy app
COPY --from=build /app/target/*.jar UnravelDocs.jar

# Tesseract data path
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata
# Keep JVM memory within dyno limits
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC"

# Heroku provides PORT; bind server to it
CMD sh -c 'java $JAVA_OPTS -Dserver.port=${PORT:-8080} -Dspring.profiles.active=heroku -jar UnravelDocs.jar'