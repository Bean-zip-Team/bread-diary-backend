# Service Layer Rules (Draft)

## 목적

서비스 레이어 책임을 명확히 나눠, 도메인 간 결합과 임시 로직 확산을 줄인다.

## 기본 규칙

### 1. Controller는 얇게 유지

- 요청 파싱
- 인증된 userId 읽기
- service 호출
- 응답 반환

Controller에 비즈니스 분기 로직을 넣지 않는다.

### 2. 일반 Service 책임

일반 Service는 자신의 도메인에 대한 핵심 로직만 가진다.

예:

- `BreadService`
- `BreadRecordService`
- `BreadTypeService`

일반 Service는 가능하면 다음만 직접 참조한다.

- 자신의 Repository
- 자신의 Mapper
- 자신의 도메인과 직접 관련된 DTO/Projection

### 3. ComplexService 책임

도메인 간 조합/오케스트레이션은 `ComplexService`에 둔다.

예:

- 빵 조회 + 기록 통계 결합
- 기록 생성 시 빵 생성/조회 동시 처리
- 인증 + 유저 생성/세션 생성 조합

복수 도메인의 Service를 함께 호출하거나, 한 요청 안에서 흐름을 조합해야 하면 `ComplexService`를 우선 고려한다.

### 4. Mapper 책임

Mapper는 변환 책임만 가진다.

- Entity -> Response DTO
- Request DTO -> Entity
- projection 조합

비즈니스 규칙 자체를 Mapper에 과도하게 넣지 않는다.

단, 표현 규칙 수준의 가벼운 변환은 허용한다.

예:

- thumbnail URL 파생
- placeholder image URL 파생
- 응답용 field formatting

### 5. Repository 직접 호출 범위

Controller가 Repository를 직접 호출하지 않는다.

일반적으로:

- Controller -> Service / ComplexService
- Service -> Repository

형태를 유지한다.

## 판단 기준

아래 중 하나라도 해당하면 `ComplexService` 후보:

- 두 개 이상 도메인 Service를 함께 호출한다.
- 한 API 안에서 조회/생성/통계/권한 분기가 섞인다.
- 흐름 제어가 핵심이고 단일 Repository 로직이 아니다.

## 현재 프로젝트에 맞춘 메모

- `bread`, `breadrecord`는 이미 Service / ComplexService 분리 패턴이 존재한다.
- 새 기능을 추가할 때는 먼저 기존 `BreadComplexService`, `BreadRecordComplexService`, `AuthService` 구조를 확인한다.
