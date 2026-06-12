# Editor DB Migration

Editor 서버는 현재 v1에서 `Document`와 `Block`을 활성 모델로 유지하고,
`Workspace`는 backup 전용으로 두면서 future Node v2의 `Node` 단일 영속 모델로 전환할 수 있도록 마이그레이션 경계를 문서화한다.

## 현재 v1 저장 모델
- `Workspace`
  - backup 전용 모델
  - v1 활성 API에서는 노출하지 않는다
- `Document`
  - 사용자 소유 문서의 트리 루트
  - `parent_id` self FK 사용
  - `sortKey`, `version`, `deletedAt` 사용
- `Block`
  - `document_id` FK + `parent_id` self FK 사용
  - `type = TEXT`만 지원
  - `content`는 `rich_text` JSON
  - rich-text subtype은 optional `content.blockType`으로 저장하고, top-level block enum은 계속 `TEXT`를 유지한다
  - 현재 runtime은 `hibernate.type.preferred_uuid_jdbc_type=CHAR`로 UUID JDBC 바인딩을 고정한다

## v2 전환 목표
- `Workspace`, `Page`, `Block`을 `nodes` 단일 테이블에 통합한다.
- 트리 관계는 `parent_id`로 단순화한다.
- 타입별 메타와 본문은 별도 테이블로 분리한다.
- 그래프 참조는 `NodeReference`로 분리한다.

## 마이그레이션 원칙
1. v1 데이터와 backup Workspace 코드는 그대로 유지한다.
2. v2 도입 전까지 v1 API와 스키마를 깨지 않는다.
3. v2는 별도 schema / rules / api / openapi 문서로 검증한 뒤 도입한다.
4. 공통 ID와 정렬 키는 v1/v2 간 매핑 가능해야 한다.

## 전환 순서
### 1) v2 `nodes` 테이블 도입
- `Node`를 먼저 생성한다.
- `Workspace`, `Page`, `Block`의 공통 필드를 `Node`로 이동한다.

### 2) 메타 / 콘텐츠 테이블 분리
- `BlockContent`
- `PageMeta`
- `WorkspaceMeta`

### 3) 참조 테이블 도입
- `NodeReference`
- mention / link / sync 관계 이동

### 4) 읽기 경로 전환
- 신규 조회는 `Node`를 기준으로 수행한다.
- 기존 v1 조회는 compatibility layer로 유지한다.

### 5) 쓰기 경로 전환
- 신규 생성/이동/삭제는 `Node` API를 사용한다.
- v1 쓰기 경로는 deprecated로 표시한다.

### 6) legacy 제거
- 모든 클라이언트가 v2 모델로 전환되면 v1 전용 테이블과 API를 제거한다.

## 운영 제약
- `Workspace` backup 코드는 `backup/workspace/` 아래에서만 관리한다.
- v1의 `Document` / `Block` 삭제는 soft delete를 우선한다.
- hard delete는 운영/테스트 정리용으로만 사용한다.
- FK의 `ON DELETE CASCADE`는 dangling row 정리용 안전장치로만 둔다.

## 검증
- v1/v2 간 데이터 수가 보존되어야 한다.
- 정렬 순서가 보존되어야 한다.
- reference 관계가 끊기지 않아야 한다.
- 롤백 가능성을 확보해야 한다.

## 운영 메모
- 대규모 트리 이동은 배치 작업으로 분리할 수 있다.
- 순환 참조와 dangling reference는 마이그레이션 후 별도 정합성 검사로 확인한다.
