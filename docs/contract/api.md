# Editor API Contract

현재 `editor-service` 구현의 v1 API는 `Document`와 `Block`을 `GlobalResponse` envelope로 반환한다.
Gateway 외부 노출 경로는 `/v1/documents/**`, `/v1/admin/**`이고, 서비스 내부 컨트롤러는 `/documents/**`, `/admin/**`를 사용한다.

- TEXT 블록 본문은 `rich_text` 스키마를 사용하고, 현재 구현은 optional `content.blockType`으로 `paragraph`, `heading1`, `heading2`, `heading3`를 허용한다.
- `segment`는 `text`, `marks`만 허용한다.

## 공통 응답 envelope
```text
GlobalResponse<T> {
  httpStatus: HttpStatus
  success: boolean
  message: string
  code: number
  data: T | null
}
```

### 성공 코드
- `SUCCESS`: HTTP 200, code 200
- `CREATED`: HTTP 201, code 201

## 에러 응답
- 현재 v1 에러 응답도 `GlobalResponse<Void>` envelope을 사용한다.
- `code`와 HTTP status는 `repositories/editor-service/errors.md`의 `ErrorCode`와 일치한다.
- 현재 구현은 `400`, `401`, `404`, `405`, `409`, `500`을 사용한다.
- 현재 구현에는 `403`과 `422` 전용 에러 코드는 없다.

## 리소스
- `Document`
- `Block`
- `DocumentTransaction`
- `TrashDocumentResponse`
- `Workspace` backup model

## Document API
### 목록
- `GET /documents`
- Gateway 외부 경로: `GET /v1/documents`

### 휴지통
- `GET /documents/trash`
- Gateway 외부 경로: `GET /v1/documents/trash`

### 생성
- `POST /documents`
- 요청: `CreateDocumentRequest`
- 응답: `GlobalResponse<DocumentResponse>`
- Gateway 외부 경로: `POST /v1/documents`

### 단건 조회
- `GET /documents/{documentId}`
- 응답: `GlobalResponse<DocumentResponse>`

### 블록 목록 조회
- `GET /documents/{documentId}/blocks`
- 응답: `GlobalResponse<BlockResponse[]>`

### 수정
- `PATCH /documents/{documentId}`
- 요청: `UpdateDocumentRequest`
- 응답: `GlobalResponse<DocumentResponse>`

### 공개 상태 수정
- `PATCH /documents/{documentId}/visibility`
- 요청: `UpdateDocumentVisibilityRequest`
- 응답: `GlobalResponse<DocumentResponse>`

### transaction 반영
- `POST /documents/{documentId}/transactions`
- 요청: `DocumentTransactionRequest`
- 응답: `GlobalResponse<DocumentTransactionResponse>`

### 삭제 / 휴지통 / 복구 / 이동
- `DELETE /documents/{documentId}`
- `PATCH /documents/{documentId}/trash`
- `POST /documents/{documentId}/restore`
- `POST /documents/{documentId}/move`

## Admin Block API
### 생성
- `POST /admin/documents/{documentId}/blocks`
- 요청: `DocumentTransactionRequest`
- 응답: `GlobalResponse<DocumentTransactionResponse>`

### 수정
- `PATCH /admin/blocks/{blockId}`
- 요청: `DocumentTransactionRequest`
- 응답: `GlobalResponse<DocumentTransactionResponse>`

### 삭제
- `DELETE /admin/blocks/{blockId}`
- 요청: `DocumentTransactionRequest`
- 응답: `GlobalResponse<DocumentTransactionResponse>`

### 이동
- `POST /admin/blocks/{blockId}/move`
- 요청: `DocumentTransactionRequest`
- 응답: `GlobalResponse<DocumentTransactionResponse>`

## 요청 스키마
### CreateDocumentRequest
- `parentId: UUID | null`
- `title: string`
- `icon: json | null`
- `cover: json | null`

### UpdateDocumentRequest
- `title: string`
- `icon: json | null`
- `cover: json | null`
- `version: number`

### UpdateDocumentVisibilityRequest
- `visibility: PUBLIC | PRIVATE`
- `version: number`

### MoveDocumentRequest
- `targetParentId: UUID | null`
- `afterDocumentId: UUID | null`
- `beforeDocumentId: UUID | null`

### DocumentTransactionRequest
- `clientId: string`
- `batchId: string`
- `operations: DocumentTransactionOperationRequest[]`
  순서 있는 단일 블록 operation 배열이다. 현재 API에는 `selectedBlockIds`, `blockIds`, `selectionRange`, `groupMove` 같은 다중 선택 전용 필드가 없다.

### DocumentTransactionOperationRequest
- `opId: string`
- `type: BLOCK_CREATE | BLOCK_REPLACE_CONTENT | BLOCK_MOVE | BLOCK_DELETE`
- `blockRef: string | null`
- `version: number | null`
- `content: json | null`
  `rich_text` content 스키마를 따르며 optional `blockType`을 포함할 수 있다.
- `parentRef: string | null`
- `afterRef: string | null`
- `beforeRef: string | null`

### CreateBlockRequest
- `parentId: UUID | null`
- `type: TEXT`
- `content: json`
  `format`, `schemaVersion`, `segments`는 필수이고 `blockType`은 선택이다.
- `afterBlockId: UUID | null`
- `beforeBlockId: UUID | null`

### UpdateBlockRequest
- `content: json`
  `rich_text` content 스키마를 따르며 optional `blockType`을 포함할 수 있다.
- `version: number`

## 응답 스키마
### DocumentResponse
- `id`
- `parentId`
- `title`
- `icon`
- `cover`
- `visibility`
- `sortKey`
- `createdBy`
- `updatedBy`
- `deletedAt`
- `version`
- `createdAt`
- `updatedAt`

### BlockResponse
- `id`
- `documentId`
- `parentId`
- `type`
- `content`
- `sortKey`
- `createdBy`
- `updatedBy`
- `deletedAt`
- `version`
- `createdAt`
- `updatedAt`

### TrashDocumentResponse
- `documentId`
- `title`
- `parentId`
- `deletedAt`
- `purgeAt`

### DocumentTransactionResponse
- `documentId`
- `documentVersion`
- `batchId`
- `appliedOperations[]`

### DocumentTransactionAppliedOperationResponse
- `opId`
- `status: APPLIED | NO_OP`
- `tempId`
- `blockId`
- `version`
- `sortKey`
- `deletedAt`

## 예시 응답
### Document 목록 / 단건 예시
```json
{
  "httpStatus": "OK",
  "success": true,
  "message": "요청 응답 성공",
  "code": 200,
  "data": {
    "id": "3f2b4e6a-3c0a-4b2f-8e18-5f4a9f4e7a11",
    "parentId": null,
    "title": "Project Notes",
    "icon": null,
    "cover": null,
    "visibility": "PRIVATE",
    "sortKey": "k00012",
    "version": 3,
    "createdBy": "user-123",
    "updatedBy": "user-123",
    "createdAt": "2026-03-31T09:00:00",
    "updatedAt": "2026-03-31T09:10:00",
    "deletedAt": null
  }
}
```

### Block 목록 예시
```json
{
  "httpStatus": "OK",
  "success": true,
  "message": "요청 응답 성공",
  "code": 200,
  "data": [
    {
      "id": "6c8d7d4f-7a67-4f3d-a0ec-7d7b7c9f4c10",
      "documentId": "3f2b4e6a-3c0a-4b2f-8e18-5f4a9f4e7a11",
      "parentId": null,
      "type": "TEXT",
      "content": {
        "format": "rich_text",
        "schemaVersion": 1,
        "segments": [
          {
            "text": "Hello world",
            "marks": []
          }
        ]
      },
      "sortKey": "k00005",
      "version": 2,
      "createdBy": "user-123",
      "updatedBy": "user-123",
      "createdAt": "2026-03-31T09:01:00",
      "updatedAt": "2026-03-31T09:02:00",
      "deletedAt": null
    }
  ]
}
```

### 트랜잭션 예시
```json
{
  "httpStatus": "OK",
  "success": true,
  "message": "요청 응답 성공",
  "code": 200,
  "data": {
    "documentId": "3f2b4e6a-3c0a-4b2f-8e18-5f4a9f4e7a11",
    "documentVersion": 4,
    "batchId": "batch-001",
    "appliedOperations": [
      {
        "opId": "op-1",
        "status": "APPLIED",
        "tempId": "temp-1",
        "blockId": "6c8d7d4f-7a67-4f3d-a0ec-7d7b7c9f4c10",
        "version": 2,
        "sortKey": "k00005",
        "deletedAt": null
      }
    ]
  }
}
```

### Void 성공 예시
```json
{
  "httpStatus": "OK",
  "success": true,
  "message": "요청 응답 성공",
  "code": 200,
  "data": null
}
```

### Error 예시
```json
{
  "httpStatus": "NOT_FOUND",
  "success": false,
  "message": "요청한 문서를 찾을 수 없습니다.",
  "code": 9004,
  "data": null
}
```

## 상태 코드
- `400`
  - 잘못된 요청, 필드 유효성 검사 실패
- `401`
  - 인증 정보가 없는 경우
- `404`
  - 문서, 블록, URL을 찾을 수 없는 경우
- `405`
  - 허용되지 않은 HTTP 메서드
- `409`
  - 버전 충돌, 정렬 충돌, 상태 충돌
- `500`
  - 서버 내부 오류

## 비고
- 현재 v1 저장은 transaction 기반으로 블록 변경을 반영한다.
- transaction batch는 여러 단건 operation의 묶음이지, 여러 블록 selection 자체를 의미하지 않는다.
- 여러 블록 delete나 subtype 일괄 변경은 클라이언트가 여러 operation으로 펼쳐 보내야 한다.
- 여러 블록의 상대 순서를 유지한 bulk move는 현재 API에 없다.
- `Block` 복구 API는 현재 v1에 없다.
- `Workspace`는 backup 모델로만 남긴다.
