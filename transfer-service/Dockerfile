FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY transfer-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]