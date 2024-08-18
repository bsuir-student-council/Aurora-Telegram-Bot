# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build

ENV HOME=/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME

# Copy the project files
COPY aurora-telegram-bot/pom.xml $HOME
COPY aurora-telegram-bot/src $HOME/src

# Install Maven and build the project
RUN apt-get update && \
    apt-get install -y maven && \
    mvn -f $HOME/pom.xml clean package

# Package stage
FROM eclipse-temurin:21-jre-jammy

ARG JAR_FILE=/usr/app/target/*.jar

# Copy the built jar file to the runner location
COPY --from=build $JAR_FILE /app/runner.jar

EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "/app/runner.jar"]
