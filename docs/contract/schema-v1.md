# Editor Schema v1

Editor 서버의 현재 v1 스키마는 `Workspace` backup 모델과 `Document` / `Block` 활성 모델을 유지한다.

## 6.2 Workspace

```text
Workspace {
  id: string
  name: string
  createdBy: string | null
  updatedBy: string | null
  createdAt: datetime
  updatedAt: datetime
  version: number
}
```

### 설명
- `name`: 워크스페이스 표시 이름
- 영속 기본 키 컬럼명은 `workspace_id`를 사용한다.
- `Workspace`는 v1 활성 범위에서 제외한다.
- 백업된 `Workspace` 관련 코드는 `backup/workspace/` 아래에서만 보관한다.
- 인증 연동 전 단계에서는 `createdBy`, `updatedBy`를 `null`로 둘 수 있다.

## 6.3 Document

```text
Document {
  id: string
  parentId: string | null
  title: string
  icon: json | null
  cover: json | null
  visibility: enum(PUBLIC, PRIVATE)
  sortKey: string
  version: number
  createdBy: string | null
  updatedBy: string | null
  createdAt: datetime
  updatedAt: datetime
  deletedAt: datetime | null
}
```

### 설명
- 영속 기본 키 컬럼명은 `document_id`를 사용한다.
- v1 문서는 `Workspace` FK 없이 사용자 소유 문서로 관리한다.
- 문서 목록/휴지통 조회의 기본 필터 기준은 `createdBy`다.
- `parentId`: 상위 문서 ID. `null`이면 루트 문서. 영속 구현에서는 `parent_id` self FK로 상위 `Document`를 참조한다.
- `visibility`: 문서 공개 상태. 현재 `PUBLIC`, `PRIVATE`를 지원한다.
- `sortKey`: 같은 부모 아래 문서 순서 정렬용 필수 키
- `version`: 낙관적 락용 버전
- `deletedAt`: soft delete 시각
- 물리 스키마의 `parent_id` FK는 hard delete 시 하위 문서를 정리할 수 있도록 `ON DELETE CASCADE`를 사용한다. 이는 운영/테스트 정리용 안전장치이며, 비즈니스 삭제 정책 자체는 soft delete를 우선한다.

## 6.4 Block

```text
Block {
  id: string
  documentId: string
  parentId: string | null
  type: "TEXT"
  content: json
  sortKey: string
  version: number
  createdBy: string | null
  updatedBy: string | null
  createdAt: datetime
  updatedAt: datetime
  deletedAt: datetime | null
}
```

### 설명
- 영속 기본 키 컬럼명은 `block_id`를 사용한다.
- `documentId`: 블록이 속한 문서 ID. 영속 구현에서는 `document_id` FK로 `Document`를 참조한다.
- `parentId`: 상위 블록 ID. `null`이면 문서 루트 블록. 영속 구현에서는 `parent_id` self FK로 상위 `Block`을 참조한다.
- `type`: 블록 바깥 타입. 현재는 `TEXT`만 지원한다.
- `content`: TEXT 블록 본문. structured rich text JSON object
- `sortKey`: 같은 부모 아래 블록 순서 정렬용 키
- `version`: 낙관적 락용 버전
- 물리 스키마의 `document_id`, `parent_id` FK는 hard delete 시 dangling block이 남지 않도록 `ON DELETE CASCADE`를 사용한다. 비즈니스 삭제 정책 자체는 soft delete를 우선한다.

### 6.4.1 TEXT 블록 content 스키마

```json
{
  "format": "rich_text",
  "schemaVersion": 1,
  "blockType": "paragraph",
  "segments": [
    {
      "text": "Hello ",
      "marks": []
    },
    {
      "text": "world",
      "marks": [
        {
          "type": "bold"
        },
        {
          "type": "textColor",
          "value": "#000000"
        }
      ]
    }
  ]
}
```

### 설명
- `content.format`: 본문 표현 포맷. v1은 `rich_text`만 허용한다.
- `content.schemaVersion`: content 스키마 버전. v1은 `1`로 시작한다.
- `content.blockType`: optional rich-text subtype. 현재 허용값은 `paragraph`, `heading1`, `heading2`, `heading3`다. 값이 없으면 paragraph 기본형으로 해석한다.
- `content.segments`: 순서가 보장되는 텍스트 조각 배열
- 현재 validator 기준 `content` 허용 필드는 `format`, `schemaVersion`, `segments`, `blockType`뿐이다.
- `segment.text`: 실제 텍스트 조각
- `segment.marks`: 해당 텍스트 조각에 적용된 mark 목록
- `segment` 허용 필드는 `text`, `marks`뿐이다. `segment.type`이나 `segment.blockType`은 v1 범위에 없다.
- `mark.type`: mark 종류. v1 허용값은 `bold`, `italic`, `textColor`, `underline`, `strikethrough`
- `mark.value`: 값이 필요한 mark에서만 사용한다. v1에서는 `textColor`에만 사용한다.
- 새 빈 블록은 `segments=[{"text":"","marks":[]}]` 한 개를 가진 empty content를 허용한다.
- `Block.type`과 `content.format`은 같은 의미가 아니다.
- `Block.type`은 블록 종류를 나타내고, `content.format`과 `content.blockType`은 TEXT 블록 내부 본문 표현 포맷/서브타입을 나타낸다.

## 6.5 DocumentTransaction

```text
DocumentTransactionRequest {
  clientId: string
  batchId: string
  operations: DocumentTransactionOperationRequest[]
}

DocumentTransactionOperationRequest {
  opId: string
  type: enum(BLOCK_CREATE, BLOCK_REPLACE_CONTENT, BLOCK_MOVE, BLOCK_DELETE)
  blockRef: string | null
  version: number | null
  content: json | null
  parentRef: string | null
  afterRef: string | null
  beforeRef: string | null
}

DocumentTransactionResponse {
  documentId: UUID
  documentVersion: number | null
  batchId: string
  appliedOperations: DocumentTransactionAppliedOperationResponse[]
}

DocumentTransactionAppliedOperationResponse {
  opId: string
  status: enum(APPLIED, NO_OP)
  tempId: string | null
  blockId: UUID | null
  version: number | null
  sortKey: string | null
  deletedAt: datetime | null
}
```

### 설명
- `clientId`: transaction을 보낸 클라이언트 식별자
- `batchId`: transaction batch 식별자
- `operations`: 순서가 보장되는 operation 배열. 현재 각 원소는 단일 블록 기준 operation이다.
- `opId`: operation 추적용 ID
- `blockRef`: 새 블록 생성 시 temp ref 또는 기존 블록 ref
- `content`: `BLOCK_REPLACE_CONTENT`에서만 사용한다.
- `parentRef`, `afterRef`, `beforeRef`: block 이동/삽입 anchor
- `status`: operation 처리 결과
- `tempId`: 생성 시 임시 참조를 되돌려주는 값
- `DocumentTransactionRequest`에는 `selectedBlockIds`, `blockIds`, `selectionRange`, `groupMove` 같은 다중 선택 전용 필드가 없다.
- 여러 블록을 한 번에 바꾸려면 클라이언트가 여러 단일 operation으로 batch를 조립해야 한다.
- 여러 블록의 상대 순서를 보존한 group move semantics는 현재 v1 응답/요청 shape에 정의돼 있지 않다.

## 6.6 공통 응답 envelope

```text
GlobalResponse<T> {
  httpStatus: HttpStatus
  success: boolean
  message: string
  code: number
  data: T | null
}
```

### 설명
- 현재 v1 API는 `GlobalResponse`를 공통 envelope로 사용한다.
- `SUCCESS = 200`, `CREATED = 201`이 현재 주요 성공 코드다.
- 에러 응답은 별도 `ErrorCode` 체계를 따른다.
