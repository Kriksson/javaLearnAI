FROM eclipse-temurin:21-jdk AS packager
WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src
RUN bash ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=packager /build/target/bank-service.jar /app/bank-service.jar
ENTRYPOINT ["java", "-jar", "/app/bank-service.jar"]