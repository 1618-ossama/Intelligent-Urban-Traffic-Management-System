FROM tomcat:9-jre17
RUN rm -rf /usr/local/tomcat/webapps/*
COPY target/axis.war /usr/local/tomcat/webapps/axis.war
EXPOSE 8084
RUN sed -i 's/port="8080"/port="8084"/' /usr/local/tomcat/conf/server.xml
CMD ["catalina.sh", "run"]
