FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY common/ ./common/
COPY service-flux-vehicules/ ./service-flux-vehicules/
RUN mvn -pl common,service-flux-vehicules -am clean package -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/service-flux-vehicules/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
