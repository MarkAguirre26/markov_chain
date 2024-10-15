# Use a base image that includes Java 21
FROM openjdk:21-jdk-slim as build

# Set the working directory
WORKDIR /app

# Copy the Maven or Gradle build files
COPY pom.xml ./
# If you're using Gradle, use the following line instead:
# COPY build.gradle ./

# Copy the source code
COPY src ./src

# Build the application (for Maven)
RUN mvn clean package -DskipTests

# For Gradle, use the following line instead:
# RUN ./gradlew build -x test

# Use a new base image for running the application
FROM openjdk:21-jdk-slim

# Set the working directory for the final image
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/target/*.jar app.jar
# If you're using Gradle, use the following line instead:
# COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port that your application runs on
EXPOSE 8081

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
