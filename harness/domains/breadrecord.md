# BreadRecord Domain Guide (Draft)

## 이 도메인의 역할

유저가 실제로 기록한 빵 섭취/방문/리뷰 데이터를 관리한다.

## 핵심 포인트

### 1. BreadRecord는 유저 활동 데이터다

중심 필드:

- `userId`
- `bread`
- `photoUrl`
- `shopName`
- `eatenDate`
- `rating`
- `review`
- `isPublic`

### 2. 삭제는 soft delete 기준

`deletedAt` 기반 soft delete를 사용한다.

조회/집계 작업 시:

- 삭제된 기록 제외 여부

를 항상 먼저 확인한다.

### 3. 이미지 처리 규칙은 record 쪽 책임이 큼

현재 사진 업로드는 record 흐름에 강하게 연결되어 있다.

예:

- 원본용 이미지 리사이즈
- thumbnail 생성
- S3 업로드
- `photo_thumbnail_url` 파생

즉 이미지 변경 이슈는 breadrecord + global image/s3 계층을 함께 봐야 한다.

### 4. 카탈로그 통계의 기반 데이터다

`eatCount`, `avgRating`, `latestPhotoUrl`, `latestEatenDate` 같은 값은 breadrecord 기반으로 파생된다.

즉 bread 쪽 조회 기능을 수정할 때도 breadrecord projection/aggregation을 같이 봐야 한다.

## 작업 전 확인 추천 파일

- `domain/breadrecord/controller/BreadRecordController.java`
- `domain/breadrecord/service/BreadRecordService.java`
- `domain/breadrecord/service/BreadRecordComplexService.java`
- `domain/breadrecord/repository/BreadRecordRepository.java`
- `global/image/ImageResizeService.java`
- `global/s3/S3UploadService.java`
