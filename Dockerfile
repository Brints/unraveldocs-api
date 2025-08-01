# Use a base image with Java 21 installed
FROM openjdk:21-jdk-slim

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
RUN ./mvnw package -DskipTests

# Expose the port your application runs on
EXPOSE 8080

# Copy the generated JAR file to the container
COPY target/UnravelDocs.jar UnravelDocs.jar

# Command to run the application
ENTRYPOINT ["java", "-jar", "UnravelDocs.jar"]