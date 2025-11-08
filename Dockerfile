# 1단계: 빌드(Build) 환경
# Gradle과 JDK 21 이미지를 'builder'라는 별명으로 사용
# (arm64 - 라즈베리파이 지원)
FROM gradle:8-jdk21 AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 빌드에 필요한 파일들만 먼저 복사 (빌드 캐시 활용)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 소스 코드 복사
COPY src ./src

# gradlew 스크립트 복사
COPY gradlew ./
# gradlew 스크립트에 실행 권한 부여
RUN chmod +x ./gradlew

# Gradle 빌드 실행 (테스트는 제외하여 빌드 속도 향상)
# 실행 가능한 jar 파일을 생성합니다.
RUN ./gradlew build -x test


# 2단계: 실행(Runtime) 환경
# 가장 가볍고, 다중 아키텍처(arm64 포함)를 지원하는 JRE 이미지를 사용
FROM eclipse-temurin:21-jre

# 작업 디렉토리 설정
WORKDIR /app

# 1단계(builder)에서 생성된 .jar 파일을 복사
# build/libs/ 디렉토리의 모든 .jar 파일 중 하나를 app.jar로 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring Boot 앱이 사용하는 8080 포트 개방
EXPOSE 8080

# 컨테이너가 시작될 때 실행할 명령어
# "java -jar app.jar" 실행
ENTRYPOINT ["java", "-jar", "app.jar"]