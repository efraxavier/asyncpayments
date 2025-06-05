FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY . /app
RUN --mount=type=cache,id=maven-repo,target=/root/.m2/repository mvn -B -DskipTests clean install
CMD ["java", "-jar", "target/asyncpayments-*.jar"]