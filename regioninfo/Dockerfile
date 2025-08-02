# Use a lightweight OpenJDK image
FROM amazoncorretto:17-alpine

# Set the working directory
WORKDIR /app

# Copy the built jar (you will build it in Step 2)
COPY build/libs/*.jar app.jar

# Expose port (same as your application)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
