FROM bellsoft/liberica-openjre-alpine:8u332-9

ENV APP_USER analyzer_server
RUN adduser -D -g '' $APP_USER

WORKDIR /analyzer_server

ARG file_jar

COPY $file_jar analyzer_server.jar

RUN chown -R $APP_USER /analyzer_server
RUN chmod -R 740 /analyzer_server

USER $APP_USER

CMD [ "java", "-Dscenario=scenario", "-jar", "analyzer_server.jar" ]
