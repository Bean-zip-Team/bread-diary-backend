# Project Structure (Draft)

## 프로젝트 개요

- project name: `bread-diary-backend`
- Gradle root project name: `bread-diary`
- base package: `com.bean.breaddiary`
- main class: `BreadDiaryApplication`
- JPA Auditing enabled

## 기술 스택

- Java 21
- Gradle
- Spring Boot 4.0.5
- Spring Data JPA
- Spring Validation
- Spring Web MVC
- springdoc-openapi
- MapStruct
- AWS SDK S3
- MySQL

## 기본 폴더 구조

메인 코드는 `src/main/java/com/bean/breaddiary` 아래에 둔다.

도메인 구조:

- `domain/<feature>/controller`
- `domain/<feature>/service`
- `domain/<feature>/repository`
- `domain/<feature>/entity`
- `domain/<feature>/dto/request`
- `domain/<feature>/dto/response`
- `domain/<feature>/dto/mapper`

공통 코드는:

- `global/`

테스트는 `src/test/java/com/bean/breaddiary` 아래에서 프로덕션 구조를 따라간다.

## 기존 구현 참고 우선순위

새 기능 구현 전 우선 확인:

- `domain.bread`
- `domain.breadrecord`

즉 새 구조를 만들기 전에 기존 패턴 재사용을 우선한다.

## Build / Run 메모

주요 명령:

- `./gradlew test`
- `./gradlew build`
- `./gradlew bootRun`
- `./gradlew clean build`

환경설정 파일이 항상 준비되어 있다고 가정하지 않는다.
