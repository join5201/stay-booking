# 백엔드 이미지 (이슈 230)
#
# 왜 이 파일이 필요한가: 백엔드는 지금까지 JDK가 깔린 개발 PC에서 gradlew bootRun으로만 떴다.
# 서버에는 JDK도 소스도 두지 않는다. 이 파일이 실행 파일(jar) 하나와 자바 실행기만 든 상자를 만든다.
#
# 두 단계로 나눈 이유: 굽는 데는 JDK와 Gradle과 소스가 필요하고 돌리는 데는 jar와 JRE만 필요하다.
# 앞 단계에서 굽고 뒤 단계에는 jar만 옮긴다. 소스와 Gradle 캐시는 최종 이미지에 남지 않는다.
#
# 실행 예시 (보통은 저장소 루트의 compose.yaml이 이 파일을 부른다)
#   docker build -t o2o-backend backend
#   docker compose --env-file backend/.env up --wait --build
#
# 실패 시 출력 예시
#   Could not resolve placeholder 'O2O_MYSQL_USER'
#     컨테이너에 DB 계정 환경변수가 안 넘어왔다. compose.yaml의 backend environment를 본다
#   Communications link failure
#     DB 주소가 설정 파일의 고정값(127.0.0.1:3307) 그대로다. SPRING_DATASOURCE_URL로 덮는다
#   COPY failed: no source files were specified (build/libs/*.jar)
#     앞 단계의 bootJar가 실패했다. 그 위 로그의 컴파일 오류를 본다
#
# DB 계정은 이미지에 넣지 않는다. 띄울 때 환경변수로 받는다. backend/.env는 .dockerignore가 뺀다.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 받기를 소스 복사보다 앞에 둔다. 소스만 바뀐 빌드는 이 층을 다시 받지 않는다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# gradlew가 실행 권한 없이(100644) 추적되고 있어 sh로 부른다
RUN sh ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null

COPY src src
# 테스트는 CI가 DB와 함께 돌린다. 여기서는 DB가 없어 뺀다
RUN sh ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

# root로 돌리지 않는다. 앱이 뚫려도 컨테이너 안에서 할 수 있는 일이 줄어든다
RUN useradd --system --uid 10001 --no-create-home o2o
COPY --from=build /app/build/libs/*.jar app.jar
USER o2o

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
