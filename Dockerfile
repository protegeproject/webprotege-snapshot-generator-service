FROM eclipse-temurin:17
LABEL maintainer="protege.stanford.edu"

EXPOSE 7773
ARG JAR_FILE
COPY target/${JAR_FILE} webprotege-snapshot-generator-service.jar
ENTRYPOINT ["java","-jar","/webprotege-snapshot-generator-service.jar"]
