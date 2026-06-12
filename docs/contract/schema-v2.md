# Future Node Schema v2

Editor 서버의 future Node v2 스키마는 `Node` 단일 영속 모델을 공통 루트로 사용한다.

## 핵심 방향
- `Workspace`, `Page`, `Block`은 별도 루트 엔티티가 아니라 `Node.type`의 차이로 본다.
- `Node`는 단일 `nodes` 테이블에 저장한다.
- 트리는 `parent_id` 기반으로 구성한다.
- 타입별 메타와 본문은 별도 테이블로 분리한다.
- 그래프성 연결은 별도 reference 테이블로 분리한다.

## Node
```text
Node {
  id: UUID
  type: enum(WORKSPACE, PAGE, BLOCK)
  parent_id: UUID | NULL
  order: string
  created_at: datetime
  updated_at: datetime
}
```

### 설명
- `type`: `WORKSPACE`, `PAGE`, `BLOCK`
- `parent_id`: 상위 Node 식별자
- `order`: 형제 정렬 키. LexoRank 또는 fractional index 계열 문자열을 권장한다.
- `created_at` / `updated_at`: 공통 감사 필드
- 부모 `PAGE` 아래에는 `BLOCK`과 `PAGE`가 함께 저장될 수 있다.
- 즉, 본문 ordering의 canonical source는 page 전용 축과 block 전용 축이 아니라 공통 `Node.order`다.

## BlockContent
```text
BlockContent {
  node_id: UUID
  block_type: string
  content: JSON
}
```

### 설명
- `node_id`: 대상 Block Node
- `block_type`: `text`, `heading`, `image`, `code`, `toggle` 등 확장 가능한 타입
- `content`: 블록 본문과 렌더링 속성
- `BLOCK`은 mixed page body의 한 item이며, sibling `PAGE`와 같은 정렬 축을 공유한다.

## PageMeta
```text
PageMeta {
  node_id: UUID
  title: string
  icon: string | NULL
  cover: string | NULL
}
```

### 설명
- `node_id`: 대상 Page Node
- `title`: 페이지 제목
- `icon` / `cover`: 페이지 메타 정보
- inline child page 렌더는 `PageMeta`를 사용해 제목/아이콘을 노출할 수 있다.

## PagePreview
```text
PagePreview {
  node_id: UUID
  snapshot_version: integer
  generated_at: datetime
  items: PagePreviewItem[]
}

PagePreviewItem {
  block_type: string
  text: string
}
```

### 설명
- `node_id`: 대상 Page Node
- `snapshot_version`: preview가 생성된 기준 문서 snapshot/version
- `generated_at`: preview 생성 시각
- `items`: 카드 렌더에 필요한 축약 block 목록
- `block_type`: `paragraph`, `heading1`, `heading2`, `heading3` 같은 카드 렌더용 subtype
- `text`: 카드에 직접 보여줄 축약 텍스트
- `PagePreview`는 카드 렌더용 파생 표현이며, 문서 본문 저장 원본이 아니다.

## WorkspaceMeta
```text
WorkspaceMeta {
  node_id: UUID
  name: string
  owner_id: UUID
}
```

### 설명
- `node_id`: 대상 Workspace Node
- `name`: 워크스페이스 이름
- `owner_id`: 소유자 사용자 ID

## UserPageListPreference
```text
UserPageListPreference {
  user_id: UUID
  scope_type: enum(ALL_DOCUMENTS, TRASH, PARENT_PAGE)
  scope_node_id: UUID | NULL
  sort_by: enum(MANUAL, NAME, DATE_CREATED, DATE_UPDATED)
  sort_direction: enum(ASC, DESC)
  updated_at: datetime
}
```

### 설명
- `user_id`: preference 소유 사용자
- `scope_type`: 정렬이 적용되는 목록 문맥
- `scope_node_id`: `PARENT_PAGE`일 때 대상 부모 페이지 ID
- `sort_by`: FE 정렬 기준
- `sort_direction`: 오름차순/내림차순
- `MANUAL`은 canonical `Node.order`를 그대로 사용하는 보기 모드이며, 사용자별 커스텀 순서를 저장하는 필드는 아니다.

## NodeReference
```text
NodeReference {
  from_node_id: UUID
  to_node_id: UUID
  type: enum(MENTION, LINK, SYNC)
}
```

### 설명
- `from_node_id`: 참조를 시작한 Node
- `to_node_id`: 참조 대상 Node
- `type`: `MENTION`, `LINK`, `SYNC`

## 예시
```json
{
  "type": "BLOCK",
  "content": {
    "text": "hello",
    "styles": ["bold"]
  }
}
```
