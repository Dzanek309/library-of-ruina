FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN addgroup -S libgroup && adduser -S libuser -G libgroup
USER libuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
