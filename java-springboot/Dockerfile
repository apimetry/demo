FROM gradle:8.10.0-jdk21 AS build
WORKDIR /app
COPY src ./src
COPY build.gradle ./build.gradle
RUN gradle bootjar

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar /optel-agent.jar
RUN chmod a+r /optel-agent.jar

FROM gcr.io/distroless/java21:nonroot
WORKDIR /app
COPY --from=build /app/build/libs/app-0.0.1.jar ./app.jar
COPY --from=build /optel-agent.jar /optel-agent.jar

USER nonroot
ENTRYPOINT ["java", "-javaagent:/optel-agent.jar", "-jar", "/app/app.jar"]
CMD []
