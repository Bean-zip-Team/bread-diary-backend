# BreadType Domain Guide (Draft)

## 이 도메인의 역할

빵 분류 기준(reference data)을 제공하는 lookup 도메인이다.

## 핵심 포인트

### 1. BreadType은 reference domain이다

주요 역할:

- code/name 기준 분류값 제공
- Bread가 참조하는 기준 데이터 제공

### 2. enum처럼 다루지 말고 DB 기준으로 본다

현재 BreadType은 DB 엔티티 기준이다.

즉 새 작업 시:

- enum 하드코딩보다는 repository/service 조회
- code/name 응답 규칙 유지

를 우선 본다.

### 3. 크게 복잡한 도메인은 아니다

복잡한 비즈니스 로직보다는:

- 조회
- 응답 매핑
- 참조 무결성

가 핵심이다.

즉 새로운 규칙을 이 도메인 자체에 넣기보다, bread/breadrecord 쪽 참조 규칙을 먼저 보는 편이 낫다.

## 작업 전 확인 추천 파일

- `domain/breadtype/controller/BreadTypeController.java`
- `domain/breadtype/service/BreadTypeService.java`
- `domain/breadtype/repository/BreadTypeRepository.java`
- `domain/breadtype/entity/BreadType.java`
