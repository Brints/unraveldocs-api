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

# Install Tesseract and its C++ libraries here
RUN apt-get update && \
    apt-get install -y tesseract-ocr libtesseract-dev && \
    rm -rf /var/lib/apt/lists/*

# Copy the packaged JAR from the build stage
COPY --from=build /app/target/*.jar UnravelDocs.jar

# Expose the port your application runs on
EXPOSE 8080

# Set production profile by default
ENV SPRING_PROFILES_ACTIVE=production

# Command to run the application
ENTRYPOINT ["java", "-jar", "UnravelDocs.jar"]