# syntax=docker/dockerfile:1
FROM gradle:9.4.1-jdk21-alpine AS build
WORKDIR /app
COPY . .
# BuildKit 캐시로 gradle 의존성을 재사용 — 이미지 빌드마다 재다운로드하다 네트워크 오류로 실패하는 것을 방지.
RUN --mount=type=cache,target=/home/gradle/.gradle gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache ca-certificates java-cacerts && update-ca-certificates
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
