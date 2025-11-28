### Backend Dockerfile - build Spring Boot JAR and run
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package
RUN mvn -B -DskipTests package spring-boot:repackage

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
