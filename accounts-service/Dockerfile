FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY accounts-service/target/accounts-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]