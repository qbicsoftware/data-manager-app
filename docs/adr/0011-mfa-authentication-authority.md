# 0011 — MFA authentication authority: the application owns the second factor

* Status: accepted (Product Owner sign-off 2026-11-17; see *Sign-off record*)
* Deciders: Product Owner (signed); tech lead and the owner of [`SECURITY.md`](../../SECURITY.md) — pending
* Date: 2026-11-17

Technical Story: [#1667](https://github.com/qbicsoftware/data-manager-app/issues/1667) (MILESTONE-MFA M0-3) — milestone plan
[`docs/plans/MILESTONE-MFA-self-governed-2fa.md`](../plans/MILESTONE-MFA-self-governed-2fa.md); input note
[`ADR-0011-input-orcid-role.md`](../plans/ADR-0011-input-orcid-role.md)

## Context and Problem Statement

[AUTH-R-01](../requirements.md#auth-r-01-mandatory-second-authentication-factor) requires a second
factor for **every** interactive login, enforced **by the system itself**, with **no exempt account**.
That requirement is stated as a property of the product, so the first thing it has to settle is
*who decides whether a second factor was presented*. The options are the application, an external
identity provider, or a mixture — and the answer determines the schema, the support process, the
privacy position, and whether the requirement is even statable for every account.

The question was deferred until two preconditions were true, both of which now hold:

1. **There is exactly one authentication path.** [AUTH-R-07](../requirements.md#auth-r-07-verifiable-ownership-of-the-authentication-address)
   shipped (2026-11-17): ORCID is an account attribute, not a login. `SecurityConfiguration` contains
   `oauth2Client()`, not `oauth2Login()`; the only principal type is `QbicUserDetails`; and
   `OidcLinkController` no longer parks an `Authentication` in the session. A second factor placed on
   *the* login path now covers all logins, which it did not before.
2. **The flows that could bypass a factor were hardened first** (M1: tokenised single-use links,
   server-side throttling, uniform failures). An attacker who can mint a session through a
   confirmation or reset link never meets a factor, so this ordering was not cosmetic.

Five sub-decisions belong to the same record because they are interdependent (§*Considered Options*):
the authority itself, the factor store and its encryption, the enforcement switch, the break-glass
posture, and whether phishing-resistant credentials are required or optional.

## Decision Drivers

* **AUTH-R-01** — enforced by the system, no exempt account, no dependence on an external IdP for
  enforcement. A login with only a first factor shall not establish an authenticated session.
* **AUTH-R-02** — self-service enrolment and management, at minimum a TOTP authenticator; a
  public-key credential is desirable but *not* a precondition.
* **AUTH-R-03** — recovery codes + administrative re-enrolment; no account permanently inaccessible.
* **AUTH-R-04** — the factor lifecycle is auditable (enrol, remove, replace, use, recovery, admin).
* **AUTH-NFR-01** — no retrievable plaintext shared secret; TOTP seeds encrypted with a key **not in
  the database**; public-key credentials stored as public keys only; recovery codes not reconstructable.
* **AUTH-NFR-02** — fail **closed** when a decision cannot be reached; ≤ 1000 ms at p95; a challenge
  never outlives its session; a session established before enrolment does not satisfy AUTH-R-01.
* **AUTH-NFR-03** — failure responses indistinguishable in content and timing; throttled per account
  and per origin. This applies to the factor step as well, or it becomes the new oracle.
* **`AUTH-C-01`** — the second factor shall not depend on an external identity provider for enforcement.
* **Operational reality** — HA deployment behind HAProxy, 30-minute sessions
  (`server.servlet.session.timeout`), PKCS12 vault for key material, MariaDB for state. Existing
  precedent for stored secrets is [ADR-0002](0002-invenio-rdm-api-client-credentials.md) S2.
* **Support reality** — a lost factor must be recoverable without developer or database access
  (AUTH-R-03), and a lost *key* must not lock out the whole population.

## Considered Options

### Authentication authority

* **A1** — **The application owns the second factor.** Factor store, policy and verification live in
  the `identity` bounded context; the app issues the challenge and grants the session.
* **A2** — **bwIDM2 / RegApp / FeLS federated step-up** (SCC KIT). RegApp runs an integrated MFA
  service (LinOTP), exposes SAML and OIDC, and can enforce step-up even when the home IdP does not.
* **A3** — **ORCID `amr` claim** as the evidence of a second factor.
* **A4** — **External hosted IdP** (Keycloak / Entra / Auth0) as the authentication authority.

### Factor store and secret protection

* **F1** — **Master AES key in the PKCS12 vault, AES-GCM blobs in MariaDB** — the ADR-0002 S2 pattern,
  under a *new* vault key alias dedicated to factor material.
* **F2** — One-way hash the TOTP seeds with the existing PAT encoder (`PersonalAccessTokenEncoder`,
  PBKDF2, ≥ 100 000 iterations, ≥ 128-bit salt).
* **F3** — Store seeds in plaintext / application-level obfuscation ("column encryption" only).

### Enforcement switch

* **P1** — **`MfaPolicy` as a bean driven by configuration** (`off` / `optional` / `required`), with
  per-account *state* (a dated grace window) in the database.
* **P2** — Policy and exemptions in a database table, editable at runtime without a deployment.
* **P3** — Policy hard-coded per environment in code.

### Break-glass

* **B1** — **No exempt accounts.** Recovery codes verified by an irreversible hash that does **not**
  depend on the vault key; administrative re-enrolment as the last resort.
* **B2** — A small set of permanently exempt accounts (service/admin/break-glass).
* **B3** — Recovery codes stored like TOTP seeds (encrypted with the vault master key).

### Phishing-resistant credentials

* **W1** — **WebAuthn is an offered extension, decided by a timeboxed spike** (M3-2). TOTP plus
  recovery codes is the milestone-complete baseline.
* **W2** — WebAuthn is required for enforcement; TOTP is only a fallback.
* **W3** — WebAuthn out of scope entirely.

## Decision Outcome

Chosen: **A1 + F1 + P1 + B1 + W1**. The application owns the second factor end to end.

1. **Authority (A1).** The application issues the challenge, verifies the factor, and only then
   establishes an authenticated session. No external assertion is ever accepted as evidence of a
   second factor, and no filter chain other than the app's own can produce a full `Authentication`
   from a password alone. This is the only option that satisfies AUTH-R-01 for *every* account:
   A2–A4 all make enforcement conditional on a third party's enrolment state, coverage, availability
   and willingness to be mandatory, which is exactly what AUTH-C-01 forbids and what the population
   (open self-service registration, external collaborators) cannot be assumed to have.
2. **Factor store (F1).** TOTP seeds are AES-GCM-encrypted (`nonce + ciphertext + tag`) with a master
   AES-256 key held **only** in the PKCS12 vault, read at startup under a dedicated alias — the same
   shape as ADR-0002 S2, whose decryption boundary rule applies unchanged: plaintext never leaves the
   infrastructure layer. Recovery codes are **hashed, not encrypted**; WebAuthn credentials store the
   public key, credential id, transports and signature counter, never a secret. All factor rows live in
   the `identity` bounded context.
   **F2 is rejected as a category error, not a preference:** a TOTP seed must be *recovered* to compute
   the expected code, so a one-way hash cannot carry it. The PAT encoder is the right tool for a value
   that only needs verification (and ADR-0002 already records this distinction: PATs are "a
   one-way-hashed credential", Invenio tokens are not). Applying it here would silently destroy the
   factor rather than protect it.
3. **Enforcement switch (P1).** `MfaPolicy` is a bean (`off` / `optional` / `required`) bound to
   configuration, so the posture is a deployment decision that appears in review, in the config diff and
   in the release note. A per-account **grace window is stored state with an expiry**, not an exemption
   flag: it ends by itself, it is visible to the user, and it cannot become a permanent silent bypass.
   P2 is rejected because *who is exempt today* would become runtime data with no review trail — the
   exact failure mode AUTH-R-01 rules out, and the reason the milestone requires a test asserting an
   empty exemption set (M5-3).

   **The flip itself is the dangerous moment, and the grace window is the entire safety net** (PO
   amendment, 2026-11-17). Reconfiguring a *running* service must not deny anyone who has not been
   given a deadline, so the enforcement decision is `required ∧ (enrolled ∨ now < graceUntil)`, with a
   missing `graceUntil` resolved by **granting** a window, never by denying. Four invariants — M5-1 must
   satisfy all four and M4-5 must test them:

   - **Absence of a deadline may only extend access, never remove it.** An absent `graceUntil` on an
     unenrolled account evaluates to `now + grace` and is persisted. The inverse reading is the lockout
     bug: the same missing value would make a `required` flip deny the whole population at once.
   - **The clock starts when the account first becomes subject to `required`,** not at deployment time
     (initial configuration: 48 h per M5-1; a longer window for a cohort is a configuration value, not
     a code change). Someone who logs in three weeks after the flip therefore still gets a full window
     instead of an expired one, and dormant accounts cannot evade enforcement indefinitely — the window
     is consumed by their first login, not by wall-clock time.
   - **Grace is for enrolment only.** The session it grants is real, but the UI is an enrolment
     interstitial that cannot be dismissed; the deadline is not extended by continued use.
   - **Announcement precedes enforcement** (M8-1: deadline announced once, countdown repeated at 4/2/1
     weeks). The grace window absorbs absence and slow enrolment; it is not a substitute for a published
     deadline, and it must not be used as one.

   Sessions already established when the configuration changes are bounded by the 30-minute session
   timeout, so no invalidation sweep is needed and nobody is logged out mid-work (AUTH-NFR-02).
4. **Break-glass (B1).** Recovery codes are verified by an irreversible hash computed **outside** the
   vault-key path, so they still work when the master key is lost or the vault is unavailable. This is
   the deliberate exception to "everything factor-shaped is vault-protected": the vault now protects
   *one* class of secret, and losing it degrades the system to recovery-code and admin re-enrolment
   instead of a total lockout (milestone risk R8). Thus B3 is rejected, and B2 is rejected because an
   exempt login path concentrates attack value on the least-exercised path (AUTH-R-01 rationale).
5. **Phishing-resistant credentials (W1).** TOTP + recovery codes satisfies AUTH-R-02 and defines the
   milestone-complete state. WebAuthn ships only if the M3-2 spike demonstrates it behind the real
   deployment: passkeys require the five prerequisites in the milestone plan §1a, of which condition 1
   (trusted forwarded headers; `server.forward-headers-strategy` is currently **unset**, so the app
   cannot resolve its own public origin) and condition 3 (sticky sessions for the challenge) are
   **unconfirmed today**. Choosing W2 would make the whole mandate depend on infrastructure work that
   is tracked elsewhere and is not in this repository.
6. **Administratively set per-account policy — deferred, recorded so it is not re-derived.** An
   administrator setting MFA explicitly for a cohort ("mandatory for power users, optional for end
   users") is a *tightening* override and is compatible with this record: it can only move an account
   from `optional` to `required`, never the reverse. It therefore does not create the exempt login path
   P1/B1 reject and does not weaken M5-3's empty-exemption test. It is a future slice and explicitly
   **non-blocking** for this milestone, but it must reuse this grace mechanism unchanged — an
   administrative override that takes effect immediately is the same lockout vector item 3 warns about,
   just triggered by a click instead of a deploy. A `NOT_REQUIRED` override is **not** part of that slice
   and is not planned.

### Sign-off record (2026-11-17)

Product Owner approved items 1, 3 and 4 as written, and item 2 with the grace-window amendment. The ADR
is the gate for M2, so no code waits on this record any more:

| # | Decision | Outcome |
|---|---|---|
| 1 | Vault-carried AES master key under a new alias for TOTP seeds (F1) | **Accepted** — dedicated master key in the PKCS12 vault |
| 2 | Policy in configuration, grace window as dated state (P1) | **Accepted with amendment** — the grace window is load-bearing for a mid-run configuration change; see the four invariants in item 3 |
| 3 | Recovery codes hashed outside the vault-key path (B1) | **Accepted** |
| 4 | WebAuthn optional, decided by the M3-2 spike (W1) | **Accepted** — optional, not required |
| — | Administratively set per-account policy | **Deferred**, non-blocking; tightening-only (item 6) |

Still outstanding before merge, neither of which blocks M2: the tech-lead and `SECURITY.md` sign-off named
under *Deciders*, and the pull request into `epic/mfa`.

## Consequences

### Positive

* **The requirement is statable for every account.** No cohort, provider or role sits outside the
  enforcement point, which is what makes the no-exemption test (M5-3) meaningful rather than aspirational.
* **One place to reason about sessions.** A `SECOND_FACTOR_PENDING` state inside the app's own filter
  chain can be made precise — no full `Authentication` before verification — instead of inferring a
  provider's intent from a claim. The saved-request cache (M4-4) and the bypass suite (M4-5) both depend
  on the app owning that state, and the session is already bounded at 30 minutes, so a pending state
  cannot linger past AUTH-NFR-02's "challenge shall not outlive its session".
* **Existing security machinery is reused, not duplicated.** The `auth_attempt` table (M1-5) was designed
  with an open `scope` value and a hashed key precisely so second-factor throttling needs no new table
  and no schema change (M5-4). Factor failures therefore inherit the uniform-response and upsert-counter
  properties already established there.
* **Fail-closed is coherent with recovery.** AUTH-NFR-02 denies login when the factor store is
  unavailable; because recovery codes are verified outside the vault path, that denial is not a total
  outage.
* **The vault keeps one job per key.** A dedicated alias means factor material can be rotated or
  restored without touching external-credential keys, and an incident in one path does not force a
  rotation in the other.

### Negative

* **Availability of login is now ours.** An `identity` or MariaDB outage that previously degraded
  non-auth features now denies login. Accepted, because AUTH-NFR-02 requires failing closed.
* **The vault master key becomes load-bearing for TOTP.** Its loss removes TOTP for everyone at once
  (risk R8). Mitigated only by B1 and by putting key restore into the DR runbook — a documentation
  obligation this ADR creates, not one it discharges.
* **Deploy-time posture changes.** `off → optional → required` needs a release or a config change per
  cohort (P1) rather than a click in an admin UI. Intentional: the change should be reviewed and
  announced, and the rollout plan (M8) already announces each step weeks in advance. The cost is that
  the flip is the one moment a configuration change can lock people out, so it is not safe by
  construction — it depends entirely on the grace window being implemented as specified in item 3.
* **Per-account policy needs an administrator surface that does not exist yet** (deferred, item 6). Until
  that slice lands, making MFA mandatory for one cohort but not another means running separate
  deployments or waiting for the rollout phase — a scheduling limitation, not a security gap.
* **Passkeys are not guaranteed.** If the §1a prerequisites are not met, the milestone ships TOTP only.
  AUTH-R-02 permits this, but the residual is that a relayed one-time code is not phishing-resistant.
* **Recovery codes become the strongest residual attack surface** (risk R9): high-entropy, single-use,
  regenerated as a set, and treated as a full account compromise in the incident process.

## Pros and Cons of the Options

### A1 — Application owns the factor (chosen)

* Good, because it is the only option where AUTH-R-01 holds for every account, including collaborators
  whose home organisation does not enforce MFA.
* Good, because the enforcement point cannot be removed by a provider outage, metadata change or
  unannounced `acr` behaviour change.
* Good, because factor data stays inside the `identity` context, matching AUTH-NFR-01 and the DPIA note.
* Bad, because the application must operate enrolment, recovery, support and retention itself.

### A2 — bwIDM2 / RegApp federated step-up

* Good, because RegApp genuinely enforces step-up MFA even when the user's home IdP does not.
* Bad, because a state-wide IdP outage becomes our login outage, on an SLA we do not hold.
* Bad, because `sub` stability across a home-IdP change is not ours to control.
* Bad, because the `User` aggregate holds a single `oidcIssuer`/`oidcId` pair, so institutional IdP
  *plus* the existing ORCID link needs a 1:N OIDC identity model — a schema change taken on for a
  dependency we do not control.
* Bad, because our population is open self-service registration, not mostly Landes-IdP, so it cannot
  be mandated for everyone.
* Neutral, because it stays the recorded **revisit trigger** for retiring the *password* as first
  factor after the milestone (M9-3), which is a convenience/attack-surface win, not a prerequisite.

### A3 — ORCID `amr` claim

* Good, because it would need no factor store of our own.
* Bad, because `amr` is returned **only via the Member API**, not the Public API this application uses.
* Bad, because ORCID cannot be mandated for all users — and since AUTH-R-07 it is not in the login path
  at all. This is now a non-option rather than a considered one.

### A4 — External hosted IdP as the authority

* Good, because MFA, recovery flows and factor UX arrive pre-built.
* Bad, because it is the same dependency argument as A2 plus a new operational surface to run, and it
  moves the requirement's enforcement outside the product, contrary to AUTH-C-01.

### F1 — Vault master key + AES-GCM blobs (chosen)

* Good, because it is the established pattern (ADR-0002 S2), including its HA property: every node reads
  the same master key from the vault and decrypts with the shared database.
* Good, because the key is not in the database, which is what AUTH-NFR-01 requires.
* Good, because rotation and restore are vault operations with a documented runbook, not a migration.
* Bad, because seed decryption is impossible without a working vault entry — hence B1.

### F2 — One-way hash the seeds like a PAT

* Good, because it reuses an existing encoder and needs no key management.
* Bad, because it is **impossible**: verification of a TOTP requires the seed, so hashing destroys the
  factor. The milestone plan's phrase "same pattern as personal access tokens" is inaccurate on this
  point and this ADR supersedes it (see *Links*).

### F3 — Plaintext or column-obfuscation

* Good, because it is trivial to implement.
* Bad, because a database dump or backup clone then yields cloneable factors for the whole population,
  which AUTH-NFR-01 explicitly forbids.

### P1 — Policy bean + dated grace state (chosen)

* Good, because the posture is reviewable, diffable and releasable per cohort without an admin UI.
* Good, because a grace window expiring by itself cannot become a permanent exemption.
* Bad, because changing posture requires a deployment or config rollout rather than a runtime toggle.

### P2 — Runtime policy table

* Good, because ops can change posture without a release.
* Bad, because the record of *who is exempt and until when* becomes unreviewed runtime data — and the
  milestone's M5-3 test asserts an empty exemption set, which a runtime table undermines by design.

### P3 — Hard-coded per environment

* Good, because it cannot drift.
* Bad, because it makes the pilot (M7) and the staged rollout (M8) impossible without code changes, and
  hides a security posture inside a branch.

### B1 — No exemptions, recovery codes outside the vault path (chosen)

* Good, because it satisfies AUTH-R-01 literally and keeps the recovery route independent of the key
  that protects the other factor (risk R8).
* Good, because recovery codes are high entropy, so an unkeyed PBKDF2 hash is sufficient and survives
  vault-key loss.
* Bad, because support cannot "just let someone in"; every lost-device case follows the documented path.

### B2 — Permanently exempt accounts

* Good, because it guarantees at least one way in.
* Bad, because that path becomes the cheapest entry point and is exercised least — the AUTH-R-01
  rationale for rejecting exemptions applies to accounts we create as much as to accounts users create.

### B3 — Recovery codes encrypted with the vault master key

* Good, because it is uniform with TOTP.
* Bad, because it couples both factors to the same key, so vault-key loss becomes a total lockout, which
  is exactly the failure R8 asks us to avoid.

### W1 — WebAuthn as an offered extension (chosen)

* Good, because the milestone does not depend on unconfirmed proxy/session-affinity properties (§1a).
* Good, because AUTH-R-02 makes the public-key credential desirable, not required.
* Bad, because TOTP is not phishing-resistant, so a relayed code remains possible.

### W2 — WebAuthn required for enforcement

* Good, because it is the strongest factor and bound to the origin.
* Bad, because it gates the mandate on §1a conditions 1 and 3, which are unconfirmed and owned outside
  this repository — and on device procurement nobody has approved.

### W3 — WebAuthn out of scope

* Good, because it removes a spike and a support story.
* Bad, because it forecloses the one factor that resists real-time phishing, contrary to AUTH-R-02's
  stated intent to add it "where the effort allows".

## Impact on the M2 schema draft

The schema draft that the milestone cites (`sql/migrations/drafts/mfa-second-factor.sql`, written
2026-11-10, recovered from commit `0b99eb2dd`, deleted in `1936f39c6`) predates these decisions. M2-1 must
rewrite it; three of its elements are **contradicted** here and must not be resurrected as-is:

| Draft element | Verdict |
|---|---|
| `secretEncrypted` — "encrypted with the existing PKCS12 vault key (same mechanism as personal access tokens)" | **Keep the column, change the mechanism.** Vault master key + AES-GCM under a dedicated alias (F1); the PAT encoder cannot carry a seed (F2) |
| `users.mfaPolicyOverride` (`REQUIRED` / `NOT_REQUIRED`) | **Drop.** A runtime, non-expiring per-account bypass is the exemption path P1/B1 reject. Replace with a dated grace window that ends by itself, and keep the posture in configuration. A future tightening-only override (item 6) may reintroduce a column, but never a `NOT_REQUIRED` value |
| `user_login_throttle` (PK `userId`, FK to `users`) | **Drop.** `auth_attempt` already covers both steps with a hashed key, no FK, an atomic upsert and a per-row window; an FK cannot count an unknown account and would make the throttle an enumeration oracle (AUTH-NFR-03) |
| `user_second_factor` (public key only for WebAuthn, soft delete, unique display name) | **Keep.** Matches AUTH-NFR-01 and AUTH-R-04 |
| `user_recovery_code.codeHash` | **Keep**, and state explicitly that verification must not require the vault key (B1) |

Still missing from the draft: the dated grace-window column (P1 — and its four invariants are the part
most likely to be implemented wrong, since the column alone says nothing about which way a missing value
resolves), an audit-event contract for AUTH-R-04, and the purge/retention story delegated to #1669.

## Out of scope for this ADR

* **Personal access tokens** — AUTH-R-01 already excludes programmatic access. The PAT posture is a
  policy decision for M4 and is recorded there, not here.
* **Federation to retire the password** — revisit trigger after M7, one 0.5-day feasibility note (M9-3).
* **Administratively set per-account policy** — deferred to a future, non-blocking slice (item 6);
  tightening-only, reusing the same grace window.
* **Passwordless (passkey-first) login** — M9-2, only once passkey coverage is measured.
* **TOTP library and BOM choice** — a dependency decision with a `sigcheck` signature requirement
  (milestone §6), not an architectural one.

## Links

* Milestone: [`MILESTONE-MFA-self-governed-2fa.md`](../plans/MILESTONE-MFA-self-governed-2fa.md) — §1 architecture,
  §1a passkey prerequisites, §7 risks (R8 vault-key loss, R9 recovery codes), §8 requirement IDs
* Input note for this decision: [`ADR-0011-input-orcid-role.md`](../plans/ADR-0011-input-orcid-role.md)
* Requirements: `AUTH-R-01`–`AUTH-R-04`, `AUTH-R-07`, `AUTH-NFR-01`–`AUTH-NFR-03`, `AUTH-C-01` in
  [`docs/requirements.md`](../requirements.md)
* Supersedes the milestone plan's wording "secrets encrypted with the existing vault key (same pattern
  as personal access tokens)" in M2-3: the PAT encoder is a one-way hash and cannot carry a TOTP seed
* Supersedes the pre-decision schema draft `sql/migrations/drafts/mfa-second-factor.sql` where it
  conflicts with F1, P1 and B1 (see *Impact on the M2 schema draft*)
* Reuses [ADR-0002](0002-invenio-rdm-api-client-credentials.md) S2 (vault master key + AES-GCM blobs) and
  D1 (decryption boundary at infrastructure)
* Relates to [ADR-0005](0005-developer-email-error-notifications.md) only through the shared PKCS12 vault
