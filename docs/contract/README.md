# Editor Contract

현재 `editor-service`의 `documents-api` 구현 기준으로 v1 문서/블록 계약을 정의하고, v2 확장 축은 shared operation core + separated persistence로 둔다.

## Source
| 항목            | 값                                        |
|---------------|------------------------------------------|
| Repo          | https://github.com/jho951/editor-service |
| Branch        | `main`                                   |
| Contract Lock | `contract.lock.yml`                      |

## 현재 v1
- [Schema v1](schema-v1.md)
- [Rules v1](rules-v1.md)
- [API v1](api.md)
- [OpenAPI v1](https://github.com/jho951/service-contract/blob/main/artifacts/openapi/editor-service.upstream.v1.yaml)
- [Persistence & Migration](db-migration.md)
- [Authorization & Ownership](authz.md)
- [Operations Semantics](operations.md)
- [Error Contract](errors.md)
- [Security Contract](security.md)
- [Operating Contract](ops.md)
- [Cache Contract](cache.md)
- [Common Audit Contract](https://github.com/jho951/service-contract/blob/main/shared/audit.md)

## 운영 배포 자산

- EC2 image-only 배포 기준 산출물은 구현 repo의 `deploy/ec2/` 아래에 둔다.
- 기본 파일은 `docker-compose.yml`, `.env.production.example`, `README.md`다.

## 현재 확장
- [V2 Extension](v2-extension.md)

## 미래 Node v2
- [Schema v2](schema-v2.md)
- [Rules v2](rules-v2.md)
- [API v2](api-v2.md)
- 현재 OpenAPI artifact는 v1만 발행했고, v2 경로/shape는 위 문서를 기준으로 관리한다.

## 범위
- v1 활성 범위는 `Document`와 `Block`이다.
- `Workspace`는 backup 전용이며 v1 활성 API에 노출하지 않는다.
- 외부 노출 경로는 Gateway 기준 `/v1/documents/**`, `/v1/admin/**`이다.
- 서비스 내부 경로는 `/documents/**`, `/admin/**`이다.
- v1 응답은 `GlobalResponse` envelope을 사용한다.
- `Document`는 `visibility`, `trash`, `restore`, `move`, `transactions`를 포함한다.
- `Block`은 top-level `TEXT`만 지원하며, 현재 저장/이동/삭제는 transaction 기반이다.
- TEXT 본문은 `rich_text` JSON을 사용하고, 현재 구현은 optional `content.blockType`으로 `paragraph`, `heading1`, `heading2`, `heading3`를 허용한다.
- `segment`는 여전히 `text`, `marks`만 허용한다. 블록 subtype은 `segment`가 아니라 `content.blockType`에 둔다.
- editor save batch는 여러 단일 블록 operation의 순차 적용 모델이다. 현재 v1에는 여러 블록 selection 자체를 나타내는 계약이 없다.
- 여러 블록 delete나 subtype 일괄 변경은 클라이언트가 여러 단일 operation으로 풀어 보낼 수 있지만, 여러 블록을 한 묶음으로 보존해 move하는 semantics는 v1에 없다.
- `Block restore`는 현재 v1에 없다.
- main 확장 목표는 shared operation core와 separated persistence를 동시에 유지하는 것이다.
- `parentId` / `sortKey` 기반 트리 구조와 rich text payload 규칙을 유지한다.

## Current Platform Runtime
- `editor-service` 현재 구현은 `platform-runtime-bom 4.0.0`을 두되, `platform-governance/security/resource BOM 4.0.0`과 `platform-security-governance-bridge`, `platform-resource-governance-bridge 4.0.0`을 함께 pin한다.
- 런타임 모듈은 `platform-governance-starter`, `platform-security-starter`, `platform-security-web-api`, `platform-resource-starter`, `platform-resource-jdbc`이며, local resource backing은 runtime `platform-resource-support-local`이 맡는다.
- `DocumentsGovernanceAuditConfiguration`은 `GovernanceAuditSink`를, `DocumentsPlatformOperationalConfiguration`은 prod Redis 기반 `PlatformRateLimitPort`를 제공한다.
- `DocumentsResourcePlatformConfiguration`은 prod profile에서 service-owned filesystem `ResourceContentStore`를 제공하고, compile surface에는 `file-storage-core`를 직접 남기지 않는다.
- `DocumentsRequestContextConfiguration`은 `platform-security-web-api`의 `SecurityFailureResponseWriter`를 구현해 editor 응답 envelope로 변환한다.
- `DocumentsResourcePlatformConfiguration`은 shared `operationalProfileResolver`만 제공한다. service가 `platform-resource-core` 구현을 직접 생성하지는 않는다.
- `platform.resource.storage.root-directory`는 local fallback 저장 위치만 정하고, 운영 storage backing은 `ResourceContentStore` SPI로 교체한다.
- `documents-boot`는 `hibernate.type.preferred_uuid_jdbc_type=CHAR`를 사용해 현재 UUID 영속 바인딩을 문자열 기반으로 고정한다.

## Editor v2 And Platform Rollout
- 현재 prod filesystem backing은 구조 위반은 아니지만 최종 형태는 아니다. generic filesystem backing 구현을 서비스가 소유하고 있기 때문이다.
- `editor v2` rollout은 문서/블록 API 확장만이 아니라 `platform-resource` 운영 backing 승격도 함께 다룬다.
- 목표 상태는 service-owned `DocumentsResourcePlatformConfiguration` 제거다.
- `editor-service`는 v2에서 `platform-resource`가 제공하는 optional prod backing module만 추가하고, 서비스는 `platform.resource.*` 설정과 kind 정책만 소유한다.
- 후보 모듈 이름은 `platform-resource-support-filesystem`이며, 실제 이름은 platform repo에서 최종 확정한다.
- 이 작업이 끝나면 `editor-service`는 local/dev의 `platform-resource-support-local`, 운영의 platform-owned filesystem support module을 구분해 소비한다.

## 원칙
1. v1은 현재 운영 기준이다.
2. main 확장은 shared operation core + separated persistence 설계안이다.
3. `Workspace`는 v1에서 backup 전용이며, 재설계 전까지 활성 API에 노출하지 않는다.
4. Node 영속 통합은 별도 미래 Node v2 문서로만 유지한다.
5. 문서/블록의 현재 계약은 v1 문서를 따른다.
6. 권한/보안/운영/캐시/에러 규칙은 현재 구현과 함께 맞춘다.
7. backup Workspace 관련 코드는 `backup/workspace/` 아래에서만 보관한다.
8. capability truth는 `authz-service`, 공개 범위는 `user-service`, 최종 실행은 `Editor`가 강제한다.
