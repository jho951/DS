# Editor Operations Semantics

Editor 서버의 작업 의미는 `Document`와 `Block`을 중심으로 정의한다.

## Document 작업
### 생성
- 새 `Document`는 `parentId`가 `null`인 루트 문서이거나, 다른 문서의 자식 문서일 수 있다.
- 생성 시 `sortKey`는 필수다.
- `visibility`는 기본값 `PRIVATE`를 권장한다.
- `createdBy`와 `updatedBy`는 생성 시점의 사용자로 기록한다.

### 조회
- 목록 조회는 기본적으로 `createdBy` 기준으로 필터링한다.
- 휴지통 조회는 `deletedAt != null`인 문서를 대상으로 한다.
- 휴지통 응답은 `purgeAt`을 포함한다.
- 상세 조회는 soft delete 상태라면 별도 복구 권한이 있어야 한다.

### 공개 상태 변경
- `visibility` 변경은 문서 메타 갱신으로 처리한다.
- 공개 상태 변경은 문서 버전이 일치해야 한다.

### 이동
- 문서 이동은 `targetParentId`, `afterDocumentId`, `beforeDocumentId`를 기준으로 한다.
- 동일 부모 내 이동은 정렬 키 갱신만으로 처리할 수 있다.
- 이동 결과는 순환 참조가 없어야 한다.

### 휴지통 이동 / 복구
- 휴지통 이동은 `deletedAt`을 채우는 soft delete다.
- 복구는 `deletedAt`을 해제하고 기존 계층과 정렬 위치를 복원한다.
- 복구 시 충돌이 있으면 `409`로 거절한다.

### 삭제
- 비즈니스 삭제는 soft delete를 우선한다.
- hard delete는 운영 정리나 데이터 정합성 작업에서만 사용한다.

## Block 작업
### 생성
- 블록은 반드시 특정 `Document`에 속해야 한다.
- `type`은 현재 `TEXT`만 허용한다.
- `content`는 `rich_text` JSON 스키마를 따라야 한다.
- 블록 생성은 transaction의 `BLOCK_CREATE`로 표준화할 수 있다.

### 수정
- 블록 수정은 본문 `content` 갱신이 핵심이다.
- `type` 변경은 현재 `TEXT` 외 타입이 없으므로 사실상 제한적이다.
- 수정 시 `version`을 이용해 낙관적 락을 적용한다.
- 블록 단건 수정은 관리자 API 또는 transaction의 `BLOCK_REPLACE_CONTENT`로 처리할 수 있다.
- 여러 블록 selection 자체를 서버가 들고 있지는 않는다. 여러 블록 일괄 수정은 클라이언트가 여러 단건 operation으로 batch를 조립하는 방식만 현재 v1에서 해석된다.

### 이동
- 블록 이동은 `parentRef`, `afterRef`, `beforeRef`를 사용한다.
- 이동 시 같은 문서 내부를 기본 정책으로 삼는다.
- `sortKey`는 동일 부모 내 순서만 반영해야 한다.
- 현재 move semantics는 단일 블록 기준이다. 여러 선택 블록의 상대 순서를 보존한 group move 규칙은 v1에 없다.
- 따라서 bulk move는 단순 batch alias가 아니라, 선택 집합 정규화, anchor 계산, 결과 응답까지 새 계약을 추가해야 한다.

### 삭제 / 복구
- 블록 삭제는 `deletedAt`을 사용하는 soft delete를 우선한다.
- 현재 v1에는 블록 복구 API가 없다.
- 복구가 필요하면 문서 단위 복구 또는 transaction 재적용 정책을 사용한다.
- 단건 블록 삭제는 선택한 루트 블록과 그 하위 subtree를 함께 soft delete할 수 있다. 이는 여러 임의 블록 selection을 한 번에 지우는 계약과는 다르다.

## 정렬 규칙
- `sortKey`는 형제 간 순서를 나타낸다.
- 삽입과 이동 비용을 줄이기 위해 LexoRank 또는 fractional index 계열을 사용할 수 있다.
- 정렬 충돌은 `409`로 처리한다.
- 정렬 키 공간이 부족하면 `9007 SORT_KEY_REBALANCE_REQUIRED`를 반환한다.

## v2 연계
- v2에서 `Node` 모델로 전환되더라도 현재 v1의 작업 의미는 유지해야 한다.
- v2의 `Node` 이동/삭제 의미는 별도 `rules-v2.md`와 `api-v2.md`를 따른다.
