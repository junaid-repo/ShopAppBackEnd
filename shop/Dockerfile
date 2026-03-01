# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Install OS-level dependencies for headless browser / PDF generation
RUN apt-get update && apt-get install -y \
    libasound2 \
    libatk1.0-0 \
    libcups2 \
    libgtk-3-0 \
    libxcomposite1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxrandr2 \
    libxtst6 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libnss3 \
    libxss1 \
    libdrm2 \
    libxkbcommon0 \
    xvfb \
    fontconfig \
    libfreetype6 \
    libatk-bridge2.0-0 \
    libgbm1 \
    && rm -rf /var/lib/apt/lists/*

# ✅ Copy your specific JAR
COPY --from=build /app/target/shop-0.0.3-USER_SPECIFIC.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
