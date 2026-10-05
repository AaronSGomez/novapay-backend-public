FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system novapay && adduser --system --ingroup novapay novapay

COPY --from=builder /app/target/novapay_backend_hex-0.0.1-SNAPSHOT.jar app.jar
RUN chown -R novapay:novapay /app

USER novapay
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
