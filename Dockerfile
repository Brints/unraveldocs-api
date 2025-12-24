# Use Eclipse Temurin JDK 25 for building
FROM eclipse-temurin:25-jdk-alpine AS build

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

# Use Eclipse Temurin JRE 25 Alpine for smaller runtime image
FROM eclipse-temurin:25-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven wrapper and the project definition file from the build stage
COPY --from=build /app/target/*.jar UnravelDocs.jar

# Expose the port your application runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "UnravelDocs.jar"]