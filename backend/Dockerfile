# ---- 1단계: 빌드 ----
# 이 프로젝트엔 gradlew(Gradle Wrapper)가 커밋되어 있지 않아
# (개발 샌드박스의 네트워크 제약으로 `gradle wrapper`를 실행하지 못함)
# 공식 Gradle 이미지를 빌드 스테이지로 사용한다.
# NCP 서버는 정상적인 인터넷 접근이 가능하므로 Maven Central에서
# 의존성을 정상적으로 받아올 수 있다.
FROM gradle:8.14.3-jdk21 AS build

WORKDIR /home/gradle/src

# 의존성 캐시를 최대한 활용하기 위해 빌드 스크립트만 먼저 복사
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# 소스 전체 복사 후 빌드 (테스트는 CI에서 이미 돌리므로 이미지 빌드 시엔 스킵)
COPY src ./src
RUN gradle build -x test --no-daemon

# ---- 2단계: 실행 ----
# 빌드에 쓴 무거운 JDK/Gradle 이미지 대신 가벼운 JRE 이미지로 실행
FROM eclipse-temurin:21-jre-jammy

# root로 실행하지 않도록 전용 사용자 생성
RUN useradd --create-home --shell /bin/false appuser
WORKDIR /home/appuser

COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 8080

# 컨테이너 안에서 메모리 제한을 인식하도록 하고,
# 컨테이너를 죽이지 않고 JVM을 정상 종료할 수 있도록 시그널 처리
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
