FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY . /app

RUN --mount=type=cache,id=s/9a5f87a8-1685-4ce0-b19d-599250d99439-maven-cache,target=/root/.m2/repository mvn -B -DskipTests clean install

CMD ["java", "-jar", "target/asyncpayments-*.jar"]