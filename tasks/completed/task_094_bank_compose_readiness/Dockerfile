FROM docker.io/library/eclipse-temurin:21
WORKDIR /bank
COPY target/bank-service.jar bank-service.jar
ENTRYPOINT ["java", "-jar", "bank-service.jar"]