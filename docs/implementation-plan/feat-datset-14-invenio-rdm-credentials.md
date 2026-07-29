# Implementation Plan: FEAT-DATSET-14 — Add credentials for an InvenioRDM instance

> **Story:** [FEAT-DATSET-14](https://github.com/qbicsoftware/data-manager-app/issues/1478)  
> **Parent Feature:** [FEAT-DATASET-CONNECTION](https://github.com/qbicsoftware/data-manager-app/issues/1466) (#1466)  
> **Requirement:** `DATA-R-03` (InvenioRDM Credential Management)  
> **Branch:** `feature/feat-datset-14-invenio-rdm-credentials`  
> **Depends on:** No prerequisite — this story is a prerequisite for FEAT-DATSET-05 (connecting restricted datasets) and FEAT-DATSET-06 (viewing restricted datasets)  
> **ADRs:** [ADR-0002](../adr/0002-invenio-rdm-api-client-credentials.md) (credential storage + client), [ADR-0003](../adr/0003-connection-lifecycle-stewardship.md) (lifecycle)

---

## 1. Story Summary

> *As a researcher, I want to provide my access token for an available InvenioRDM instance, so that I can connect access-restricted resources in a project.*

### Acceptance Criteria

| # | Given | When | Then |
|---|---|---|---|
| AC-1 | User is in account settings | Navigates to "External Providers" | System offers a way to configure InvenioRDM instances |
| AC-2 | User is configuring instances | Views available instances | System displays the list of configured InvenioRDM instances (e.g. Zenodo, FDAT) |
| AC-3 | User pastes/enters a token | Submits | Token is validated against InvenioRDM API via `GET /api/users` ([`getAUserById`](https://inveniosoftware.github.io/invenio-openapi/), operationId: `getAUserById`) — the spec-defined authenticated-user endpoint; no parameters, security: `BearerAuth`; returns `200` with a JSON object on success, `401` on invalid/expired token |
| AC-4 | Validation fails | Token invalid/expired | Token **not** added; user informed it is invalid |
| AC-5 | Validation succeeds | Token valid | User informed token valid; instance now connected |
| AC-6 | User views the section | Sees benefit description | System explains that providing a token enables connection of access-restricted datasets |

---

## 2. Architectural Context

### 2.1 What already exists

| Component | Layer | Module | Status |
|---|---|---|---|
| `DatasetSource` port (`search`, `resolveMetadata`) | Application | `project-management` | ✅ Done |
| `InvenioRdmDatasetSource` adapter | Infrastructure | `project-management-infrastructure` | ✅ Done (public-only; `actingUserId` param unused) |
| `InvenioRdmClient` interface + `InvenioRdmHttpClient` | Infrastructure | `project-management-infrastructure` | ✅ Done (`authHeader` param exists in retry flow but never populated by caller) |
| `SourceInstanceRegistry` + `PropertiesBackedSourceInstanceRegistry` | Application/Infrastructure | — | ✅ Done |
| `InvenioRdmProperties` (config binding) | Infrastructure | `project-management-infrastructure` | ✅ Done |
| `InvenioRdmConfiguration` (Spring bean wiring) | App | `datamanager-app` | ✅ Done |
| `associated_dataset` table + migration | DB | `sql/migrations/` | ✅ Done |
| `DataManagerVault` (PKCS12 keystore) | Infrastructure | `project-management-infrastructure` | ✅ Done |
| `/profile` and `/account` Vaadin routes | Views | `datamanager-app` | ✅ Done |
| `InstanceConfig` + `SourceInstanceDescriptor` value objects | Application | `project-management` | ✅ Done |

### 2.2 What needs to be built

| Component | Layer | Module | Plan section |
|---|---|---|---|
| `user_external_credential` DB table + migration | Schema | `sql/migrations/` | §4 Task 1 |
| `ExternalCredentialEncryptor` (AES-256-GCM) | Infrastructure | `project-management-infrastructure` | §4 Task 2 |
| `UserExternalCredential` domain entity | Domain | `project-management` | §4 Task 3 |
| `UserExternalCredentialRepository` domain interface | Domain | `project-management` | §4 Task 3 |
| JPA entity + Spring Data repo implementation | Infrastructure | `project-management-infrastructure` | §4 Task 4 |
| `InvenioRdmClient.getAuthenticatedUser()` — new endpoint method | Infrastructure | `project-management-infrastructure` | §4 Task 5 |
| Composite credential validator (dispatcher + per-provider adapters) | Infrastructure | `project-management-infrastructure` | §4 Task 5 |
| `ExternalCredentialValidator` port (application) | Application | `project-management` | §4 Task 5 |
| `ExternalCredentialService` (application orchestration) | Application | `project-management` | §4 Task 6 |
| Wire token resolution into `InvenioRdmDatasetSource` | Infrastructure | `project-management-infrastructure` | §4 Task 7 |
| `ExternalProvidersMain` Vaadin route (`/account/external-providers`) | Views | `datamanager-app` | §4 Task 8 |
| Dedicated master key entry in PKCS12 vault + config property | Config | `datamanager-app` | §4 Task 9 |
| Spring wiring: new beans in `InvenioRdmConfiguration` | App | `datamanager-app` | §4 Task 10 |
| Unit tests (Spock) + integration tests | Test | — | §4 Task 11 |

---

## 3. Security Model

### 3.1 Threat model

| Threat vector | Mitigation |
|---|---|
| **At-rest exposure** (DB breach) | AES-256-GCM with per-token random 12-byte nonce. Dedicated master key in PKCS12 vault, deployed to HA nodes at deploy time. |
| **In-flight** | HTTPS only. All target instances (Zenodo, FDAT) are HTTPS. No plaintext over network. |
| **In-memory** | Plaintext token exists as `char[]` only in the infrastructure method performing the HTTP call. Zeroed in `finally` block (`Arrays.fill(token, '\0')`). |
| **Layer boundary leak** | Application layer never holds, logs, or serialises the plaintext token. Only `actingUserId` and `instanceId` cross layer boundaries. |
| **Logging leak** | Token values never logged. Logs carry user ID + instance ID only. |
| **Error message leak** | Generic user-facing errors ("Token validation failed"). Raw upstream errors to server logs only. |
| **Replay / unauthorized access to other users' tokens** | Repository queries enforce `user_id` filter. No cross-user lookup path exists. |
| **Admin / ops plaintext exposure** | Master key is in PKCS12 vault; ops has keystore access but the AES-GCM blobs require both key + nonce. Token values are not visible in DB dumps without the master key. |

### 3.2 Decryption boundary (ADR-0002 D1)

```
┌───────────────────────────────────────────────────────────────────┐
│ Views (UI)                                                         │
│ → Never sees token. Only status (VALID/INVALIDATED) per instance. │
├───────────────────────────────────────────────────────────────────┤
│ Application layer (services)                                       │
│ → Passes (userId, instanceId). Never decrypts, never holds token  │
│   beyond the scope of the add/validate operation.                 │
├───────────────────────────────────────────────────────────────────┤
│ Infrastructure layer (adapters)                                    │
│ → Decrypts token to char[]. Passes as Bearer header in HTTP call. │
│ → Zeroes char[] in finally block. Returns SearchResult, not token.│
│ → Validates tokens via per-provider adapters dispatched by type.  │
└───────────────────────────────────────────────────────────────────┘
```

### 3.3 Never-borrow-credentials (ADR-0002 T1 + ADR-0003 C1)

Each user owns their own tokens per instance. The system never uses one user's token to fulfil another user's action. Forensic correctness: external platform audit logs accurately reflect who initiated each call.

---

## 4. Task Breakdown

---

### Task 1: Database schema — `user_external_credential`

**Files:**
- New: `sql/migrations/create-user-external-credential.sql`
- Update: `sql/complete-schema.sql` (append the table definition)

**Schema (per ADR-0002 S2):**
```sql
-- =============================================================================
-- Migration: Create `user_external_credential` table
-- Feature:     FEAT-DATSET-14 — Add credentials for an InvenioRDM instance
-- ADRs:        0002 (credential storage), 0003 (lifecycle)
--
-- Stores per-user, per-instance personal access tokens for external providers.
-- The table is source-agnostic at the schema level: `source_type` and
-- `instance_id` together identify the provider and instance. Today the only
-- rows will be INVENIO_RDM / zenodo | fdat. Future providers add rows with
-- a different source_type — no schema change required.
--
-- Tokens are AES-256-GCM encrypted at rest (nonce + ciphertext + tag).
-- The master key is stored in the PKCS12 vault at deploy time.
--
-- Unique constraint on (user_id, source_type, instance_id): one token per
-- user per instance. source_type is included in the unique key to keep the
-- door open for the same instance_id value across different providers in
-- a hypothetical future scenario.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `user_external_credential`
(
    `id`                varchar(36)     NOT NULL,
    `user_id`           varchar(255)    NOT NULL    COMMENT 'DM user ID',
    `source_type`       varchar(32)     NOT NULL    COMMENT 'e.g. INVENIO_RDM — matches SourceType enum',
    `instance_id`       varchar(64)     NOT NULL    COMMENT 'matches InstanceConfig.id (e.g. zenodo, fdat)',
    `encrypted_token`   varbinary(512)  NOT NULL    COMMENT 'AES-256-GCM: 12-byte nonce + ciphertext + 16-byte auth tag',
    `status`            varchar(16)     NOT NULL    COMMENT 'VALID | INVALIDATED',
    `created_at`        timestamp(3)    NOT NULL,
    `updated_at`        timestamp(3)    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_src_instance` (`user_id`, `source_type`, `instance_id`),
    KEY `idx_cred_user` (`user_id`),
    KEY `idx_cred_user_src` (`user_id`, `source_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
```

**Design rationale for generalised table name:**
- `user_external_credential` (not `user_invenio_rdm_credential`) — the table stores credentials for any external provider. Adding a second provider type (e.g. LIMS) requires zero schema changes.
- `source_type` column mirrors the `source_type` on `associated_dataset` — consistency with the existing aggregate and repository queries.
- `instance_id` references `InstanceConfig.id` which is already provider-agnostic.
- The unique key includes `source_type` so that theoretically the same `instance_id` string could coexist under different provider types, and practically to keep the schema fully generalised.

**Notes:**
- `encrypted_token` is `VARBINARY(512)` — sufficient for AES-GCM nonce (12) + encrypted token (variable) + tag (16). Typical PATs are 64–128 hex chars.
- `instance_id` is plain-text, admin-configured, not secret.
- `status` has only two values in v1: `VALID` and `INVALIDATED`. Updated only on explicit user action (AC-3/AC-4), never from a failed sync (ADR-0002 §9).

---

### Task 2: Encryption service — `ExternalCredentialEncryptor`

**Files:**
- Interface: `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/external/ExternalCredentialEncryptor.java`
- Implementation: `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/external/AesGcmCredentialEncryptor.java`

**Note:** The interface lives in a **provider-agnostic** package (`infrastructure/external/`) rather than `infrastructure/external/invenio/`. This is deliberate: encryption is not provider-specific. Future providers use the same encryptor.

**Responsibility:** Symmetric AES-256-GCM encryption/decryption of user tokens using the dedicated master key from `DataManagerVault`.

**Contract:**
```java
package life.qbic.projectmanagement.infrastructure.external;

/**
 * Encrypts and decrypts external provider user tokens using AES-256-GCM.
 *
 * <p>Provider-agnostic: the same encryptor handles tokens for InvenioRDM,
 * LIMS, or any future external source. All share the same master key
 * from the PKCS12 vault (ADR-0002 S2).</p>
 *
 * <p>Each encryption generates a fresh random 12-byte nonce. The nonce is
 * prepended to the ciphertext so decryption can extract it.</p>
 *
 * <p>Output format: {@code nonce (12 bytes) || ciphertext (n bytes) || tag (16 bytes)}</p>
 *
 * @since 1.12.0
 */
public interface ExternalCredentialEncryptor {

    /**
     * Encrypts a plaintext token.
     *
     * @param plaintext the plaintext token as char[]
     * @return the encrypted blob (nonce + ciphertext + tag)
     */
    byte[] encrypt(char[] plaintext);

    /**
     * Decrypts an encrypted token.
     *
     * @param encrypted the encrypted blob (nonce + ciphertext + tag)
     * @return the plaintext token as char[] — caller MUST zero after use
     *         (Arrays.fill(result, '\0') in a finally block)
     */
    char[] decrypt(byte[] encrypted);
}
```

**Implementation class:** `AesGcmCredentialEncryptor`

**Key constants:**
- Algorithm: `AES/GCM/NoPadding`
- Key size: 256 bits
- Nonce size: 12 bytes (96 bits — GCM standard, RFC 5116)
- Tag size: 128 bits (16 bytes — GCM default)
- Vault entry alias: `external-credential-master-key` (configurable)

**Security review checklist for this class:**
- [ ] Fresh nonce per encryption (never reuse)
- [ ] Nonce included in output (prepended)
- [ ] Master key loaded once at bean creation, held as `SecretKey`, never logged
- [ ] `decrypt()` returns `char[]` (not `String`) to enable zeroing
- [ ] No token value in `toString()`, `hashCode()`, logs, or exceptions

---

### Task 3: Domain entity + Repository interface

**Files:**
- `project-management/src/main/java/life/qbic/projectmanagement/domain/model/associated_dataset/UserExternalCredential.java`
- `project-management/src/main/java/life/qbic/projectmanagement/domain/model/associated_dataset/CredentialStatus.java`
- `project-management/src/main/java/life/qbic/projectmanagement/domain/model/associated_dataset/repository/UserExternalCredentialRepository.java`

**Domain entity:**
```java
package life.qbic.projectmanagement.domain.model.associated_dataset;

import java.time.Instant;

/**
 * Per-user, per-instance credential for an external data provider.
 *
 * <p>This entity owns the encrypted token blob and the credential
 * status. The plaintext token never exists in this layer — it exists
 * only transiently in the infrastructure adapter during HTTP calls
 * (ADR-0002 D1 decryption boundary).</p>
 *
 * <p>The entity is source-agnostic at the domain boundary: it carries
 * a {@link SourceType} and an {@code instanceId}, consistent with the
 * {@link AssociatedDataset} aggregate.</p>
 *
 * @since 1.12.0
 */
public class UserExternalCredential {

    private final String id;
    private final String userId;
    private final SourceType sourceType;
    private final String instanceId;
    private final byte[] encryptedToken;   // AES-GCM blob, opaque at this layer
    private CredentialStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    // constructor, getters, status transition method
}
```

**Enum:**
```java
public enum CredentialStatus {
    VALID,
    INVALIDATED
}
```

**Repository interface (domain-layer port):**
```java
package life.qbic.projectmanagement.domain.model.associated_dataset.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;
import life.qbic.projectmanagement.domain.model.associated_dataset.UserExternalCredential;

/**
 * Persistence port for per-user external provider credentials.
 *
 * <p>Implemented by the infrastructure layer (JPA). The application layer
 * depends only on this interface.</p>
 *
 * <p>Queries are scoped by source type so that the application service
 * can look up credentials without knowing the storage details.</p>
 *
 * @since 1.12.0
 */
public interface UserExternalCredentialRepository {

    Optional<UserExternalCredential> findByUserIdAndSourceTypeAndInstanceId(
        String userId, SourceType sourceType, String instanceId);

    List<UserExternalCredential> findByUserId(String userId);

    List<UserExternalCredential> findByUserIdAndSourceType(
        String userId, SourceType sourceType);

    void save(UserExternalCredential credential);

    void deleteByUserIdAndSourceTypeAndInstanceId(
        String userId, SourceType sourceType, String instanceId);
}
```

---

### Task 4: JPA entity + Repository implementation

**Files:**
- `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/dataset/associated/UserExternalCredentialEntity.java`
- `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/dataset/associated/UserExternalCredentialJpaRepository.java` (Spring Data JPA)
- `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/dataset/associated/UserExternalCredentialRepositoryImpl.java`

**JPA entity — key design decisions:**
- `@Entity` mapped to `user_external_credential`
- `sourceType` as `@Enumerated(EnumType.STRING)`
- `encryptedToken` as `byte[]` with `@Column(columnDefinition = "varbinary(512)")`
- `status` as `@Enumerated(EnumType.STRING)`
- `id` generated as UUID (application-generated)
- Unique constraint on `(userId, sourceType, instanceId)`

**Domain model mapping:**
- Bidirectional mapping between `UserExternalCredentialEntity` (JPA) ↔ `UserExternalCredential` (domain)
- Domain entity never holds the plaintext token — only the encrypted blob

---

### Task 5: Token validation — composite dispatcher + per-provider adapters

This task introduces the token validation infrastructure. The design uses a **composite dispatcher** pattern so that adding a new provider type (e.g. LIMS with PAT authentication) requires zero changes to the application-layer port, application service, or UI.

#### 5a. Application-layer port — `ExternalCredentialValidator`

**File:** `project-management/src/main/java/life/qbic/projectmanagement/application/associated_dataset/ExternalCredentialValidator.java`

```java
package life.qbic.projectmanagement.application.associated_dataset;

import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;

/**
 * Validates a user's personal access token against an external data
 * source instance.
 *
 * <p>Separate from {@link DatasetSource} (ADR-0002 P2): token validation
 * is a credential management concern, not a dataset search/resolve
 * concern.</p>
 *
 * <p>Source-type dispatching: the implementation routes to the correct
 * provider-specific validator based on the {@link SourceType}. The
 * application service does not need to know which validator handles a
 * given provider — it passes the source type and the dispatcher handles
 * the rest.</p>
 *
 * @since 1.12.0
 */
public interface ExternalCredentialValidator {

    /**
     * Validates whether the given plaintext token is accepted by the
     * instance's authenticated-user endpoint.
     *
     * @param sourceType the external source type — routes to the
     *                   appropriate provider-specific validator
     * @param config     the target instance configuration
     * @param token      the plaintext token as char[] — implementation
     *                   MUST zero after use
     * @return true if the token is valid (server returned 200), false if
     *         the server rejected the token (401/403)
     * @throws CredentialValidationException if the source type has no
     *         registered validator, or on transient failures (network
     *         error, server error after retries)
     */
    boolean validateToken(SourceType sourceType, InstanceConfig config,
        char[] token);
}
```

**Why a separate port?** ADR-0002 P2 defines `DatasetSource` as the port for dataset search and resolve. Token validation is a credential lifecycle operation (add/verify), not a dataset operation. Keeping them separate:
- Maintains cohesion in `DatasetSource` (search + resolve of datasets)
- Prevents the `actingUserId` parameter contract from being overloaded with a "validate my own credential" use case
- Allows independent evolution of credential management vs. dataset discovery

#### 5b. Per-provider adapter interface — `CredentialValidatorAdapter`

**File:** `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/external/CredentialValidatorAdapter.java`

```java
package life.qbic.projectmanagement.infrastructure.external;

import life.qbic.projectmanagement.application.associated_dataset.InstanceConfig;

/**
 * Provider-specific credential validation adapter.
 *
 * <p>Each external source type implements this interface with its own
 * authentication scheme. The composite dispatcher routes to the correct
 * adapter based on source type.</p>
 *
 * @since 1.12.0
 */
public interface CredentialValidatorAdapter {

    /**
     * Validates the given plaintext token against the instance.
     *
     * @param config the target instance configuration
     * @param token  the plaintext token as char[] — implementation MUST
     *               zero after use
     * @return true if valid, false if rejected
     */
    boolean validate(InstanceConfig config, char[] token);
}
```

#### 5c. InvenioRDM adapter — `InvenioRdmCredentialValidatorAdapter`

**File:** `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/external/invenio/InvenioRdmCredentialValidatorAdapter.java`

```java
package life.qbic.projectmanagement.infrastructure.external.invenio;

import life.qbic.projectmanagement.application.associated_dataset.InstanceConfig;
import life.qbic.projectmanagement.infrastructure.external.CredentialValidatorAdapter;

/**
 * Validates InvenioRDM personal access tokens via the spec-defined
 * authenticated-user endpoint.
 *
 * <p>Endpoint: {@code GET /api/users} (operationId: {@code getAUserById})
 * with {@code Authorization: Bearer <token>}.</p>
 *
 * @since 1.12.0
 */
public class InvenioRdmCredentialValidatorAdapter implements CredentialValidatorAdapter {

    private final InvenioRdmClient client;

    public InvenioRdmCredentialValidatorAdapter(InvenioRdmClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public boolean validate(InstanceConfig config, char[] token) {
        char[] tokenCopy = Arrays.copyOf(token, token.length);
        try {
            String authHeader = "Bearer " + new String(tokenCopy);
            client.getAuthenticatedUser(config.baseUrl(), authHeader);  // 200 → valid
            return true;
        } catch (InvenioRdmClient.InvenioRdmPermanentException e) {
            if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
                return false;   // token invalid
            }
            throw e;            // unexpected 4xx
        } catch (InvenioRdmClient.InvenioRdmTransientException e) {
            throw new CredentialValidationException(
                "Token validation failed due to transient error", e);
        } finally {
            Arrays.fill(tokenCopy, '\0');
        }
    }
}
```

#### 5d. Composite dispatcher — `SourceTypeDispatchingCredentialValidator`

**File:** `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/external/SourceTypeDispatchingCredentialValidator.java`

```java
package life.qbic.projectmanagement.infrastructure.external;

import java.util.Map;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialValidator;
import life.qbic.projectmanagement.application.associated_dataset.InstanceConfig;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;

/**
 * Composite dispatcher that routes credential validation to the correct
 * provider-specific adapter based on source type.
 *
 * <p>New providers are added by implementing {@link CredentialValidatorAdapter}
 * and registering the new adapter in the constructor (or via Spring wiring
 * in {@code InvenioRdmConfiguration}). No changes to the application
 * service, port, UI, or database are required.</p>
 *
 * @since 1.12.0
 */
public class SourceTypeDispatchingCredentialValidator
    implements ExternalCredentialValidator {

    private final Map<SourceType, CredentialValidatorAdapter> adapters;

    public SourceTypeDispatchingCredentialValidator(
            Map<SourceType, CredentialValidatorAdapter> adapters) {
        if (adapters.isEmpty()) {
            throw new IllegalArgumentException(
                "At least one credential validator adapter must be registered");
        }
        this.adapters = Map.copyOf(adapters);
    }

    @Override
    public boolean validateToken(SourceType sourceType, InstanceConfig config,
        char[] token) {
        CredentialValidatorAdapter adapter = adapters.get(sourceType);
        if (adapter == null) {
            throw new CredentialValidationException(
                "No credential validator registered for source type: "
                + sourceType);
        }
        return adapter.validate(config, token);
    }
}
```

**Registration today:** One entry — `SourceType.INVENIO_RDM → InvenioRdmCredentialValidatorAdapter`.

**Adding a future provider (e.g. LIMS):**
1. Implement `LimsCredentialValidatorAdapter implements CredentialValidatorAdapter`
2. Register it in the Spring config: `adapters.put(SourceType.LIMS, new LimsCredentialValidatorAdapter(...))`
3. Add the LIMS instance to `application.properties`
4. Done. The application service, port, UI, and DB table all remain unchanged.

#### 5e. InvenioRdmClient extension — `getAuthenticatedUser()`

**File to modify:** `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/external/invenio/InvenioRdmClient.java`

**API contract (source of truth):**
Per the official [InvenioRDM OpenAPI specification](https://inveniosoftware.github.io/invenio-openapi/):

| Field | Value |
|---|---|
| Path | `GET /api/users` |
| Operation ID | `getAUserById` |
| Parameters | none |
| Security | `BearerAuth` (i.e. `Authorization: Bearer <token>`) |
| Success (200) | `application/json` → `type: object` (generic JSON object; spec does not constrain the schema) |
| Unauthorized (401) | Authentication required — token is missing, invalid, or expired |
| Forbidden (403) | Insufficient permissions |

The response body is typed simply as `type: object` in the spec (no named fields), so the implementation must **not rely on specific fields** beyond the HTTP status code. The 200 status alone is sufficient to prove the token is valid; we capture a best-effort display name for informational/logging purposes only, using `@JsonIgnoreProperties(ignoreUnknown = true)` for forward compatibility against schema evolution.

```java
/**
 * Retrieves the authenticated user's identity from the InvenioRDM instance.
 *
 * <p>This is the token validation endpoint defined in the InvenioRDM
 * OpenAPI spec (operationId: {@code getAUserById}).</p>
 *
 * <ul>
 *   <li>{@code 200} — token is valid; response contains the user's
 *       identity as a JSON object.</li>
 *   <li>{@code 401} — token is missing, invalid, or expired.</li>
 * </ul>
 *
 * <p>The response body is typed {@code type: object} in the official spec
 * (no named fields guaranteed). The DTO below captures best-effort
 * display values for informational/log purposes only — validation
 * succeeds based purely on the 200 status.</p>
 *
 * @param instanceUrl the base URL of the InvenioRDM instance
 * @param authHeader  the full Authorization header value
 *                    (e.g. {@code Bearer <token>})
 * @return authenticated user response
 * @throws InvenioRdmPermanentException on 4xx (401 = invalid token)
 * @throws InvenioRdmTransientException on 5xx or network errors after retries
 */
AuthenticatedUserResponse getAuthenticatedUser(String instanceUrl, String authHeader)
    throws InvenioRdmPermanentException, InvenioRdmTransientException,
           InvenioRdmResponseParsingException;

@JsonIgnoreProperties(ignoreUnknown = true)
record AuthenticatedUserResponse(
    @JsonProperty("id") String id,
    @JsonProperty("username") String username,
    @JsonProperty("email") String email
) {}
```

---

### Task 6: Application service — `ExternalCredentialService`

**File:** `project-management/src/main/java/life/qbic/projectmanagement/application/associated_dataset/ExternalCredentialService.java`

**Responsibility:** Orchestrates the user token lifecycle — add, list, remove. The service is **provider-agnostic**: it works with any source type that has a registered `CredentialValidatorAdapter`.

**Contract:**
```java
package life.qbic.projectmanagement.application.associated_dataset;

import java.util.List;

/**
 * Application service for managing user-level external provider
 * credentials.
 *
 * <p>Orchestrates token validation, encryption (delegated to
 * infrastructure), and persistence. The service is source-agnostic —
 * it operates on any configured instance regardless of source type.</p>
 *
 * @since 1.12.0
 */
public interface ExternalCredentialService {

    /**
     * Adds (or replaces) a token for the given user and instance.
     *
     * <p>Flow:
     * <ol>
     *   <li>Resolve the instance from the registry to obtain the source type</li>
     *   <li>Validate the token against the instance via
     *       {@link ExternalCredentialValidator} (dispatcher routes to the
     *       correct provider adapter)</li>
     *   <li>If valid, encrypt and persist the token</li>
     *   <li>If invalid, return failure — no persistence</li>
     * </ol>
     *
     * @param userId     the DM user adding the token
     * @param instanceId the target instance (e.g. "zenodo")
     * @param token      the plaintext token — zeroed by this method after use
     * @return result indicating success or failure with reason
     */
    AddCredentialResult addCredential(String userId, String instanceId,
        char[] token);

    /**
     * Removes the token for the given user and instance.
     *
     * @param userId     the DM user removing the token
     * @param instanceId the target instance
     * @return true if a credential was removed, false if none existed
     */
    boolean removeCredential(String userId, String instanceId);

    /**
     * Lists the user's credential status for all configured instances
     * across all source types.
     *
     * <p>Returns one entry per configured instance (from
     * {@link SourceInstanceRegistry}). Instances where the user has no
     * token configured are included with a status indicating "not
     * configured".</p>
     *
     * @param userId the DM user whose credentials to list
     * @return credential status per instance; never null
     */
    List<CredentialStatusView> listCredentialStatuses(String userId);

    /**
     * Result of an add-credential operation.
     */
    sealed interface AddCredentialResult
        permits AddCredentialResult.Success, AddCredentialResult.InvalidToken,
                AddCredentialResult.ServiceError,
                AddCredentialResult.UnknownInstance {}

    record Success() implements AddCredentialResult {}
    record InvalidToken(String reason) implements AddCredentialResult {}
    record ServiceError(String reason) implements AddCredentialResult {}
    record UnknownInstance(String instanceId) implements AddCredentialResult {}

    /**
     * Credential status view for a single instance (no plaintext token).
     */
    record CredentialStatusView(
        SourceType sourceType,
        String instanceId,
        String instanceDisplayName,
        boolean configured,
        String status   // "VALID" | "INVALIDATED" | "NOT_CONFIGURED"
    ) {}
}
```

**Key change from the original plan:** The `addCredential` method no longer needs the caller to specify the `SourceType`. It resolves the source type from the `SourceInstanceRegistry` via the `instanceId`. This keeps the view-layer call simple: "add this token for instance X."

---

### Task 7: Wire token resolution into `InvenioRdmDatasetSource`

**Files to modify:**
- `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/external/invenio/InvenioRdmDatasetSource.java`

**Current state:** The `InvenioRdmClient` already threads `authHeader` through `getWithRetry()` but it is always passed as `null`. The `InvenioRdmDatasetSource.search()` receives `actingUserId` but ignores it.

**Changes:**
1. `InvenioRdmDatasetSource` receives `UserExternalCredentialRepository` and `ExternalCredentialEncryptor` via constructor injection.
2. In `search()` and `resolveMetadata()`:
   - Look up `repository.findByUserIdAndSourceTypeAndInstanceId(actingUserId, SourceType.INVENIO_RDM, config.id())`
   - If found: decrypt token, build `Authorization: Bearer {token}` header, pass to client
   - If not found: pass `null` (public records only)
   - Zero token `char[]` in `finally` block

**Decryption boundary enforcement (ADR-0002 D1):**
```java
@Override
public SearchResult search(SearchQuery query, InstanceConfig config,
    String actingUserId) throws DatasetSearchException {
    char[] token = resolveTokenForUser(actingUserId, config.id());
    try {
        String authHeader = token != null ? "Bearer " + new String(token) : null;
        // ... perform search with authHeader ...
    } finally {
        if (token != null) {
            Arrays.fill(token, '\0');
        }
    }
}

private char[] resolveTokenForUser(String userId, String instanceId) {
    return credentialRepository
        .findByUserIdAndSourceTypeAndInstanceId(
            userId, SourceType.INVENIO_RDM, instanceId)
        .map(cred -> encryptor.decrypt(cred.encryptedToken()))
        .orElse(null);
}
```

**Note:** This task creates a dependency from `InvenioRdmDatasetSource` → `UserExternalCredentialRepository`. Since the repository is a domain port (domain-layer interface) and `InvenioRdmDatasetSource` is in the infrastructure layer, this is an acceptable infrastructure-to-domain dependency (infrastructure depends on domain, not the reverse).

---

### Task 8: UI — "External Providers" view

**New route:** `/account/external-providers` (alongside `/profile` and `/account`)

**Files:**
- `datamanager-app/src/main/java/life/qbic/datamanager/views/account/ExternalProvidersMain.java` — Vaadin `@Route` class
- `datamanager-app/src/main/java/life/qbic/datamanager/views/account/ExternalProvidersComponent.java` — UI logic
- `datamanager-app/src/main/java/life/qbic/datamanager/views/account/AddExternalCredentialTokenDialog.java` — Token input dialog
- CSS: `datamanager-app/frontend/themes/datamanager/components/external-providers.css`

**UI layout:**

```
┌─────────────────────────────────────────────────────────────────────┐
│ External Providers                                                   │
│                                                                      │
│ Connect your personal access tokens to enable access to              │
│ access-restricted datasets on external instances.                    │
│                                                                      │
│ InvenioRDM Instances                                                 │
│ ┌────────────────────────────────────────────┐  ┌───────────────┐  │
│ │ 🟢 Zenodo (zenodo.org)                     │  │ Connected     │  │
│ │    Token configured · Status: VALID        │  │ [Remove]      │  │
│ └────────────────────────────────────────────┘  └───────────────┘  │
│                                                                      │
│ ┌────────────────────────────────────────────┐  ┌───────────────┐  │
│ │ 🔴 FDAT (fdat.uni-tuebingen.de)            │  │ Not connected │  │
│ │    No token configured                      │  │ [Add Token]   │  │
│ └────────────────────────────────────────────┘  └───────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**Add Token Dialog:**
```
┌─────────────────────────────────────────────────────────────────────┐
│ Add Token — Zenodo (zenodo.org)                                      │
│                                                                      │
│ Paste your personal access token from your Zenodo account:           │
│                                                                      │
│ ┌────────────────────────────────────────────────────────────────┐  │
│ │ Personal Access Token                                          │  │
│ └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│ ⓘ Your token is stored encrypted and used only to access your      │
│   own restricted datasets. You can create one at:                   │
│   https://zenodo.org/account/settings/applications/                 │
│                                                                      │
│                              [Cancel]    [Validate & Save]           │
└─────────────────────────────────────────────────────────────────────┘
```

**Key UX decisions:**
1. **Benefit text (AC-6):** Shown as a descriptive paragraph at the top of the view — explains that providing a token enables connection of access-restricted datasets to a project.
2. **Instance list:** The view iterates all configured instances by source type (currently only `INVENIO_RDM`). Grouped under section headings. Not hardcoded, not user-configurable (ADR-0002 I2).
3. **Status indicators:** Green checkmark + "Connected" for configured/VALID instances; red X + "Not connected" for unconfigured or INVALIDATED instances.
4. **Token input:** `PasswordField` component — no echo of characters.
5. **Validation feedback:** Toast notification — "Token validated successfully. Zenodo is now connected." or "Token validation failed. Please check your token and try again."
6. **External link:** Help text links to the instance's token creation page. The URL pattern `{baseUrl}/account/settings/applications/` is standard for InvenioRDM. For non-InvenioRDM providers, the `SourceInstanceDescriptor` could carry an optional `tokenCreationUrl` field in the future.
7. **Security:** No token value ever displayed after saving. No copy button. Field is cleared on cancel.

**Navigation integration:** The "External Providers" route should be accessible from the existing account/profile area navigation. This may require adding a link to the account navigation sidebar or tabs.

---

### Task 9: Vault master key + configuration

**New application property:**
```properties
# Dedicated master key for encrypting user external provider credentials (ADR-0002 S2)
# This is the keystore entry alias. The actual key bytes come from the PKCS12 vault.
# The same key is used for all source types (InvenioRDM, future LIMS, etc.).
qbic.security.vault.external-credential.key-alias=external-credential-master-key
```

**Vault setup:** At deployment, a new PKCS12 keystore entry is created with alias `external-credential-master-key` containing an AES-256 secret key. This is separate from the existing OpenBIS vault entries.

**Bean wiring:** `ExternalCredentialEncryptor` reads this alias from the configuration, loads the corresponding `SecretKey` from the `DataManagerVault`, and uses it for all AES-GCM operations.

**Security consideration:** The key alias is configurable so that environments can use different keys (e.g., for key rotation in the future). The vault entry password is the same existing `DATAMANAGER_VAULT_ENTRY_PASSWORD` env var — the alias is the distinguishing factor.

---

### Task 10: Spring wiring in `InvenioRdmConfiguration`

**File:** `datamanager-app/src/main/java/life/qbic/datamanager/configuration/InvenioRdmConfiguration.java`

**New beans to register:**
```java
// ── Encryption (provider-agnostic) ─────────────────────────────────
@Bean
public ExternalCredentialEncryptor externalCredentialEncryptor(
    DataManagerVault vault,
    @Value("${qbic.security.vault.external-credential.key-alias}") String keyAlias) {
    return new AesGcmCredentialEncryptor(vault, keyAlias);
}

// ── Per-provider credential validator adapters ──────────────────────
@Bean
public CredentialValidatorAdapter invenioRdmCredentialValidatorAdapter(
    InvenioRdmClient client) {
    return new InvenioRdmCredentialValidatorAdapter(client);
}

// ── Composite dispatcher ────────────────────────────────────────────
@Bean
public ExternalCredentialValidator externalCredentialValidator(
    CredentialValidatorAdapter invenioRdmCredentialValidatorAdapter) {
    return new SourceTypeDispatchingCredentialValidator(Map.of(
        SourceType.INVENIO_RDM, invenioRdmCredentialValidatorAdapter
        // Future: SourceType.LIMS, limsCredentialValidatorAdapter
    ));
}

// ── Application service (provider-agnostic) ─────────────────────────
@Bean
public ExternalCredentialService externalCredentialService(
    ExternalCredentialValidator validator,
    UserExternalCredentialRepository credentialRepository,
    ExternalCredentialEncryptor encryptor,
    SourceInstanceRegistry registry) {
    return new DefaultExternalCredentialService(
        validator, credentialRepository, encryptor, registry);
}

// ── Update DatasetSource bean ───────────────────────────────────────
@Bean
public DatasetSource invenioRdmDatasetSource(
    InvenioRdmClient client,
    UserExternalCredentialRepository credentialRepository,
    ExternalCredentialEncryptor encryptor) {
    return new InvenioRdmDatasetSource(client, credentialRepository, encryptor);
}
```

---

### Task 11: Tests

| Test class | Type | Framework | Module |
|---|---|---|---|
| `AesGcmCredentialEncryptorSpec` | Unit | Spock | `project-management-infrastructure` |
| `InvenioRdmCredentialValidatorAdapterSpec` | Unit | Spock | `project-management-infrastructure` |
| `SourceTypeDispatchingCredentialValidatorSpec` | Unit | Spock | `project-management-infrastructure` |
| `DefaultExternalCredentialServiceSpec` | Unit | Spock | `project-management` |
| `InvenioRdmDatasetSourceSpec` (update existing) | Unit | Spock | `project-management-infrastructure` |
| `InvenioRdmClientSpec.getAuthenticatedUser` (update existing) | Unit | Spock | `project-management-infrastructure` |

**Key test scenarios:**

**`AesGcmCredentialEncryptorSpec`:**
- Encryption → decryption roundtrip preserves plaintext
- Different nonces produce different ciphertexts for same plaintext
- Decryption with wrong key fails
- Decrypting corrupted data throws meaningful exception

**`InvenioRdmCredentialValidatorAdapterSpec`:**
- 200 response → `validate()` returns `true`
- 401 response → `validate()` returns `false`
- 403 response → `validate()` returns `false`
- 500 response (transient) → exception (not swallowed as "invalid")
- Token `char[]` is zeroed after the validate call completes

**`SourceTypeDispatchingCredentialValidatorSpec`:**
- Correct adapter is invoked for a known `SourceType`
- `CredentialValidationException` thrown for unknown `SourceType` (no registered adapter)
- Token is forwarded to the correct adapter (not lost or duplicated)

**`DefaultExternalCredentialServiceSpec`:**
- `addCredential` with valid token → resolves source type from registry → persists encrypted blob
- `addCredential` with invalid token → does NOT persist, returns `InvalidToken`
- `addCredential` with unknown instance → returns `UnknownInstance`
- `addCredential` when instance already has a token → replaces with new token
- `removeCredential` with existing credential → deletes it
- `removeCredential` with no credential → returns false
- `listCredentialStatuses` returns all configured instances (configured + unconfigured) with correct source type
- Token is zeroed after `addCredential` completes

**`InvenioRdmDatasetSourceSpec` (update):**
- `search()` with user who has token → includes auth header
- `search()` with user who has no token → no auth header (public only)
- Token `char[]` is zeroed after search completes
- `resolveMetadata()` follows same token resolution pattern

---

## 5. Module Dependency Graph

```
datamanager-app (composition root)
├── InvenioRdmConfiguration wires:
│   ├── ExternalCredentialEncryptor (infrastructure interface)
│   │   └── AesGcmCredentialEncryptor ← DataManagerVault (PKCS12)
│   │
│   ├── ExternalCredentialValidator (port: application)
│   │   └── SourceTypeDispatchingCredentialValidator (composite, infra)
│   │       ├── INVENIO_RDM → InvenioRdmCredentialValidatorAdapter
│   │       │                   └── InvenioRdmClient.getAuthenticatedUser()
│   │       └── (future) LISMS → LimsCredentialValidatorAdapter
│   │
│   ├── ExternalCredentialService (application)
│   │   ├── ExternalCredentialValidator (dispatcher)
│   │   ├── SourceInstanceRegistry
│   │   ├── UserExternalCredentialRepository (port: domain)
│   │   └── ExternalCredentialEncryptor
│   │
│   ├── DatasetSource (port: application)
│   │   └── InvenioRdmDatasetSource (adapter: infrastructure)
│   │       ├── InvenioRdmClient
│   │       ├── UserExternalCredentialRepository (for token resolution)
│   │       └── ExternalCredentialEncryptor (for token decryption)
│   │
│   └── CredentialValidatorAdapter beans (per provider)
│
└── ExternalProvidersMain (@Route: /account/external-providers)
    └── ExternalCredentialService
```

---

## 6. Implementation Order (recommended)

This order minimises blocked work and allows incremental validation:

| Step | Tasks | Rationale |
|---|---|---|
| **Phase 1: Foundation** | Task 1 (DB schema), Task 9 (vault key config) | No code dependencies. Can be done first to unblock everything. |
| **Phase 2: Encryption** | Task 2 (`ExternalCredentialEncryptor`) | Core security primitive. Must be right before anything else touches tokens. |
| **Phase 3: Domain** | Task 3 (domain entity + repo interface) | Defines the domain contract. Infrastructure implements it in Phase 4. |
| **Phase 4: Infrastructure persistence** | Task 4 (JPA entity + Spring Data repo) | Implements the domain repository. Depends on Task 1 (table exists) and Task 2 (encryptor exists). |
| **Phase 5: Validation layer** | Task 5 (InvenioRdmClient.getAuthenticatedUser + per-provider adapter + composite dispatcher) | The validation primitive needed by the application service in Task 6. |
| **Phase 6: Application service** | Task 6 (`ExternalCredentialService`) | Orchestrates validation + encryption + persistence. Depends on Tasks 2–5. |
| **Phase 7: Wire token into DatasetSource** | Task 7 | Modifies the existing adapter to use stored tokens. Depends on Tasks 3, 4. |
| **Phase 8: UI** | Task 8 | Frontend work. Can be prototyped earlier with mock data, but final integration requires Task 6. |
| **Phase 9: Wiring** | Task 10 (Spring config) | Ties all beans together. Done last or iteratively alongside Tasks 6–7. |
| **Phase 10: Tests** | Task 11 | Written alongside each phase, but final integration tests last. |

**Parallelism opportunities:**
- Tasks 5 (validation) and 7 (token resolution in DatasetSource) are independent and can be done in parallel.
- Task 8 (UI) can be prototyped in parallel with Tasks 2–6 using hardcoded/mock statuses.

---

## 7. Risk Register

| Risk | Impact | Mitigation |
|---|---|---|
| `GET /api/users` endpoint contract differs across InvenioRDM versions | Token validation fails with unexpected response shape | The official spec defines this as a stable, parameter-less `BearerAuth` endpoint returning `type: object`. Implementation must not depend on specific response fields (spec leaves the body open-ended). Validation is based solely on the 200 status. The `@JsonIgnoreProperties(ignoreUnknown = true)` DTO handles forward-compatible field extraction. If a future instance removes the endpoint, a fallback to any authenticated call (e.g. `GET /api/records?size=1` with the auth header) can be introduced as an adapter-level change without touching the port. |
| PKCS12 vault key distribution to HA nodes | Encryption/decryption fails on some nodes | Same deployment pattern as existing OpenBIS vault keys — already proven. Document in ops runbook. |
| Token expiry / revocation on InvenioRDM | Stored token becomes invalid unexpectedly | AC-4 covers this for re-validation. For sync (FEAT-DATSET-04/08), the invoking user is informed they need to re-add their token (ADR-0003 C1). |
| InvenioRDM rate limits on `GET /api/users` during token add | Validation fails transiently | `InvenioRdmClient` already has retry logic for 429/5xx. Token validation will benefit from this. |
| Plaintext token leak in logs / error messages | Security incident | Decryption boundary (ADR-0002 D1), audit checklist in Task 2, code review emphasis. |

---

## 8. Out of Scope (explicitly deferred)

| Item | Reason | Linked story |
|---|---|---|
| Remove credential from UI | Separate story with its own ACs | FEAT-DATSET-15 (#1479) |
| Background sync / auto-refresh | Deferred to future (ADR-0003 Y1) | — |
| Service-account / institutional tokens | Not in v1 scope (ADR-0002 T1) | — |
| Token rotation reminders / expiry warnings | No user story drives this yet | — |
| Cross-project credential sharing | Out of scope per feature boundaries | — |

---

## 9. Design Decisions Summary

| Decision | Choice | Rationale |
|---|---|---|
| DB table name | `user_external_credential` (not `user_invenio_rdm_credential`) | Schema is provider-agnostic. The `source_type` + `instance_id` columns handle any provider. Adding a second provider requires zero schema changes. |
| Token validation dispatch | Composite dispatcher (`SourceTypeDispatchingCredentialValidator`) | Application service is provider-agnostic. Adding a new provider = implement adapter + register bean. No changes to port, service, UI, or DB. |
| Validation port shape | `ExternalCredentialValidator.validateToken(SourceType, InstanceConfig, char[])` | Source type parameter enables dispatch. Application service resolves source type from registry via `instanceId` — the caller doesn't need to know. |
| Encryptor scope | Provider-agnostic (`ExternalCredentialEncryptor`) | Encryption algorithm is the same regardless of provider. One master key, one encryptor, one set of security tests. |
| Per-provider adapter | `CredentialValidatorAdapter` interface in infrastructure | Each provider's auth scheme (endpoint, header format, response shape) is encapsulated in its own adapter class. Dispatcher routes to the right one by `SourceType`. |
