# Editor Security Contract

Editor 서버의 보안 계약은 Gateway를 통한 내부 호출과 민감 데이터 노출 방지를 기준으로 한다.

## 전송 경계
- Editor 서버는 외부 공개 API가 아니라 Gateway 뒤의 내부 서비스로 취급한다.
- 브라우저 쿠키와 외부 `Authorization` 헤더는 Editor 서버가 직접 처리하지 않는다.
- 인증과 채널 판정은 Gateway에서 완료한 뒤 내부 컨텍스트만 전달한다.
- Gateway는 인증 성공 뒤 외부 인증 정보를 내부 전용 JWT와 내부 헤더로 정규화한다.

## 신뢰 헤더
- 신뢰 가능한 핵심 헤더는 Gateway가 재주입한 `X-User-Id`, `X-Request-Id`다.
- `X-User-Role`은 신뢰하지 않으며 권한 판정 입력으로 사용하지 않는다.
- 외부 입력에서 들어온 `X-User-Id`는 무시한다.

## 헤더 정규화
- 요청 진입 시 외부 `Authorization`, `Cookie`, `X-User-Id`는 신뢰하지 않는다.
- Editor 서버는 Gateway가 정규화한 내부 토큰 또는 내부 헤더만 수용한다.
- 내부 `Authorization`은 Gateway가 재발급한 JWT를 의미하며, 기본 검증 계약은 `issuer=api-gateway`, `audience=internal-services`, shared secret 일치다.
- `authz-service` caller proof 용 `aud=authz-service` 토큰은 Editor 서버 인증 토큰으로 사용하지 않는다.
- 로그에 사용자 토큰, 쿠키 값, rich text 전체 payload를 남기지 않는다.

## 데이터 보호
- `createdBy`, `updatedBy`는 개인정보 취급 가능성이 있으므로 운영 로그에서 필요 이상 노출하지 않는다.
- `Block.content`는 사용자가 입력한 텍스트이므로 렌더링 시 클라이언트 측에서 XSS 방어가 필요하다.
- 서버는 content를 저장할 때 escape 정책보다 구조 무결성을 우선한다.

## 접근 정책
- v1 문서/블록 API는 인증된 사용자만 접근한다.
- backup Workspace 경로는 운영/마이그레이션 작업자만 접근한다.
- 보안 검증 실패는 현재 구현의 `401`, `400`, `404`, `409`, `500` 중 적절한 코드로 처리한다.

## 운영 주의
- direct service-to-service 호출은 허용하지 않는 것을 기본으로 한다.
- 내부망이라도 인증 컨텍스트 누락 요청은 거절해야 한다.
- 비정상적인 content payload, 순환 구조, 대량 이동 요청은 별도 보호 로직으로 제한할 수 있다.
- Editor 및 Block 서버의 편집/공유/복구/게시 이벤트는 `audit-log` 공통 모듈로 발행한다.
