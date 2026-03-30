FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -pl common,service-feux -am clean package -DskipTests -q

FROM tomcat:9-jre17
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/service-feux/target/axis.war /usr/local/tomcat/webapps/axis.war
EXPOSE 8084
# Override default Tomcat port
RUN sed -i 's/port="8080"/port="8084"/' /usr/local/tomcat/conf/server.xml
CMD ["catalina.sh", "run"]
