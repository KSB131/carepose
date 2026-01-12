# 1단계: 빌드 스테이지 (Maven을 사용하여 .jar 파일 생성)
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
# 권한 부여 및 빌드 진행 (테스트는 스킵하여 속도 향상)
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# 2단계: 실행 스테이지 (생성된 .jar 파일만 가져와서 실행)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
# 빌드 스테이지에서 생성된 jar 파일을 복사
COPY --from=build target/*.jar app.jar

# 포트 개방
EXPOSE 8084

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]