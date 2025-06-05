FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY . /app

RUN mvn -B -DskipTests clean install

CMD ["java", "-jar", "target/asyncpayments-*.jar"]