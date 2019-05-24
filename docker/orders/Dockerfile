FROM weaveworksdemos/msd-java:jre-latest

WORKDIR /usr/src/app
COPY *.jar ./app.jar

RUN	chown -R ${SERVICE_USER}:${SERVICE_GROUP} ./app.jar

USER ${SERVICE_USER}

ENTRYPOINT ["/usr/local/bin/java.sh","-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1041", "-jar","./app.jar", "--port=80"]
