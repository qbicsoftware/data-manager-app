# Checklist: FEAT-DATSET-14 Implementation Review

> Companion to [`feat-datset-14-invenio-rdm-credentials.md`](feat-datset-14-invenio-rdm-credentials.md).
> Each checkpoint is self-contained: one logical implementation step that can be built, committed,
> and reviewed in one pass. Reviewers should be able to walk through in the order listed and
> confidently confirm each boundary before moving on to the next.

**Legend:**
- 🔒 — security-critical check
- 🧱 — layer-boundary / architecture check
- 📐 — ADR compliance check

---

## Checkpoint 1 — Database migration: `user_external_credential`

> **Plan ref:** §4 Task 1  
> **Deliverables:** `sql/migrations/create-user-external-credential.sql`, updated `sql/complete-schema.sql`  
> **Blocks:** all subsequent checkpoints

### Reviewer checks

- [ ] 📐 Table name is `user_external_credential` (generalised, not `user_invenio_rdm_credential`)
- [ ] 📐 Columns include `source_type varchar(32) NOT NULL` alongside `instance_id` (consistent with `associated_dataset` table)
- [ ] Unique key is on `(user_id, source_type, instance_id)` — three columns, not two
- [ ] `encrypted_token` is `VARBINARY(512)` with a comment documenting the AES-GCM blob layout (`nonce ‖ ciphertext ‖ tag`)
- [ ] `status` is `VARCHAR(16)` with only two values documented: `VALID`, `INVALIDATED`
- [ ] Migration file header references FEAT-DATSET-14 and the ADRs (0002, 0003)
- [ ] Migration is idempotent (uses `CREATE TABLE IF NOT EXISTS`)
- [ ] `complete-schema.sql` was updated in parallel with the migration (kept in sync per project convention)

---

## Checkpoint 2 — Vault configuration property

> **Plan ref:** §4 Task 9  
> **Deliverables:** new entry in `application.properties`, ops runbook note  
> **Blocks:** Checkpoint 3

### Reviewer checks

- [ ] Property key: `qbic.security.vault.external-credential.key-alias=external-credential-master-key` (or documented alternative)
- [ ] Property comment clearly states the key is dedicated to credential encryption and is distinct from the existing OpenBIS vault entries
- [ ] Documentation notes the ops prerequisite: the PKCS12 keystore must have a corresponding entry at deploy time before the application can start
- [ ] 🔒 No default/placeholder AES key value is committed (the key material comes from the environment/vault deployment, not from `application.properties`)

---

## Checkpoint 3 — `ExternalCredentialEncryptor` (AES-256-GCM)

> **Plan ref:** §4 Task 2  
> **Files:** `infrastructure/external/ExternalCredentialEncryptor.java` (interface),  
> `infrastructure/external/AesGcmCredentialEncryptor.java` (impl)  
> **Note:** Interface lives in the **provider-agnostic** `infrastructure/external/` package — not under `infrastructure/external/invenio/`  
> **Blocks:** Checkpoints 4, 6, 7

### Reviewer checks

- [ ] 🧱 Interface is in `life.qbic.projectmanagement.infrastructure.external` (not under a provider-specific sub-package) — encryption is provider-agnostic
- [ ] 🔒 Algorithm is `AES/GCM/NoPadding`, 256-bit key, 96-bit nonce, 128-bit tag
- [ ] 🔒 A **fresh random 12-byte nonce is generated per encryption call** (verified via `SecureRandom` or `SecureRandom.getInstanceStrong()` — no nonce caching, no counter reuse)
- [ ] 🔒 Output format is `nonce (12 bytes) ‖ ciphertext ‖ tag (16 bytes)` — documented in Javadoc
- [ ] 🔒 `decrypt()` returns `char[]` (not `String`) to enable zeroing by caller
- [ ] 🔒 Master key is loaded **once** at bean creation and held as a `javax.crypto.SecretKey` field
- [ ] 🔒 Master key value is never returned from any method, never logged, never included in `toString()`/`hashCode()`/exception messages
- [ ] 🔒 Exception messages do not include plaintext token value
- [ ] 🔒 Encryptor constructor validates that the master key material is exactly 32 bytes (AES-256); rejects AES-128/AES-192 keys with a clear error message
- [ ] 🔒 Spring configuration (`InvenioRdmConfiguration.credentialEncryptor`) reads the vault entry as a Base64-encoded string, decodes it, and validates the decoded length is exactly 32 bytes before constructing the `SecretKey`
- [ ] 🔒 Vault provisioning instructions document that the key must be stored Base64-encoded (e.g., `openssl rand -base64 32`)
- [ ] Spock spec `AesGcmCredentialEncryptorSpec` exists and covers: round-trip, nonce uniqueness, wrong-key failure, corrupted-data failure, key-size validation (rejects too-short and too-long keys)
- [ ] Vault integration is lazy or fail-fast — missing alias throws a clear `DataManagerVaultException` at startup, not a `KeyStoreException` deep in a request

---

## Checkpoint 4 — Domain model: `UserExternalCredential` + repository port

> **Plan ref:** §4 Task 3  
> **Files:** `domain/model/associated_dataset/UserExternalCredential.java`,  
> `domain/model/associated_dataset/CredentialStatus.java`,  
> `domain/model/associated_dataset/repository/UserExternalCredentialRepository.java`  
> **Blocks:** Checkpoints 6, 7

### Reviewer checks

- [ ] 🧱 All classes live in `life.qbic.projectmanagement.domain.model.associated_dataset` or its `repository` sub-package
- [ ] 🧱 **Zero imports from `*.infrastructure.*`** in these files (domain must not depend on infrastructure)
- [ ] 🧱 **No `char[]`, `String` token, or any plaintext credential field** on the entity — only `byte[] encryptedToken`
- [ ] `sourceType` is the existing `SourceType` enum (reused from `AssociatedDataset` — consistency with the aggregate)
- [ ] Entity carries: `id`, `userId`, `sourceType`, `instanceId`, `encryptedToken`, `status`, `createdAt`, `updatedAt`
- [ ] `CredentialStatus` has exactly two values: `VALID`, `INVALIDATED`
- [ ] Repository interface is in `repository` sub-package (not directly under `domain/model/`)
- [ ] Repository queries are parameterised by `sourceType` (the dispatcher needs to scope lookups by source)
- [ ] Entity is final or immutable where state transition is explicit (no public `setStatus()` that any caller can invoke from outside the domain)

---

## Checkpoint 5 — JPA entity + Spring Data repository implementation

> **Plan ref:** §4 Task 4  
> **Files:** `infrastructure/dataset/associated/UserExternalCredentialEntity.java`,  
> `infrastructure/dataset/associated/UserExternalCredentialJpaRepository.java`,  
> `infrastructure/dataset/associated/UserExternalCredentialRepositoryImpl.java`  
> **Prerequisites for merge:** Checkpoints 1, 3, 4 pass

### Reviewer checks

- [ ] 🧱 `@Entity` is mapped to table `user_external_credential` (matching Checkpoint 1)
- [ ] 🧱 No domain type is annotated with `@Entity` — the JPA entity is a separate class from the domain entity
- [ ] `encryptedToken` is mapped as `byte[]` / `@Lob` with matching column definition
- [ ] `sourceType`, `status` are `@Enumerated(EnumType.STRING)`
- [ ] Unique constraint annotation matches the migration: `(userId, sourceType, instanceId)`
- [ ] Bidirectional mapping class exists: `UserExternalCredentialEntity ↔ UserExternalCredential`
- [ ] 🔒 Mapping never exposes the decrypted token — domain entity always holds `byte[]`, never a `String` or `char[]` derived from decryption
- [ ] Repository implementation correctly maps between entity and domain, including the `sourceType` parameter in queries
- [ ] `deleteByUserIdAndSourceTypeAndInstanceId` matches the method signature in the domain repository interface (Checkpoint 4)

---

## Checkpoint 6 — `InvenioRdmClient.getAuthenticatedUser()` + `InvenioRdmCredentialValidatorAdapter`

> **Plan ref:** §4 Task 5a–5c  
> **Files:** `infrastructure/external/invenio/InvenioRdmClient.java` (addition),  
> `infrastructure/external/CredentialValidatorAdapter.java`,  
> `infrastructure/external/invenio/InvenioRdmCredentialValidatorAdapter.java`  
> **Blocks:** Checkpoint 8

### Reviewer checks

- [ ] 🔒 `getAuthenticatedUser(instanceUrl, authHeader)` uses the **spec-defined path** `GET /api/users` with `BearerAuth` (operationId: `getAUserById`) — not a guessed `/api/me`
- [ ] 🔒 The response DTO `AuthenticatedUserResponse` uses `@JsonIgnoreProperties(ignoreUnknown = true)` — the implementation must not rely on specific response fields, only the HTTP 200 status
- [ ] 🔒 Token is handled as `char[]` inside `InvenioRdmCredentialValidatorAdapter.validate()` — copied, used within a try/finally, zeroed with `Arrays.fill(tokenCopy, '\0')` in the finally block
- [ ] 🔒 Token is **never** converted to `String` and stored — the `"Bearer " + token` is built only for the HTTP request header, in local scope
- [ ] 🔒 Error mapping: 401/403 → returns `false` (token invalid); other 4xx → rethrown; 5xx/transient → wraps in `CredentialValidationException`
- [ ] 🧱 `CredentialValidatorAdapter` interface is in the **provider-agnostic** `infrastructure/external/` package — each provider has its own adapter; the dispatcher is source-type-aware, not provider-aware
- [ ] Existing retry policy (3 attempts, exponential backoff, HTTP 5xx/429, no retry on 401/403/404) is reused from `InvenioRdmClient.getWithRetry()`
- [ ] Tests:
  - `InvenioRdmCredentialValidatorAdapterSpec`: 200 → `true`; 401 → `false`; 403 → `false`; transient → exception; token zeroed after call
  - `InvenioRdmClientSpec.getAuthenticatedUser`: request URL, response DTO parsing, auth header threading

---

## Checkpoint 7 — `SourceTypeDispatchingCredentialValidator`

> **Plan ref:** §4 Task 5d  
> **Files:** `infrastructure/external/SourceTypeDispatchingCredentialValidator.java`  
> **Implementation:** `ExternalCredentialValidator` (application port)  
> **Prerequisites for merge:** Checkpoint 6 passes  
> **Blocks:** Checkpoint 8

### Reviewer checks

- [ ] 🧱 Implements the application-layer port `ExternalCredentialValidator` — bridge from infrastructure into application
- [ ] 🧱 Adapter map is `Map<SourceType, CredentialValidatorAdapter>` — keyed by source type enum, not by string
- [ ] Constructor validates that at least one adapter is registered (fail-fast at startup, not per-request)
- [ ] Map copy is defensive (`Map.copyOf(adapters)`) — cannot be mutated after construction
- [ ] 🔒 Unknown `SourceType` throws `CredentialValidationException` with a clear message — no silent fallback, no "best effort" dispatch
- [ ] 🔒 Token `char[]` is forwarded to the selected adapter — not copied again, not accumulated in a loggable collection
- [ ] Test: `SourceTypeDispatchingCredentialValidatorSpec`
  - Dispatches to correct adapter for a registered `SourceType`
  - Throws `CredentialValidationException` for unregistered `SourceType`
  - Token passed to adapter unchanged (verifies the adapter reference receives the call)

---

## Checkpoint 8 — Application service: `ExternalCredentialService`

> **Plan ref:** §4 Task 6  
> **Files:** `application/associated_dataset/ExternalCredentialService.java` (interface),  
> `application/associated_dataset/DefaultExternalCredentialService.java` (impl)  
> **Prerequisites for merge:** Checkpoints 3, 5, 6, 7 pass

### Reviewer checks

- [ ] 🧱 Interface lives in `application` layer — no `infrastructure.*` imports
- [ ] 🧱 No plaintext token (`char[]`) escapes the `addCredential()` method beyond validation + storage: the method accepts `char[]`, passes it to the validator, then to the encryptor, then zeroes it in a `finally` — the return type is `AddCredentialResult`, not `char[]`
- [ ] 🔒 `addCredential()` zeroes the token in a `finally` block — verified by test (mock validator/encryptor; `Arrays.fill` call asserted via `char[]` content post-return)
- [ ] 🔒 Return type `AddCredentialResult` is a **sealed interface** with only `Success`, `InvalidToken`, `ServiceError`, `UnknownInstance` — no plaintext, no encrypted bytes
- [ ] 🔒 `listCredentialStatuses()` returns `CredentialStatusView` records that contain **no token value** — only `sourceType`, `instanceId`, `instanceDisplayName`, `configured`, `status`
- [ ] `addCredential()` resolves `SourceType` from the registry via `instanceId` — caller doesn't need to know the source type (decoupled from view-layer concerns)
- [ ] `addCredential()` flow (in order):
  1. Resolve instance + source type from `SourceInstanceRegistry`
  2. Call `ExternalCredentialValidator.validateToken(sourceType, config, token)`
  3. On failure → return `InvalidToken` (no persistence)
  4. On success → `encryptor.encrypt(token)` → `repository.save(credential)` → return `Success`
  5. Zero token in `finally`
- [ ] `addCredential()` handles idempotency: if a credential for `(userId, sourceType, instanceId)` already exists, the existing row is **replaced** (updated) with the new token — not rejected, not duplicated
- [ ] Test: `DefaultExternalCredentialServiceSpec`
  - Valid token → persisted; encrypted blob stored; `Success` returned
  - Invalid token → `InvalidToken` returned; repository **not** called to save
  - Unknown instance → `UnknownInstance` returned
  - Existing credential → replaced
  - Transient validator failure → wrapped as `ServiceError`; zeroing still occurred
  - Token content is all-zero after return (zeroing verification)

---

## Checkpoint 9 — Wire token resolution into `InvenioRdmDatasetSource`

> **Plan ref:** §4 Task 7  
> **Files:** `infrastructure/external/invenio/InvenioRdmDatasetSource.java` (modified constructor and methods)  
> **Prerequisites for merge:** Checkpoints 3, 5 pass

### Reviewer checks

- [ ] 🔒 Token is resolved **inside** the `search()` / `resolveMetadata()` methods — not passed in as a parameter (the decryption boundary is enforced here)
- [ ] 🔒 Resolved token is `char[]`, decrypted by `encryptor.decrypt()`, used only to build `"Bearer " + token` for the HTTP `Authorization` header, then **zeroed in a `finally` block** — verified by test
- [ ] 🔒 No token value appears in any log statement, exception message, or return object
- [ ] Look-up uses `repository.findByUserIdAndSourceTypeAndInstanceId(actingUserId, SourceType.INVENIO_RDM, config.id())` — correctly scoped by source type
- [ ] No-token path: lookup returns empty → `authHeader` is `null` → client issues request without `Authorization` (public records only) — verified by test
- [ ] Constructor now takes `UserExternalCredentialRepository` and `ExternalCredentialEncryptor` as additional parameters
- [ ] Test: `InvenioRdmDatasetSourceSpec` (updated)
  - Search with user who has token → HTTP request carries `Authorization: Bearer ...`
  - Search with user who has no token → HTTP request carries no `Authorization`
  - Token `char[]` content is all-`\0` after `search()` returns (zeroing assertion)
  - Same pattern for `resolveMetadata()`

---

## Checkpoint 10 — UI: External Providers view + Add Token dialog

> **Plan ref:** §4 Task 8  
> **Files:** `views/account/ExternalProvidersMain.java`, `views/account/ExternalProvidersComponent.java`,  
> `views/account/AddExternalCredentialTokenDialog.java`,  
> `frontend/themes/datamanager/components/external-providers.css`  
> **Prerequisites for merge:** Checkpoint 8 (service) passes

### Reviewer checks

- [ ] 🧱 View class uses `@Route("/account/external-providers", layout = UserMainLayout.class)` — sits alongside `/profile` and `/account` routes
- [ ] 🧱 View has `@PermitAll` — no additional ACL check is performed (credential management is user-scoped: the user only sees their own credentials)
- [ ] 🧱 View calls only `ExternalCredentialService` (application service) — **no direct access to `SourceInstanceRegistry`, `ExternalCredentialValidator`, or the repository** (layer discipline)
- [ ] 🔒 `AddExternalCredentialTokenDialog` uses `PasswordField` — no character echo
- [ ] 🔒 Dialog does not retain the token value in component state after submission — token is passed to the service and the field is immediately cleared (`passwordField.clear()` in both success and failure paths)
- [ ] 🔒 No token value is ever stored in `localStorage`, `sessionStorage`, Vaadin component state, or query parameters
- [ ] 🔒 Toast notification on validation failure: generic message ("Token validation failed. Please check your token and try again.") — no upstream error details, no token value, no internal error code
- [ ] Benefit paragraph (AC-6) is visible and explains the purpose of adding a token (connecting access-restricted datasets to a project)
- [ ] Instance list is loaded from `ExternalCredentialService.listCredentialStatuses(userId)` — not hard-coded instances
- [ ] Status indicators differentiate:
  - 🟢 Connected / VALID
  - 🔴 Not connected / NOT_CONFIGURED
  - 🟡 (or neutral) INVALIDATED (future)
- [ ] External link points to `{baseUrl}/account/settings/applications/` for InvenioRDM instances — verified manually

---

## Checkpoint 11 — Spring wiring + integration smoke test

> **Plan ref:** §4 Task 10  
> **Files:** `datamanager-app/configuration/InvenioRdmConfiguration.java` (updated)  
> **Prerequisites for merge:** All prior checkpoints pass

### Reviewer checks

- [ ] 🔒 All beans wired in a single `@Configuration` class with explicit constructor injection — no wildcard `@Autowired` field injection
- [ ] `SourceTypeDispatchingCredentialValidator` is registered with exactly one adapter for `SourceType.INVENIO_RDM` (today) — comment in `@Bean` method lists future adapters
- [ ] `ExternalCredentialService` is registered as the application-layer service (not `InvenioRdmCredentialService` — name change confirmed)
- [ ] `InvenioRdmDatasetSource` bean is updated to receive the repository + encryptor (Checkpoint 9 dependency)
- [ ] 🔒 Encryptor bean fails-fast at startup if the PKCS12 vault entry `external-credential-master-key` is missing
- [ ] 🔒 Token encryptor uses the **dedicated** master-key alias from `qbic.security.vault.external-credential.key-alias` — not shared with the OpenBIS vault entries
- [ ] `ExternalCredentialValidator` bean is typed as the composite dispatcher, not the individual adapter
- [ ] Smoke test (or `@SpringBootTest` integration test) runs end-to-end: token add → validation against a mock `/api/users` → encrypted write → read-back → decryption → search with `Authorization` header

---

## Final review gate

Before merging the feature branch, walk through the full security checklist:

| Check | Where it's enforced |
|---|---|
| 🔒 Plaintext token is `char[]`, never `String` | Checkpoints 3, 6, 8, 9 |
| 🔒 Token zeroed with `Arrays.fill(char[], '\0')` in `finally` | Checkpoints 6, 8, 9 |
| 🔒 Master key in PKCS12; AES-256-GCM in DB | Checkpoints 2, 3 |
| 🔒 Decryption boundary: token decrypted only in infrastructure | Checkpoints 5, 8, 9 |
| 🔒 Application service never returns/holds token bytes | Checkpoint 8 |
| 🔒 UI never persists token in component state or storage | Checkpoint 10 |
| 🔒 Log statements never contain token value | Checkpoints 3, 6, 9 |
| 🔒 Validation endpoint is the spec-defined `GET /api/users` | Checkpoint 6 |
| 🔒 Generic user-facing error messages (no upstream leakage) | Checkpoints 6, 10 |
| 📐 Provider-agnostic composite dispatcher pattern | Checkpoints 3, 6, 7 |
| 📐 Generalised table name `user_external_credential` | Checkpoint 1 |
| 📐 `SourceType` reused from `AssociatedDataset` aggregate | Checkpoint 4 |
| 📐 ADR-0002 D1 decryption boundary enforced | Checkpoints 5, 6, 9 |
| 📐 ADR-0002 I2 admin-configured instances (no UI entry) | Checkpoint 10 |
| 📐 ADR-0002 T1 never-borrow-credentials (user's own token, per-instance) | Checkpoints 4, 9 |
