# git submodules (libs/make-you-chic-ui, libs/java-mustache-processor) must be
# checked out on the host before `docker build` — COPY does not run
# `git submodule update` itself. See README.md setup step 1.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :backend:bootWar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/backend/build/libs/*.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
