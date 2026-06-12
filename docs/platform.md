# Platform 사용 기준

`editor-service`는 현재 문서/블록 runtime 경계를 `platform-runtime`, `platform-security`, `platform-governance`, `platform-resource` `4.0.0` 위에 올려 둡니다.

## 현재 적용 요약

- 기본 BOM은 `platform-runtime-bom 4.0.0`입니다.
- mainline starter는 `platform-security-starter`, `platform-governance-starter`, `platform-resource-starter`, `platform-resource-jdbc`입니다.
- sanctioned add-on은 `platform-security-governance-bridge`, `platform-resource-governance-bridge`, `platform-security-web-api`입니다.
- local/runtime 보조 모듈은 `platform-resource-support-local`을 사용합니다.

## 현재 구현 포인트

- `SecurityFailureResponseWriter`
- `GovernanceAuditSink`
- `PlatformRateLimitPort`
- `ResourceContentStore`
- `ResourceLifecyclePublisher`
- `ResourceCatalog`

현재 서비스는 위 surface를 기준으로 문서/블록 API, resource lifecycle, governance audit 연결을 조립합니다.

## prod resource backing 현재 상태

- 현재 prod filesystem backing은 service-owned `ResourceContentStore` 구현으로 유지합니다.
- 이 상태는 임시 경계로 보고, `editor v2` rollout에서는 `platform-resource` optional support module로 승격하는 것을 목표로 합니다.
- 즉 장기적으로는 generic filesystem backing bean을 서비스가 직접 소유하지 않고, 서비스는 `platform.resource.*` 설정과 kind policy만 소유하는 방향을 기준으로 합니다.

## 금지/주의

- raw `file-storage-*` 좌표를 service public compile contract로 설명하지 않습니다.
- `platform-resource-core` 구현 타입을 서비스 코드가 직접 생성하는 방향은 피합니다.
- governance/security/resource bridge는 explicit add-on으로만 유지하고, base starter에 암묵적으로 섞어 설명하지 않습니다.

## 검증

```bash
./gradlew :documents-core:compileJava :documents-api:compileJava :documents-infrastructure:compileJava :documents-boot:compileJava
./gradlew -q :documents-boot:dependencyInsight --configuration runtimeClasspath --dependency platform-security-starter
./gradlew -q :documents-boot:dependencyInsight --configuration runtimeClasspath --dependency platform-governance-starter
./gradlew -q :documents-boot:dependencyInsight --configuration runtimeClasspath --dependency platform-resource-starter
```

