# Editor V2 Extension

이 문서는 `Document`, `Block`, 추후 `Workspace`까지도 하나의 공통 operation core로 묶고, 영속 모델은 분리 유지하는 4번 방안을 정리한다.

## 목적
- 편집 경험을 하나의 operation 흐름으로 통일한다.
- 저장 구조는 도메인별로 유지한다.
- 공통 정책은 공유하고, 도메인 invariant는 분리한다.
- 향후 `Workspace`가 추가되더라도 operation 계층을 재사용할 수 있게 한다.

## 범위
- move
- transaction/save
- page duplicate
- reorder / rebalance
- batch apply 성격 작업
- 공통 sortKey policy
- 공통 tracing / logging / validation entry
- 사용자별 문서 personal metadata
  - favorites
  - recently viewed
- 메인 카드용 page preview summary
- 페이지 본문 안의 자식 페이지 inline block 처리

## 비범위
- `Document`, `Block`, `Workspace`의 영속 엔티티 통합
- `Node` 단일 테이블 도입
- CRUD 전체의 단일 컨트롤러 통합
- type별 권한/소유권/콘텐츠 규칙 통합

## Platform Rollout Coupling
- `editor v2`는 API/영속 구조 변경만 따로 가지 않고, platform runtime 경계 정리와 함께 진행한다.
- 현재 prod filesystem backing은 service-owned `ResourceContentStore` 구현이 맡고 있다.
- v2 rollout에서는 이 구현을 `platform-resource` optional support module로 승격해 서비스에서 제거하는 것을 포함한다.
- 즉 v2 기준 `editor-service`는 resource kind 정책과 도메인 사실만 소유하고, generic filesystem backing 조립은 2계층이 소유한다.

## 권장 구조
- `DocumentController`
- 블록 전용 CRUD 컨트롤러
- `Workspace`가 활성화되면 별도 CRUD 컨트롤러
- `EditorOperationController`
- `EditorOperationService`
- 타입별 domain service
- 기존 transaction orchestrator
- 공통 sortKey generator

## 4번 방안의 핵심
1. 리소스 CRUD는 각 도메인별 컨트롤러와 서비스에 남긴다.
2. 공통 편집 행위는 `EditorOperationController` 아래로 모은다.
3. 공통 작업은 command와 orchestration에서 먼저 정리한다.
4. 저장 모델은 도메인별로 유지하고, 공통 정책만 공유한다.

## 공통화 기준
- 정적 리소스 상태를 바꾸는 요청이면 리소스 컨트롤러로 둔다.
- editor 상호작용에서 생기는 작업이면 operation 컨트롤러로 보낸다.
- 공통화는 request shape, command, tracing, logging, testing에서 먼저 얻는다.
- 공통 policy는 `sortKey`, move anchor, batch apply 순서, operation status에 둔다.

## 사용자별 문서 메타데이터 v2 후보
- `favorite`와 `recently viewed`는 사용자 전역 preference가 아니라 사용자-문서 관계다.
- source of truth는 `user-service`가 아니라 `editor-service`가 소유한다.
- 접근 가능 여부, 문서 `visibility`, 소유권, trash 상태와 함께 평가해야 하므로 문서 도메인에서 처리한다.
- 저장 모델은 문서 본체와 분리한 사용자별 relation 테이블을 권장한다.
  - 예: `document_favorites(user_id, document_id, created_at)`
  - 예: `document_recents(user_id, document_id, last_viewed_at)`
- `favorite`는 명시적 사용자 action으로만 생성/삭제한다.
- `recently viewed`는 문서 상세 또는 editor 진입이 성공했을 때만 upsert 한다.
- 목록 hover, prefetch, 권한 실패, preview 실패는 recent 기록으로 취급하지 않는다.
- recent는 같은 문서를 짧은 시간 안에 반복 진입할 때 write churn을 막기 위해 debounce/throttle 정책을 둔다.
  - 권장: 같은 문서 60초 이내 재열람은 `last_viewed_at` 갱신 생략
- recent 목록은 최신순 `last_viewed_at desc`로 반환하고, 보관 개수 상한을 둔다.
  - 권장: 사용자당 최근 100개 유지
- trash 문서나 더 이상 접근 권한이 없는 문서는 기본 favorites/recent 조회 결과에서 제외한다.

## 목록 정렬 preference v2 후보
- FE 문서 목록/LNB 하위 목록 정렬 UI는 v2에서 아래 기준을 지원한다.
  - `manual`
  - `name`
  - `date_created`
  - `date_updated`
- `manual`은 사용자별 커스텀 순서를 뜻하지 않는다. canonical node `order`를 그대로 따르는 보기 모드다.
- `name`, `date_created`, `date_updated`는 사용자별 list view preference로 저장한다.
- 방향은 최소 `asc`, `desc`를 지원한다.
  - 초기 기본값은 `manual`
  - `name`은 FE에서 방향 토글 UI를 둘 수 있다.
  - `date_created`, `date_updated`도 direction 필드를 재사용해 asc/desc를 표현할 수 있다.
- preference scope는 전역 하나가 아니라 목록 문맥별로 저장할 수 있어야 한다.
  - 예: 전체 문서 목록
  - 예: 특정 parent page의 child page 목록
  - 예: trash 목록
- source of truth는 localStorage가 아니라 backend relation/preference 저장소다.
- 브라우저가 바뀌어도 같은 사용자는 같은 정렬 기준을 받아야 한다.
- 정렬 기준 preference는 canonical tree order를 바꾸지 않는다.
- drag/drop reorder는 `manual` 모드에서만 직접 노출하는 방향을 권장한다.

## 카드 preview v2 후보
- 메인 페이지 카드 preview의 기본 모델은 bitmap 이미지가 아니라 snapshot 기반 summary다.
- source of truth는 문서 본문 DB와 마지막으로 합의된 서버 snapshot이며, 카드 preview는 그 파생 표현이다.
- 카드 preview는 첫 3~5개 visible block을 축약해 미니 문서처럼 렌더하는 것을 기본값으로 둔다.
  - 예: `paragraph`, `heading1`, `heading2`, `heading3`
- preview summary는 문서 상세 API 전체를 다시 내려받지 않고 카드 렌더에 필요한 최소 필드만 포함한다.
- preview summary는 stale 허용 데이터다.
  - 저장 직후 즉시 최신화되지 않아도 목록 UX를 깨지 않는 범위에서 허용한다.
- preview 생성 실패는 문서 저장 실패로 전파하지 않는다.
  - preview만 비어 있고 카드 자체는 fallback UI로 렌더할 수 있어야 한다.
- bitmap thumbnail은 v2 기본선이 아니다.
- 이미지 thumbnail이 필요해지면 별도 비동기 파생 리소스로 취급한다.
  - 예: `document-thumbnail`
  - 문서 save transaction 안에서 동기 생성하지 않는다.
  - 실패해도 문서 CRUD, favorites, recent 흐름에는 영향을 주지 않는다.

## 문서 복제 v2 후보
- 문서 복제는 v1 범위가 아니다. FE 문서 액션과 backend API를 함께 v2로 올린다.
- source of truth는 `editor-service`이며, FE는 duplicate command를 직접 로컬 조합하지 않고 backend duplicate API를 호출한다.
- 복제 대상은 `PAGE` node와 그 body subtree 전체다.
  - 제목
  - icon / cover
  - visible body child node
  - inline child page subtree
  - preview 생성에 필요한 파생 입력
- 복제 결과는 새 `nodeId`와 새 ordering anchor를 가진 독립 페이지여야 한다.
- 사용자별 relation 데이터는 원본에서 자동 복제하지 않는다.
  - `favorite`
  - `recently viewed`
  - 공유 토글/권한 결과 캐시
- 기본 제목 정책은 `"원본 제목의 사본"` 계열 suffix를 허용하되, locale별 문자열은 FE/BE 협의로 고정한다.
- 큰 subtree 복제는 단순 sync insert 루프가 아니라 backend batch/copy orchestration으로 처리한다.
- preview summary는 복제 직후 stale이거나 비어 있어도 되지만, 본문 원본은 정합하게 복제되어야 한다.

## 자식 페이지 inline block v2 후보
- 현재 v1은 `Document.sortKey` 축과 `Block.sortKey` 축이 분리돼 있다.
- 따라서 자식 페이지를 텍스트 블록들 사이에 끼워 넣거나, 블록과 같은 ordering axis에서 함께 이동시키는 것은 v1 범위가 아니다.
- FE에서 요구하는 "페이지 안의 페이지를 하나의 블록처럼 렌더하고 이동" UX는 editor-server v2 계약으로 올린다.
- v2에서는 페이지 본문을 `PAGE`와 `BLOCK`이 함께 존재할 수 있는 mixed node body로 본다.
- 같은 부모 `PAGE` 아래의 자식 `PAGE`와 자식 `BLOCK`은 하나의 공통 `order` 축을 공유해야 한다.
- inline child page는 별도 reference가 아니라 실제 body child node다.
- 즉, parent page 본문 조회 결과에는 text block과 child page block이 섞인 순서대로 내려와야 한다.
- child page block 이동은 블록 이동과 다른 별도 UX가 아니라 공통 node move semantics로 처리한다.
- child page block의 카드형 미리보기 텍스트, 아이콘, 제목은 `PageMeta`와 `PagePreview` 파생 데이터로 구성할 수 있다.
- 현재 FE에서 임시로 구현한 "본문 아래 별도 child page section"은 v1 한계 대응용이며, canonical body model은 아니다.

## 주요 흐름
### move
1. 프론트는 공통 move 요청을 보낸다.
2. `EditorOperationController`가 공통 request를 받는다.
3. command로 변환한다.
4. `EditorOperationService`가 타입별 service로 분배한다.
5. 실제 검증과 영속 변경은 각 도메인 서비스가 담당한다.
6. 공통 sortKey 정책은 shared policy layer에서 재사용한다.

### transaction/save
1. 프론트는 editor operation batch를 보낸다.
2. `EditorOperationController`가 공통 입구에서 받는다.
3. 기존 transaction orchestrator를 호출한다.
4. 필요하면 기존 `DocumentTransactionServiceImpl`, `DocumentTransactionOperationExecutor` 구조를 operation 관점에서 재배치한다.
5. batch apply 결과와 versioning은 공통 response shape로 맞춘다.

## 레이어
### Resource layer
- `Document`
- `Block`
- `Workspace` backup 또는 future activation

### Operation layer
- move
- reorder
- transaction/save
- batch apply

### Domain layer
- `Document` lifecycle
- `Block` content and tree rules
- `Workspace` ownership and container rules

### Shared policy layer
- `sortKey` policy
- trace id / request id propagation
- operation validation envelope
- batch ordering semantics

## 기대 효과
- 공통 흐름을 서비스 입구에서 통합할 수 있다.
- 저장 모델 변경 없이 공통 작업 의미를 공유할 수 있다.
- type별 정책과 lifecycle을 흐리지 않는다.
- 현재 v1 계약과의 충돌을 줄인다.
- 나중에 `Workspace`가 추가되어도 operation 축은 유지된다.

## 유지 원칙
1. v1 CRUD는 현재 리소스 경계를 유지한다.
2. operation 공통화는 editor interaction에만 적용한다.
3. `Node`는 별도 미래 연구 축으로 남기고, v2의 기본 전제로 두지 않는다.
4. 공통화 이득이 storage 복잡도를 앞서지 않는 한 persistence는 분리한다.
