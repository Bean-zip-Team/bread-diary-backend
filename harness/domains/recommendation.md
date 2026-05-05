# Recommendation Domain Guide

## 이 도메인의 역할

오늘의 추천 빵 목록을 생성하고, 날짜별 추천 이력을 관리하는 도메인이다.

## 핵심 포인트

### 1. 추천 목록은 공용이다

- 같은 날에는 모든 유저가 같은 추천 빵 5개를 본다.
- 유저별로 달라지는 값은 `isCollected`, `imageUrl` 표현뿐이다.

### 2. 추천 이력은 날짜 기준으로 저장한다

- 최근 3일 추천 제외 규칙을 위해 날짜별 추천 결과를 저장한다.
- 추천 이력은 유저별이 아니라 날짜별 공용 데이터다.

### 3. 추천 후보는 bread + breadrecord 데이터를 함께 본다

- 추천 대상은 system catalog bread 기준이다.
- 최근 7일 기록 수 집계는 `bread_records` 기준이다.
- `deletedAt is null`, `eatenDate` 최근 7일 기준을 우선한다.

### 4. 응답은 개인화 표현만 추가한다

- 로그인 사용자면 `isCollected` 계산
- 수집한 빵은 원본 `imageUrl`
- 미수집 빵은 placeholder `imageUrl`

## 작업 전 확인 추천 파일

- `domain/recommendation/controller/RecommendationController.java`
- `domain/recommendation/service/RecommendationComplexService.java`
- `domain/recommendation/service/RecommendationService.java`
- `domain/recommendation/repository/DailyRecommendationRepository.java`
- `domain/breadrecord/repository/BreadRecordRepository.java`
