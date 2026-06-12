# Editor Cache Contract

Editor 서버의 캐시 계약은 현재 구현에 존재하지 않는 공유 캐시를 도입할 때의 기준을 정의한다.

## 현재 상태
- 현재 v1 구현에는 Redis 같은 공유 캐시 계층이 없다.
- 문서/블록 조회는 DB와 서비스 결과를 직접 반환한다.
- `purgeAt`은 휴지통 보존 정책이며 캐시가 아니다.

## 도입 시 캐시 대상
- Document 목록
- Document 트리/자식 목록
- Document 휴지통 목록
- Block 목록
- Block content

## 도입 시 캐시 계층
- 1차 캐시는 요청 범위 내 in-memory 캐시를 허용한다.
- 2차 캐시는 Redis 같은 공유 캐시를 사용할 수 있다.
- 캐시는 정합성보다 성능을 위해 사용하며, 쓰기 이후 즉시 무효화가 우선이다.

## 무효화 규칙
- Document 생성, 수정, 이동, 삭제, 복구 시 관련 Document 목록과 트리 캐시를 무효화한다.
- Block 생성, 수정, 이동, 삭제 시 관련 Block 목록과 content 캐시를 무효화한다.
- 권한 변화가 없더라도 `sortKey` 변경은 캐시 무효화 대상이다.
- `createdBy` 기준 목록 캐시는 사용자 단위로 분리한다.

## TTL 정책
- TTL은 최후의 안전장치로만 사용한다.
- mutation 이후에는 TTL 만료를 기다리지 말고 명시적으로 무효화한다.
- 운영 기본값은 짧은 TTL을 권장하지만, 스테일 응답이 허용되는 범위는 최소화한다.

## 캐시 키 설계
- 캐시 키는 최소한 `userId`, `documentId`, `parentId`, `deletedAt` 상태를 구분해야 한다.
- `createdBy`가 다르면 같은 경로라도 별도 캐시로 본다.
- `version`이 바뀌면 이전 캐시는 무효화한다.

## 금지 사항
- `Block.content` 전체를 장기 캐시하면서 write-through 없이 갱신하는 방식은 금지한다.
- 삭제/복구 직후에도 이전 데이터를 그대로 반환하는 stale cache는 허용하지 않는다.
- backup Workspace 데이터는 v1 활성 캐시 대상으로 삼지 않는다.
