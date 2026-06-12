# Editor Authorization & Ownership

Editor 서버의 권한 규칙은 `createdBy` 기반 소유권과 Gateway가 주입한 내부 사용자 컨텍스트를 기준으로 한다.

`authz-service`는 사용자가 `editor:write`, `workspace:write`, `project:edit` 같은 capability를 보유하는지 판정한다.
`user-service`는 그 capability를 프로필에 공개할지 여부를 결정한다.
`Editor`는 실제 문서/블록 변경 시점에 최종 집행을 강제한다.

## 신뢰 경계
- Editor 서버는 외부에서 직접 들어오는 `Authorization`, `Cookie`, `X-User-Id`를 신뢰하지 않는다.
- Editor 서버는 Gateway가 정규화한 내부 요청만 처리한다.
- 현재 v1에서 신뢰 가능한 핵심 헤더는 `X-User-Id`, `X-Request-Id`다.
- Gateway는 인증 성공 후 내부 `Authorization` JWT도 다시 주입하며, Editor 서버는 이 내부 JWT 계약을 기준으로 인증 컨텍스트를 검증한다.
- `X-User-Role`은 신뢰하지 않으며 권한 판정 입력으로 사용하지 않는다.
- 프로필에서 보이는 권한 배지는 실행 가능 여부를 대신하지 않는다.

## 소유권 원칙
- `createdBy`가 리소스의 기본 소유자다.
- `updatedBy`는 마지막 수정 주체를 기록한다.
- `createdBy = null`인 legacy/system 데이터는 초기 이관 또는 백업 경로에서만 허용할 수 있다.
- `createdBy = null` 리소스의 수정 권한은 관리자 또는 마이그레이션 작업에 제한한다.

## Document 권한
- 문서 생성은 인증된 사용자만 가능하다.
- 문서 목록/휴지통 조회는 현재 사용자 컨텍스트 기준으로 개인 문서를 반환한다.
- 단건 조회와 블록 목록 조회는 현재 구현상 `CurrentUserId` 인자를 받지 않지만, Gateway 인증을 통과한 요청만 허용한다.
- 문서 수정, 이동, 휴지통 이동, 복구는 `actorId`와 소유권 정책이 일치하는 경우에만 허용한다.
- `createdBy`가 다른 사용자의 문서는 소유권 이전 정책이 따로 없으면 수정할 수 없다.

## Block 권한
- 블록의 권한은 부모 `Document` 권한을 따른다.
- 문서에 대한 수정 권한이 없으면 해당 문서의 블록도 수정할 수 없다.
- 블록 생성, 이동, 수정, 삭제는 문서 소유권 정책과 일치하는 사용자만 허용한다.
- 현재 v1에는 블록 복구 API가 없다.

## Workspace backup 권한
- `Workspace`는 v1 활성 API에 노출하지 않는다.
- `backup/workspace/` 아래 코드는 내부 운영/마이그레이션 경로로만 사용한다.
- 일반 사용자 요청으로는 `Workspace` backup 모델을 변경하지 않는다.

## 관리자 권한
- 현재 editor-server 구현은 별도 관리자 상태 헤더에 의존하지 않는다.
- Gateway 또는 상위 정책이 추가 메타데이터를 전달하더라도, 서비스는 `CurrentUserId`와 리소스 상태를 기준으로 검증한다.
- 소유권 기록(`createdBy`, `updatedBy`)은 관리자 권한과 무관하게 유지한다.
- 권한 보유 사실의 공개 여부는 editor-server 책임이 아니라 user-service 정책이다.

## 검증 규칙
- 권한 또는 상태 실패는 현재 구현의 `401`, `400`, `404`, `409` 중 해당하는 에러 코드로 반환한다.
- 인증 정보 자체가 없거나 Gateway 컨텍스트가 누락된 경우는 `401` 또는 Gateway 차단으로 처리한다.
- 소유권 검사는 Document 기준으로 먼저 수행하고, Block은 Document 권한을 상속한다.
- 최종 실행 직전에는 capability truth와 현재 리소스 소유권을 함께 확인한다.
- 권한 추가/변경 시에는 `role`, `condition`, `token scope`, `cache invalidation`, `final enforcement` 관점으로 다시 검토한다.
