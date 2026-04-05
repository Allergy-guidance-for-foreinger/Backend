FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app

COPY --from=builder /app/target/*.jar /app/app.jar
RUN chown app:app /app/app.jar
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
