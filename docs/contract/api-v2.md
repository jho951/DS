# Future Node API Contract v2

future Node v2 API는 `Node` 중심 리소스를 기준으로 정의한다.

## 리소스
- `Node`
- `BlockContent`
- `PageMeta`
- `PagePreview`
- `WorkspaceMeta`
- `NodeReference`

## 권장 API 형태

### Node
- `GET /v2/editor/nodes/{nodeId}`
- `GET /v2/editor/nodes/{nodeId}/children`
- `POST /v2/editor/nodes`
- `PATCH /v2/editor/nodes/{nodeId}`
- `DELETE /v2/editor/nodes/{nodeId}`
- `POST /v2/editor/nodes/{nodeId}/move`

### PageBody
- `GET /v2/editor/pages/{nodeId}/body`
- `POST /v2/editor/pages/{nodeId}/body/pages`

### BlockContent
- `GET /v2/editor/nodes/{nodeId}/content`
- `PUT /v2/editor/nodes/{nodeId}/content`

### PageMeta
- `GET /v2/editor/pages/{nodeId}/meta`
- `PUT /v2/editor/pages/{nodeId}/meta`
- `POST /v2/editor/pages/{nodeId}/duplicate`

### PagePreview
- `GET /v2/editor/pages/{nodeId}/preview`
- `GET /v2/editor/pages?include=preview`

### Personalized Page State
- `GET /v2/editor/pages/favorites`
- `POST /v2/editor/pages/{nodeId}/favorite`
- `DELETE /v2/editor/pages/{nodeId}/favorite`
- `GET /v2/editor/pages/recent`
- `POST /v2/editor/pages/{nodeId}/recent-view`
- `GET /v2/editor/preferences/page-list-sort`
- `PUT /v2/editor/preferences/page-list-sort`

### WorkspaceMeta
- `GET /v2/editor/workspaces/{nodeId}/meta`
- `PUT /v2/editor/workspaces/{nodeId}/meta`

### NodeReference
- `GET /v2/editor/nodes/{nodeId}/references`
- `POST /v2/editor/nodes/{nodeId}/references`
- `DELETE /v2/editor/nodes/{nodeId}/references/{referenceId}`

## 공통 규칙
1. `Node.type`은 요청/응답에서 항상 명시한다.
2. `parentId`와 `order`는 트리 정렬과 이동의 기준이다.
3. `childrenIds`는 응답 편의 필드로는 허용할 수 있지만, 저장 원본은 아니다.
4. `BlockContent.content`는 JSON 스키마 버전을 가질 수 있다.
5. `NodeReference`는 트리 관계와 분리된 별도 그래프 리소스다.
6. `favorites`와 `recent`는 사용자별 page relation이며, `Node` 본체 필드에 직접 넣지 않는다.
7. `recent-view`는 문서 상세/editor 진입 성공 뒤 기록하는 사용자 activity endpoint로 본다.
8. `PagePreview`는 카드 렌더용 축약 표현이며, 문서 본문이나 bitmap thumbnail의 canonical source가 아니다.
9. `PagePreview`는 stale 허용 응답이다.
10. `GET /v2/editor/pages?include=preview`는 홈 카드 렌더용 목록 최적화 경로로 사용할 수 있다.
11. `GET /v2/editor/pages/{nodeId}/body`는 페이지 본문에 속한 mixed child node 목록을 순서대로 반환한다.
12. body 응답에는 `BLOCK`과 `PAGE`가 함께 포함될 수 있다.
13. inline child page는 별도 부가 섹션이 아니라 body node로 취급한다.
14. `POST /v2/editor/nodes/{nodeId}/move`에서 `beforeNodeId`와 `afterNodeId` anchor는 `BLOCK`과 `PAGE`를 모두 참조할 수 있어야 한다.
15. `POST /v2/editor/pages/{nodeId}/body/pages`는 부모 페이지 본문 안에 inline child page를 생성하는 편의 API 후보다.
16. `POST /v2/editor/pages/{nodeId}/duplicate`는 페이지 meta와 body subtree를 새 page로 복제하는 v2 전용 endpoint 후보다.
17. duplicate는 원본 page의 favorites, recent, 공유 상태 같은 사용자별 relation을 승계하지 않는다.
18. `page-list-sort` preference는 최소 `manual`, `name`, `date_created`, `date_updated` 기준과 `asc|desc` 방향을 지원해야 한다.
19. `manual`은 사용자별 임의 순서가 아니라 canonical node `order`를 따르는 정렬 모드다.
20. `page-list-sort` preference는 scope를 가져야 한다.
   - 예: `all_documents`
   - 예: `trash`
   - 예: `parent_page`
21. `scope=parent_page`일 때는 대상 parent `nodeId`를 함께 받아 child page 목록 정렬 preference를 저장할 수 있어야 한다.

## 예시 응답
```json
{
  "id": "6d2f6c5c-57d4-4d4a-bd6e-2af9f4b98a91",
  "type": "PAGE",
  "parentId": "b34d1c3d-2f5f-4c7a-9b35-31d6e7d7d2a1",
  "order": "k00012",
  "meta": {
    "title": "Project Notes",
    "icon": "📝",
    "cover": null
  }
}
```

## PagePreview 예시 응답
```json
{
  "nodeId": "6d2f6c5c-57d4-4d4a-bd6e-2af9f4b98a91",
  "snapshotVersion": 18,
  "generatedAt": "2026-04-24T11:20:00",
  "items": [
    {
      "blockType": "heading1",
      "text": "Project Notes"
    },
    {
      "blockType": "paragraph",
      "text": "이번 주 편집 규칙을 정리합니다."
    },
    {
      "blockType": "paragraph",
      "text": "문서 카드는 이미지 대신 미니 렌더 preview를 사용합니다."
    }
  ]
}
```

## PageBody 예시 응답
```json
{
  "pageId": "6d2f6c5c-57d4-4d4a-bd6e-2af9f4b98a91",
  "items": [
    {
      "id": "fbe84f0f-7442-4b0a-bc86-0bcb4a93451d",
      "type": "BLOCK",
      "order": "k00010",
      "blockType": "paragraph",
      "content": {
        "text": "문서 본문 첫 줄"
      }
    },
    {
      "id": "9c04fb14-fcc7-4c8f-b2d8-788ea2380b0d",
      "type": "PAGE",
      "order": "k00011",
      "meta": {
        "title": "하위 페이지",
        "icon": "📄"
      },
      "preview": {
        "items": [
          {
            "blockType": "paragraph",
            "text": "하위 페이지 미리보기"
          }
        ]
      }
    }
  ]
}
```

## 상태 코드
- `400`
  - 잘못된 `type`, `parentId`, `order`, `content` 형식
- `404`
  - 노드 또는 메타 리소스 없음
- `409`
  - 순환 참조, 정렬 충돌, 타입 불일치
- `422`
  - 비즈니스 규칙 위반
