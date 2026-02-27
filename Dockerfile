# Use Eclipse Temurin JDK 25 for building
FROM eclipse-temurin:25-jdk AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven wrapper and the project definition file
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Make the Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies. This layer is cached if pom.xml doesn't change.
RUN ./mvnw dependency:go-offline

# Copy the rest of your application's source code
COPY src ./src

# Package the application into a JAR file
RUN ./mvnw package -DskipTests && ls -la /app/target/

# ==========================================
# RUNTIME STAGE
# ==========================================
# Use Eclipse Temurin JRE 25 for runtime
FROM eclipse-temurin:25-jre

# Set the working directory inside the container
WORKDIR /app

# Install Tesseract OCR engine and English language data (libtesseract-dev not needed at runtime)
RUN apt-get update && \
    apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/* && \
    echo "--- Tesseract verification ---" && \
    tesseract --version && \
    find /usr/share/tesseract-ocr -name "eng.traineddata" 2>/dev/null && \
    echo "--- End verification ---"

# Copy the packaged JAR from the build stage
COPY --from=build /app/target/*.jar UnravelDocs.jar

# Expose the port your application runs on
EXPOSE 8080

# Set production profile by default
ENV SPRING_PROFILES_ACTIVE=production

# Command to run the application
ENTRYPOINT ["java", "-jar", "UnravelDocs.jar"]