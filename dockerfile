# Use a Maven image for the build stage with Java 17
FROM maven:3.9.3-openjdk-17-slim AS build

# Set the working directory
WORKDIR /app

# Copy the Maven build files
COPY pom.xml ./
# If you're using Gradle, use the following line instead:
# COPY build.gradle ./

# Copy the source code
COPY src ./src

# Build the application (for Maven)
RUN mvn clean package -DskipTests

# Use a new base image for running the application with Java 17
FROM openjdk:17-jdk-slim

# Set the working directory for the final image
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/target/*.jar app.jar
# If you're using Gradle, use the following line instead:
# COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port that your application runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
