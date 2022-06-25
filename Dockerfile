FROM openjdk:18.0.1.1-jdk
WORKDIR /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]