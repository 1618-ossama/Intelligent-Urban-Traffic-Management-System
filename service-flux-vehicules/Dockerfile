FROM tomcat:9-jre17
RUN rm -rf /usr/local/tomcat/webapps/*
COPY target/flux.war /usr/local/tomcat/webapps/flux.war
EXPOSE 8081
RUN sed -i 's/port="8080"/port="8081"/' /usr/local/tomcat/conf/server.xml
CMD ["catalina.sh", "run"]
