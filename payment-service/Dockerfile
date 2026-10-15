# Build from the repository root:  docker build -f payment-service/Dockerfile -t payment-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY common-events common-events
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
RUN mvn -q -B -pl payment-service -am dependency:go-offline -DskipTests || true
COPY payment-service payment-service
RUN mvn -q -B -pl payment-service -am package -DskipTests \
 && java -Djarmode=layertools -jar payment-service/target/payment-service-*.jar extract --destination /layers

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
