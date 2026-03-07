FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/service-camera-*.jar app.jar
EXPOSE 1099
ENTRYPOINT ["java", "-jar", "app.jar"]
