# Editor Rules v1

Editor 서버의 현재 규칙은 `Document` / `Block` 활성 모델과 `Workspace` backup 모델을 분리하는 방향을 기준으로 한다.

## 1. v1 활성 범위
- `Workspace`는 v1 활성 범위에서 제외한다.
- v1에서 사용자가 직접 다루는 도메인은 `Document`와 `Block`이다.
- `Workspace` 관련 코드는 `backup/workspace/` 아래에서만 보관한다.
- 활성 API와 UI는 `Workspace` FK 없이 동작한다.

## 2. 경로 / API 규칙
- 외부 경로는 Gateway 기준 `/v1/documents/**`, `/v1/admin/**`로 노출된다.
- 내부 서비스 컨트롤러는 `/documents/**`, `/admin/**`를 사용한다.
- 모든 성공 응답은 `GlobalResponse` envelope을 사용한다.
- 문서 수정, 상태 변경, 이동, 복원은 `version` 기반 낙관적 락을 적용한다.

## 3. 트리 구조
- `Document`는 `parentId`로 계층을 만든다.
- `Block`도 `parentId`로 계층을 만든다.
- `childrenIds`는 저장하지 않는다.
- 자식 조회는 `parentId` 기준으로 수행한다.
- `Document`는 페이지 트리의 내부 노드가 될 수 있다.
- `Block`은 문서 루트 블록이 될 수 있고, 하위 블록을 가질 수 있다.

## 4. 정렬
- `sortKey`는 같은 부모 아래 형제 순서를 결정한다.
- 추천 방식은 `LexoRank` 또는 fractional index다.
- 삽입/이동 시 재정렬 비용을 줄이기 위해 문자열 기반 순서 키를 사용한다.
- 정렬 키가 부족해지면 `SORT_KEY_REBALANCE_REQUIRED`를 반환한다.

## 5. 이동 규칙
- 문서 이동은 `targetParentId`, `afterDocumentId`, `beforeDocumentId`를 기준으로 한다.
- 블록 이동은 `parentRef`, `afterRef`, `beforeRef`를 사용한다.
- 부모가 바뀌어도 하위 트리의 상대적 순서는 유지해야 한다.
- 동일 부모 내 이동은 정렬 키만 갱신할 수 있다.

## 6. 삭제 / 휴지통 규칙
- 문서 삭제와 블록 삭제는 soft delete를 우선한다.
- 문서 휴지통 이동은 `deletedAt`을 기록하고, 복구는 `deletedAt`을 해제한다.
- 휴지통 응답은 `purgeAt`을 함께 제공한다.
- `ON DELETE CASCADE`는 hard delete 정리용 안전장치다.

## 7. Visibility 규칙
- 문서는 `PUBLIC` / `PRIVATE` visibility를 가진다.
- visibility 변경은 문서 버전과 함께 처리한다.
- 공개 상태 변경은 문서 소유자 또는 관리자만 수행할 수 있다.

## 8. Transaction 규칙
- 에디터 저장의 표준 경로는 `POST /documents/{documentId}/transactions`다.
- transaction은 `clientId`, `batchId`, `operations[]`로 구성된다.
- `operations[]`는 여러 단일 블록 operation의 순차 적용 모델이지, 다중 선택 selection 자체를 표현하는 모델은 아니다.
- operation type은 `BLOCK_CREATE`, `BLOCK_REPLACE_CONTENT`, `BLOCK_MOVE`, `BLOCK_DELETE`만 허용한다.
- `BLOCK_CREATE`는 `blockRef`가 필요하고 `content`는 비워둔다.
- `BLOCK_REPLACE_CONTENT`는 `blockRef`와 `content`가 필요하다.
- `BLOCK_MOVE`와 `BLOCK_DELETE`는 `blockRef`가 필요하다.
- operation 결과는 `APPLIED` 또는 `NO_OP`로 기록된다.
- 현재 v1 request에는 `selectedBlockIds`, `blockIds`, `selectionRange`, `groupMove` 같은 다중 선택 전용 필드가 없다.
- 여러 블록 delete나 subtype 일괄 변경은 클라이언트가 여러 `BLOCK_DELETE` 또는 `BLOCK_REPLACE_CONTENT` operation으로 펼쳐서 보낼 수 있다.
- 여러 블록의 상대 순서를 유지한 채 한 번에 이동하는 bulk move는 v1에 정의되지 않았고, 별도 request/validation/result 계약이 필요하다.

## 9. Block content 규칙
- `Block.type`은 현재 `TEXT`만 지원한다.
- `content.format`은 `rich_text`만 허용한다.
- `content.schemaVersion`은 `1`부터 시작한다.
- `content.blockType`은 optional string subtype이고, 현재 허용값은 `paragraph`, `heading1`, `heading2`, `heading3`다.
- `content.blockType`이 없으면 paragraph 기본형으로 본다.
- `content.segments`는 순서가 보장되어야 한다.
- `content` 허용 필드는 `format`, `schemaVersion`, `segments`, `blockType`뿐이다.
- `segment` 허용 필드는 `text`, `marks`뿐이다.
- `mark.type` 허용값은 `bold`, `italic`, `textColor`, `underline`, `strikethrough`다.
- `textColor`만 `value`를 사용할 수 있다.
- 새 빈 블록은 `segments=[{"text":"","marks":[]}]` 형태를 허용한다.
- `Block.type`과 `content.format`은 같은 의미가 아니다.
- top-level block enum은 여전히 `TEXT`이고, rich text subtype은 별도 Java enum이 아니라 `content.blockType` 문자열 closed set으로 검증한다.

## 10. Workspace backup 규칙
- `Workspace`는 재설계 전까지 backup 모델로만 유지한다.
- backup Workspace 구현은 active API와 분리한다.
- backup 코드는 `backup/workspace/` 아래에만 둔다.
- active 문서/블록 로직과 workspace backup 로직을 섞지 않는다.
