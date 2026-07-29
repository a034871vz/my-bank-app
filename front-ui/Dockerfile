FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY front-ui/target/front-ui-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]