FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY backend-core backend-core
COPY backend-users-management backend-users-management

WORKDIR /workspace/backend-users-management

RUN chmod +x gradlew && \
    ./gradlew bootJar -x test -x spotlessCheck --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/backend-users-management/build/libs/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
