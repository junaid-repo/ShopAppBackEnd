# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
# We upgrade to Debian 12 (Bookworm) which natively supports Java 17 AND native Chromium!
FROM debian:bookworm-slim
WORKDIR /app

# Install Java 17 JRE, Chromium, and fonts directly from Debian 12's native repository
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    chromium \
    fonts-liberation \
    fontconfig \
    xvfb \
    && rm -rf /var/lib/apt/lists/*

# ✅ Copy the predictable JAR from Stage 1
COPY --from=build /app/target/shop.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]