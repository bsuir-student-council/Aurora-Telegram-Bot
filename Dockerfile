FROM openjdk:21-jdk-slim

WORKDIR /app

COPY aurora-telegram-bot/target/aurora-telegram-bot-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
