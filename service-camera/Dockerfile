FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY common/ ./common/
COPY service-camera/ ./service-camera/
RUN mvn -pl common,service-camera -am clean package -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/service-camera/target/*.jar app.jar
EXPOSE 1099
ENTRYPOINT ["java", "-jar", "app.jar"]
