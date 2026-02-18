FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY common/ ./common/
COPY service-bruit/ ./service-bruit/
RUN mvn -pl common,service-bruit -am clean package -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/service-bruit/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
