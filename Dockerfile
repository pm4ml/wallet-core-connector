### Build runtime image
FROM openjdk:8-jdk-alpine
ARG JAR_FILE=core-connector/target/*.jar
COPY ${JAR_FILE} app.jar
ENV BACKEND_ENDPOINT=http://simulator:3000
ENV MLCONN_OUTBOUND_ENDPOINT=http://pm4ml-mojaloop-connector:4001
ENV DFSP_LOCALE=my
ENTRYPOINT ["java","-Dbackend.endpoint=${BACKEND_ENDPOINT}","-Doutbound.endpoint=${MLCONN_OUTBOUND_ENDPOINT}","-jar","-Ddfsp.locale=${DFSP_LOCALE}","/app.jar"]
EXPOSE 3003