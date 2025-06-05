FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY . /app

# Instala o Maven
RUN apk add --no-cache maven

RUN mvn -B -DskipTests clean install

CMD ["java", "-jar", "target/asyncpayments-0.0.1-SNAPSHOT.jar"]