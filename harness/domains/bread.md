# Bread Domain Guide

## 이 도메인의 역할

빵 카탈로그, 자동완성, 도감 목록, 빵 프로필 조회의 중심 도메인이다.

## 핵심 포인트

### 1. Bread는 카탈로그 기준 엔티티

현재 Bread는:

- `stickerNumber` unique
- `name` unique
- `breadType` 보유
- `imageUrl` 보유
- `createdBy`가 있으면 user-created bread

### 2. imageUrl 표현 규칙이 API마다 다를 수 있음

현재 이미지는 단순 원본 필드가 아니라, 응답 문맥에 따라 달라질 수 있다.

예:

- catalog: 미수집이면 `_placeholder`
- autocomplete: 미수집이면 `_placeholder`
- profile/detail: 원본 유지

즉 `Bread.imageUrl` 원본 필드와 응답 `imageUrl`은 항상 1:1이 아닐 수 있다.

### 3. 수집 여부는 유저별 값

Bread 자체가 수집 여부를 갖는 게 아니라,

- 로그인 유저의 기록 수
- 관련 통계 projection

으로 계산된다.

예:

- `eatCount > 0` → collected
- `eatCount == 0` → uncollected

### 4. breadType은 코드/이름 응답 규칙 유지

응답에서는 보통:

- `breadType` = code
- 필요 시 label/name 별도 노출

패턴을 유지한다.

### 5. 카탈로그/추천/자동완성은 optional auth를 자주 씀

로그인 상태면 개인화:

- `isCollected`
- placeholder 여부
- 통계값

비로그인이면:

- 기본 카탈로그 데이터
- `is_collected=false`

형태로 응답하는 패턴을 우선 고려한다.

## 작업 전 확인 추천 파일

- `domain/bread/controller/BreadController.java`
- `domain/bread/service/BreadService.java`
- `domain/bread/service/BreadComplexService.java`
- `domain/bread/dto/mapper/BreadMapper.java`
- `domain/bread/repository/BreadRepository.java`
