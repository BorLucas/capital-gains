# Build stage: compiles and runs the full test suite; a failing test fails the image build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B verify

# Runtime stage: JRE only, no Maven, no JDK, no sources
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && mkdir /data && chown app:app /data
USER app
WORKDIR /app
COPY --from=build /src/target/capital-gains-*.jar app.jar
# Low-footprint JVM: serial GC, C1 only, heap sized from the container limit
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxRAMPercentage=75 -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
