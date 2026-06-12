# Future Node Rules v2

future Node v2는 `Node` 단일 영속 모델을 기준으로 한다.

## 트리 구조
- `Node`는 `nodes` 테이블의 `parent_id`로 계층을 만든다.
- `childrenIds`는 저장하지 않는다.
- 자식 조회는 `parent_id` 기준으로 수행한다.
- `Page`도 자식을 가질 수 있다.
- `Page`의 본문 자식은 `BLOCK`만이 아니라 `PAGE`도 될 수 있다.
- 같은 부모 `PAGE` 아래에서 `BLOCK`과 `PAGE`는 mixed body를 구성할 수 있다.

## 그래프 구조
- `@page mention`
- `synced block`
- `backlink`

이런 기능은 트리만으로 표현하지 않고 `NodeReference`로 분리한다.

## 정렬
- `order`는 형제 노드 순서를 결정한다.
- 추천 방식은 `LexoRank` 또는 fractional index다.
- 삽입/이동 시 재정렬 비용을 줄이기 위해 밀도 높은 순서 키를 사용한다.
- mixed body에서도 `BLOCK`과 `PAGE`는 별도 정렬 축을 쓰지 않고 하나의 `order` 축을 공유한다.

## 이동 규칙
- 노드 이동은 `parent_id`와 `order`를 함께 변경한다.
- 부모가 바뀌면 하위 트리의 상대적 순서는 유지해야 한다.
- 동일 부모 내 이동은 정렬 키만 갱신할 수 있다.
- inline child page 이동도 별도 문서 이동 규칙이 아니라 공통 node move로 처리한다.
- `PAGE`를 `BLOCK` 앞뒤로, `BLOCK`을 `PAGE` 앞뒤로 이동할 수 있어야 한다.
- body move anchor는 type이 아니라 sibling 순서를 기준으로 해석한다.
- FE에서 보이는 page block drag/drop은 서버에서 mixed node reorder로 수용해야 한다.

## 삭제 규칙
- `Node` 삭제 시 하위 노드와 메타 데이터의 cascade 규칙을 명확히 해야 한다.
- `NodeReference`는 대상 노드가 사라지면 dangling reference 정리 정책이 필요하다.

## 개인화 메타 규칙
- `favorite`는 사용자-페이지 명시적 bookmark다.
- `favorite` 추가/해제는 사용자의 직접 action으로만 일어난다.
- `recent`는 사용자-페이지 자동 activity 로그다.
- `recent`는 목록 노출, hover, prefetch로 기록하지 않는다.
- `recent`는 페이지 상세 또는 editor 진입이 성공한 뒤에만 upsert 한다.
- 동일 사용자가 같은 페이지를 짧은 시간 안에 반복 열어도 무조건 갱신하지 않는다.
  - 권장: 60초 이내 재열람은 no-op
- `recent` 조회는 `last_viewed_at desc` 정렬을 기본으로 한다.
- soft delete/trash 또는 권한 상실 페이지는 기본 `favorites`/`recent` 응답에서 제외한다.
- `favorite`와 `recent`는 `Node` 본체 lifecycle과 분리된 relation data로 관리한다.
- 목록 정렬 preference도 `Node` 본체 lifecycle과 분리된 사용자별 relation/preference data로 관리한다.
- FE 정렬 기준은 최소 `manual`, `name`, `date_created`, `date_updated`를 지원한다.
- `manual`은 canonical tree/body `order`를 따르는 보기 모드다.
- `name`, `date_created`, `date_updated`는 canonical order를 바꾸지 않고 조회/응답 정렬에만 영향을 준다.
- direction은 `asc|desc`를 지원한다.
- preference는 최소 목록 문맥 단위 scope를 가져야 한다.
  - 전체 문서 목록
  - trash 목록
  - 특정 parent page의 child page 목록
- 사용자가 브라우저를 바꿔도 같은 계정이면 같은 정렬 기준을 받아야 한다.
- drag/drop reorder는 `manual` 모드에서만 canonical order 변경으로 연결해야 한다.

## 카드 preview 규칙
- 카드 preview는 문서의 축약 렌더 표현이다.
- 기본 표시 방식은 첫 3~5개 visible block을 미니 문서처럼 렌더하는 summary다.
- heading/paragraph 계열은 유지하되, 상세 editor formatting 전체를 1:1 재현할 필요는 없다.
- preview는 문서 상세 조회용 full payload를 그대로 재사용하지 않고 카드 전용 축약 shape를 사용한다.
- preview는 최신 저장 상태와 약간 어긋날 수 있는 stale 허용 데이터다.
- preview 생성/갱신 실패는 문서 저장 실패로 간주하지 않는다.
- preview가 비어 있으면 카드 UI는 skeleton 또는 text fallback으로 렌더한다.
- bitmap thumbnail은 선택적 미래 기능이며, 도입하더라도 preview summary를 대체하는 canonical source로 두지 않는다.
- bitmap thumbnail 생성은 문서 save path의 동기 단계에 넣지 않고, 별도 비동기 파생 작업으로 분리한다.

## inline child page 규칙
- child page는 부모 page의 부가 목록이 아니라 본문 body item으로 간주할 수 있다.
- body 조회는 현재 순서의 `BLOCK`과 `PAGE`를 함께 반환해야 한다.
- inline child page는 제목, 아이콘, preview summary를 가질 수 있다.
- inline child page를 클릭하면 해당 page 상세/editor로 진입한다.
- inline child page 삭제는 단순 preview 제거가 아니라 page node lifecycle 규칙을 따라야 한다.
- inline child page가 이동해도 그 page 자신의 하위 subtree 상대 순서는 유지해야 한다.
- v1의 `Document.sortKey`와 `Block.sortKey` 분리 모델에서는 이 규칙을 만족할 수 없으므로 v2 범위로 둔다.

## Block 확장
- 새로운 Block 타입은 `nodes` 테이블 구조를 바꾸지 않고 `block_type`과 `content` JSON만 확장한다.
- 렌더러는 `block_type`별 플러그인 구조로 분리한다.

## 불변식
1. `Node.type`은 실제 저장 시점에 변경 정책이 필요하다.
2. `WORKSPACE`는 루트 컨테이너 역할을 한다.
3. `PAGE`는 페이지 트리의 내부 노드가 될 수 있다.
4. `BLOCK`은 콘텐츠 렌더링 단위다.
5. 트리 관계와 참조 관계를 섞지 않는다.
6. 같은 부모 `PAGE` 아래의 `PAGE`와 `BLOCK`은 동일 body ordering semantics를 공유한다.
