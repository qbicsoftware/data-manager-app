# Plan — WebAuthn / FIDO2 as a second-factor type

**Parent:** [MILESTONE-MFA](MILESTONE-MFA-self-governed-2fa.md) · ADR [0011](../adr/0011-mfa-authentication-authority.md)
decision **W1** (WebAuthn is an offered extension, decided by the M3-2 spike) · Requirement
AUTH-R-02 ("a public-key credential is desirable but *not* a precondition")

**What this plan covers:** the implementation path for adding WebAuthn as a second-factor type
alongside TOTP, self-hosted, in this application. The small version, not FIDO2-only.

**What this plan is not:** a procurement decision. Mandating institute-issued hardware keys is a
separate decision with its own costs (§8); this plan makes such a mandate *possible* (§5.4) without
requiring it.

---

## 1. Verified facts this plan rests on

Each was read out of the code on this branch, not from memory.

| Fact | Where verified |
|---|---|
| `SecondFactorType.WEBAUTHN` already exists in the enum | `identity/.../domain/model/mfa/SecondFactorType.java:34` |
| `user_second_factor.webauthnCredential text` column already exists | `sql/migrations/drafts/mfa-second-factor.sql:148` |
| `chk_user_second_factor_shape` already has the WEBAUTHN branch (`type='WEBAUTHN' AND webauthnCredential IS NOT NULL AND secretCiphertext IS NULL`) | `sql/migrations/drafts/mfa-second-factor.sql:160` |
| `SecondFactor.enrolWebAuthn(userId, credential, displayName, enrolledAt)` exists | `SecondFactor.java:125` |
| A WebAuthn credential already round-trips through the store (spec-covered) | `MfaPersistenceSpec` — "a WebAuthn credential round-trips and exposes no secret" |
| The TOTP verifier already skips non-TOTP factors: `if (factor.type() != SecondFactorType.TOTP) continue;` with the comment *"WebAuthn assertions are verified by their own path (M4-2); a code cannot satisfy them."* | `SecondFactorVerificationService.java:78` |
| `MfaGate.decide(...)` is factor-type agnostic — it takes `boolean hasActiveFactor` | `MfaGate.java:88` |
| `MfaPolicy` is **deployment-global**: `mode`, `grace`, `graceCutoff`. No per-account or per-role override exists | `MfaPolicy.java:54` |
| `SecondFactor` has `lastUsedAt` only; `markUsed(Instant)` writes it with no compare-and-set. The credential is an **opaque `text`** — no `credentialId`, `signatureCount`, `aaguid` or `transports` columns | `SecondFactor.java:76,232` |
| The pending-login state is a small session-held record: `MfaPendingLogin(userId, email, createdAt)` | `MfaPendingLogin.java:35` |
| Spring Security **7.2.0** is on the classpath, so `spring-security-webauthn` is a same-version add | `mvn help:evaluate spring-security.version` → `7.2.0` |
| `spring-security-webauthn` is **not** currently a dependency | no `webauthn` match in `pom.xml`, `datamanager-app/pom.xml`, `datamanager-bom/pom.xml` |
| `spring-security-webauthn:7.2.0` **is published** on Maven Central, and its only new third-party group is `com.webauthn4j:webauthn4j-core:0.31.11.RELEASE` | `repo1.maven.org/.../spring-security-webauthn/7.2.0/`, its `.pom` |
| The frontend already loads npm modules into Vaadin's module space (`import QRCode from 'qrcode'`) and there is no root `package.json` — the dependency comes from Vaadin | `frontend/javascript/totpqrcode.js:14`, `TotpQrCode.java:32` |
| The build has a signature gate with a keys allow-list (`sigcheck`, `pgp-keys-override.list`, 290 lines) | `pom.xml:263`, `pom.xml:317` |

**Two consequences worth stating up front.** The data model is *already* FIDO2-shaped — this is an
unfinished seam someone left deliberately, not a redesign. And the "FIDO server" is the library
already on the classpath plus a four-method repository interface; no vendor service is involved, which
is what ADR 0011 A1 requires.

## 2. Why no external FIDO server

WebAuthn has two parties: the **authenticator** (phone, laptop, security key) and the **relying
party** — the server that issues the challenge and verifies the signature. In this architecture the
relying party is the Data Manager. There is no third party in the protocol.

A commercial "FIDO server" is a hosted credential repository plus a policy console plus attestation
metadata upkeep, usually wrapped in SSO — i.e. ADR 0011's options **A2–A4, rejected**, because:

> A2–A4 all make enforcement conditional on a third party's enrolment state, coverage, availability
> and willingness to be mandatory, which is exactly what AUTH-C-01 forbids.

The population here has open self-service registration and external collaborators. Making the mandate
contingent on a vendor's uptime and on the population having enrolled *there* is the thing AUTH-R-01
rules out. Self-hosted keeps the posture identical to TOTP, recovery codes and throttling: we own
verification.

**What is genuinely lost by not buying one:** vendor-maintained attestation metadata (MDS) — knowing
*which model* of authenticator a user holds. That matters only for an institute-issued-hardware
mandate; §5.4 shows what can be done without it.

## 3. What must be built

```
                      exists                                  new
                ┌───────────────────┐              ┌────────────────────────────┐
 domain         │ SecondFactor      │              │ WebAuthnCredential         │
                │  .enrolWebAuthn() │              │  value object: credential  │
                │ SecondFactorType  │              │  id, public key, counter,  │
                │  .WEBAUTHN        │              │  aaguid, transports, BE/BS │
                └───────────────────┘              └────────────────────────────┘

 application    │ MfaGate (agnostic)│              │ WebAuthnEnrolmentService   │
                │ MfaPolicy (global)│              │ WebAuthnAssertionService   │
                │ RecoveryCodeService│             │ RelyingPartyConfig         │
                └───────────────────┘              └────────────────────────────┘

 infrastructure │ JpaSecondFactor   │              │ JpaUserCredentialRepository│
                │  Repository       │              │  implements Spring's       │
                └───────────────────┘              │  UserCredentialRepository  │
                                                   └────────────────────────────┘

 security       │ MfaLoginSuccess   │              │ challenge bridge           │
 (app)          │  Handler          │              │  pending login <-> session │
                │ MfaChallenge      │              └────────────────────────────┘
                │  EnforcementFilter│
                └───────────────────┘

 ui             │ TotpEnrolment...  │              │ PasskeyEnrolmentSection    │
                │ SecondFactorView  │              │  button + JS ceremony      │
                │ TwoFactorSettings │              │ passkey option on challenge│
                └───────────────────┘              └────────────────────────────┘
```

## 4. Deployment prerequisites — and a correction to ADR 0011

The milestone plan §1a lists five. **ADR 0011 W1's condition 1 is wrong, and leaving it uncorrected
would schedule this work behind something it does not depend on.**

ADR 0011 says WebAuthn is gated on `server.forward-headers-strategy` because "the app cannot
resolve its own public origin". That is not how Spring Security's WebAuthn works. In
`Webauthn4JRelyingPartyOperations` (spring-security-webauthn), `rpId` and
`allowedOrigins` are **constructor-supplied static values**, converted via
`new Origin(String)`. Nothing is derived from the request. The app therefore does *not* need
request-derived origin resolution for WebAuthn.

The real prerequisites are:

| # | Requirement | Status |
|---|---|---|
| 1 | **Exactly one public origin per environment**, matching `rpId` + `allowedOrigins` | **Blocking.** Canonical origin: `https://rdm.qbic.uni-tuebingen.de`. A staging instance on another host needs its own values |
| 2 | **`rpId` = `rdm.qbic.uni-tuebingen.de`** (narrowest valid value, not a parent domain) | Config |
| 3 | **Sticky sessions on HAProxy** — the challenge lives in the HTTP session, so the assertion POST must land on the issuing node | ✅ **Confirmed present (2026-11-27).** No longer a blocker |
| 4 | Valid TLS certificate on the public origin | Ops — browsers refuse `navigator.credentials` outside a secure context |
| ~~5~~ | ~~`server.forward-headers-strategy`~~ | **Not required for WebAuthn.** Still needed for #1691 and for building absolute mail/OAuth links — a separate concern |

**Action:** correct ADR 0011 W1 and milestone §1a row 1 to say this, so nobody schedules passkeys
behind the #1691 operator step.

Note also that `HttpClientOriginResolver` / `ClientOrigin` do **not** help here,
despite the shared word. `ClientOrigin` is a *grouped network* for rate limiting; WebAuthn
needs the *web origin* (scheme + host). What transfers is `OriginTrustConfiguration`'s
**pattern** — a startup gate that refuses to boot on a misconfiguration — which is exactly right for a
wrong `rpId` (§5.3).

## 5. Design decisions

### 5.1 Schema: two columns now, while the draft is still a draft

The credential is currently an opaque `text` blob whose own comment says it holds
"credentialId, public key, signature counter, transports, AAGUID, backup flags". Two of those cannot
live in a blob:

| Column | Why it must be a column | Notes |
|---|---|---|
| `credentialId` `varchar(1024)` **UNIQUE** | The assertion path looks a credential up **by credential id, without knowing the account** — the mirror of the recovery-code lookup. Cannot be a blob scan | Same reasoning as `user_recovery_code.codeHash` being globally unique |
| `signatureCount` `bigint` | Clone detection needs a **compare-and-set** (`UPDATE ... WHERE signatureCount = :expected`). A blob read-modify-write has no atomicity, and clone detection without atomicity is theatre | `NULL` for TOTP |

Optional but cheap now, deferrable: `aaguid` `char(36)`, `transports`
`varchar(255)`, `backupEligible` / `backupState`
`tinyint(1)` — the last two also need to be readable for the policy tier in §5.4.

> ✅ **Done 2026-11-27** — both columns are now in `sql/migrations/drafts/mfa-second-factor.sql`,
> before the release cut, while the table is empty everywhere. The `CHECK` was widened at the same
> time so the shape invariant covers them (below). No `ALTER` will be needed.

The `chk_user_second_factor_shape` CHECK needed widening, not rewriting. It was already an OR over
both shapes, but it said nothing about the new columns, so a TOTP row could have carried a
`credentialId` and a WEBAUTHN row could have had none. It now reads:

```sql
CHECK ((type = 'TOTP'     AND secretCiphertext IS NOT NULL AND webauthnCredential IS NULL
                          AND credentialId IS NULL AND signatureCount IS NULL)
    OR (type = 'WEBAUTHN' AND webauthnCredential IS NOT NULL AND secretCiphertext IS NULL
                          AND credentialId IS NOT NULL))
```

`signatureCount` is deliberately unconstrained for WEBAUTHN: `NULL` means "this authenticator
reports no counter" and `0` means "it reports one, currently zero". Those are different facts, and a
constraint that collapsed them would silently disable clone detection for every authenticator that
starts at zero.

### 5.2 A sibling verifier, not a rewrite of the TOTP one

`SecondFactorVerificationService.verify(userId, code, now)` is *code-shaped* — the proof is a
string the user types. A WebAuthn assertion is a structured `PublicKeyCredential`
(clientDataJSON + authenticatorData + signature), which that signature cannot carry.

So: add `WebAuthnAssertionService.verify(userId, credentialJson, expectedChallenge, now)` as a
**sibling**, and leave the existing service untouched apart from its already-present `continue`.
The seam is pre-cut; do not reunify them behind a common `verify(Object)`.

### 5.3 `rpId` is validated at startup, not at first login

Follow `OriginTrustConfiguration`'s precedent: a bean that fails startup when
`qbic.webauthn.rp-id` / `allowed-origins` are absent or inconsistent, with the
message naming the public origin. A wrong `rpId` is silently broken — every ceremony fails
with an opaque browser `SecurityError` — which is precisely the class of failure that must
not reach production.

```properties
qbic.webauthn.rp-id=${WEBAUTHN_RP_ID:rdm.qbic.uni-tuebingen.de}
qbic.webauthn.rp-name=${WEBAUTHN_RP_NAME:Data Manager}
qbic.webauthn.allowed-origins=${WEBAUTHN_ALLOWED_ORIGINS:https://rdm.qbic.uni-tuebingen.de}
```

### 5.4 Two policy tiers, both tightening-only

`MfaPolicy` today is global: `mode`, `grace`, `graceCutoff`.
ADR 0011 **rejected** a general per-account override because "an override can express *not required for
this account*", which becomes the permanent exemption AUTH-R-01 forbids. That rejection stands.

What is permitted, and what the hardware question needs, is a **role-scoped minimum** — a value that
can only *add* a requirement:

```properties
qbic.security.mfa.required-factors=<role>:<condition>[,<role>:<condition>]
   condition := ANY                       # any active factor — today's behaviour
              | DEVICE_BOUND              # credential with backupEligible = false
              | ATTESTED:<aaguid>[,...]   # needs attestation, see below
```

`backupEligible` and `backupState` arrive in **signed authenticator data**, so the
server reads them truthfully — no browser-reported hearsay, no metadata service. `DEVICE_BOUND`
therefore costs nothing beyond §5.1's column. It proves the credential is not cloud-synced; it does not
prove institute ownership.

`ATTESTED:<aaguid>` is the stronger claim and needs (a) attestation-format verification at
enrolment and (b) an AAGUID allow-list from procurement. **Defer it** until hardware is actually being
issued.

### 5.5 The challenge bridge is the part that fights the current flow

The TOTP challenge is stateless in the sense that matters: the code *is* the proof, so one POST
completes it. WebAuthn is a multi-round ceremony — begin (server issues challenge) → authenticator
signs → finish (server verifies). Two round trips, with server-side challenge state.

This does not break `MfaPendingLogin` — it **helps**.
`MfaPendingLogin(userId, email, createdAt)` is already exactly what the challenge step needs:
which account, which session, how old. The bridge is:

1. `GET /login/second-factor` → if the account has a WebAuthn factor, call begin, store the
   `PublicKeyCredentialRequestOptions` in the **HTTP session**
   (`HttpSessionPublicKeyCredentialRequestOptionsRepository` does this), render the button.
2. Browser runs `navigator.credentials.get()` → POSTs the assertion.
3. finish verifies, **consumes the challenge exactly once**, CAS-increments `signatureCount`,
   then proceeds through the existing `establishSession()` / `MfaSessionCompletion`
   path.

Two consequences to design for explicitly:

- **Single-use challenge consumption must be enforced**, not assumed. A replayed assertion must fail.
  This is the WebAuthn analogue of the recovery-code "exactly once" rule and deserves the same kind of
  spec (`RecoveryCodeThrottleSpec` and the concurrency cases in `MfaPersistenceSpec`
  are the model).
- **The challenge lives in the session**, which is why sticky sessions (§4 #3) is a hard prerequisite
  and why `MfaPendingLogin`'s `createdAt` must gate the challenge's age.

Throttling reuses `AuthAttemptThrottle.Scope.SECOND_FACTOR` — already added for MFA-68,
already covering recovery codes. **Do not add a second bucket.** Failed assertions and failed codes
should share the account-keyed allowance; they are the same attack.

### 5.6 The vault key is bypassed — and that strengthens ADR 0011 B1

`SecondFactorSecretProtector` exists because a TOTP seed is a *secret* that must be recovered
to compute the expected code. A WebAuthn credential is a **public** key: there is nothing to protect.

So vault-key loss knocks out **TOTP only** — passkeys *and* recovery codes survive. That is an argument
*for* shipping WebAuthn that ADR 0011 does not currently make, and it is the strongest one available.
**Action:** add it to ADR 0011 §B1's rationale.

### 5.7 The client needs no new npm package

`navigator.credentials.create()` and `.get()` are **browser-native**. Unlike
`totpqrcode.js` (which needs `qrcode` from Vaadin's module space), the ceremony
needs a small `@JsModule` handler and nothing else — no supply-chain surface on the frontend at
all.

```javascript
// frontend/javascript/webauthn.js
// window.qbicWebAuthn = { create(optionsJson), get(optionsJson) }
const b64urlToBuf = s => { /* base64url -> ArrayBuffer */ }
const bufToB64url = b => { /* ArrayBuffer -> base64url */ }
window.qbicWebAuthn = {
  async get(optionsJson)   { /* decode, navigator.credentials.get, encode result */ },
  async create(optionsJson) { /* same for registration */ }
}
```

The server must convert base64url ⇄ byte arrays on both ends; getting this wrong is the single most
common WebAuthn integration bug. Pin it with a unit test over fixed vectors.

## 6. Phases and sizing

**Rough band: 15–22 engineer-days.** ~18–25 files touched, of which 10–12 are new. Two people can run
phases 4 and 5 in parallel once 2–3 are settled, giving ~2 calendar weeks.

**Updated 2026-11-27:** sticky sessions confirmed present, so the largest deployment unknown is gone.
One blocker remains — exactly one public origin per environment, §7.2.

| Phase | Work | Size | Depends on |
|---|---|---|---|
| **1. Dependency + config plumbing** | `spring-security-webauthn` into `datamanager-app/pom.xml` (version from the Spring Security BOM, no explicit version); `com.webauthn4j:*` entry in `pgp-keys-override.list` (the `sigcheck` gate will otherwise fail the build); `qbic.webauthn.*` properties | **0.5–1 d** | — |
| **2. Credential repository adapter** | `JpaUserCredentialRepository implements UserCredentialRepository` (4 methods: find by credential id, find by username, save, delete) over `user_second_factor`; the `credentialId` + `signatureCount` columns; **CAS on the counter** | **2–3 d** | 1 |
| **3. RP wiring + startup gate** | `Webauthn4JRelyingPartyOperations` bean from `qbic.webauthn.*`; refuse-to-boot check modelled on `OriginTrustConfiguration` | **1–2 d** | 1 |
| **4. Enrolment ceremony** | begin/finish application services; `webauthn.js` `@JsModule` + base64url codec tests; passkey section in `TwoFactorEnrolmentMain`; naming/removal in `TwoFactorAuthenticationMain` | **3–4 d** | 2, 3 |
| **5. Login assertion ceremony** | `WebAuthnAssertionService`; challenge bridge in `SecondFactorView`; single-use challenge; reuse the `SECOND_FACTOR` throttle; `establishSession()` unchanged | **4–5 d** | 2, 3 |
| **6. Policy, recovery, UI copy** | `required-factors` tier (§5.4, `DEVICE_BOUND` only); recovery codes shared as-is (no work — already factor-agnostic); copy for synced-vs-device-bound passkeys | **2–3 d** | 4, 5 |
| **7. Tests** | ~5–7 Spock specs (see §7.3) | **3–4 d** | all |

**Biggest phase is 5, then 2–3.** Those are the two everyone underestimates: the ceremony bridging and
the counter's atomicity. They must be settled before UI work starts.

**First slice, if you want to de-risk cheaply:** phases **1–3 only** — ~3–6 days, entirely server-side,
fully Spock-testable, no UI. That proves the dependency clears the supply-chain gate, the adapter maps
our table correctly, and the RP gate fails loudly on a bad config. Recommend doing exactly this before
committing to 4–5.

## 7. Risks

### 7.1 Sticky sessions — resolved

**Was the #1 passkey-behind-load-balancer failure.** Confirmed present on 2026-11-27, so the
session-held challenge will find its own node.

Worth keeping in the runbook anyway: this is a property of the *deployment*, not of the code. If
affinity is ever removed — a new backend, a rebalanced cluster, an autoscaling group — passkey login
breaks while password+TOTP keeps working, which makes it look like a passkey bug rather than a proxy
change. Note it in the ops doc next to the `rpId` settings.

### 7.2 Exactly one public origin per environment

`rpId` creep ("my passkey doesn't work on staging") is a real and recurring support cost. A
staging instance on another host needs its own `rp-id`/`allowed-origins`, which
means staging credentials are *separate credentials* — not transferable. Say so in the ops doc.

### 7.3 No browser E2E harness exists

The existing suite is Spock. A WebAuthn ceremony cannot be fully driven from Spock, exactly as the TOTP
enrolment already cannot (`HANDOVER-mfa-m3-totp-enrolment.md` admits the browser step is
manual). What *can* and should be spec-covered:

| Spec | Asserts |
|---|---|
| base64url codec vectors | The most common integration bug: wrong padding/charset both ways |
| `JpaUserCredentialRepositorySpec` | Round trip; lookup by credential id without the account; a disabled factor is invisible |
| signature counter CAS | Two concurrent assertions with the same counter → exactly one wins (mirror `MfaPersistenceSpec`'s concurrency cases) |
| challenge single-use | A replayed assertion is refused, and refused *without* consuming a second challenge |
| RP gate | Missing/mismatched `rp-id` fails startup with a message naming the public origin |
| factor-shape invariants | The existing `chk_user_second_factor_shape` cases already cover the WEBAUTHN branch — extend, do not duplicate |
| bypass | A WebAuthn factor alone never yields a session without a verified assertion (joins M4-5) |

The actual ceremony stays a manual acceptance step: enrolment on desktop + a phone + a hardware key, and
login with each.

### 7.4 Passkey sync changes the support story, and no UI shows it

`backupEligible`/`backupState` mean a credential may live in iCloud/Google and work
on a device the user does not physically hold. That is a *feature* for adoption and a *support problem*
without explanation: "I have a passkey but not on this laptop". Needs new UI copy **and** a
helpdesk-runbook line (M6-2), otherwise support has nothing to look at.

### 7.5 Supply chain

`spring-security-webauthn` is signed by the same key as every Spring artifact and is covered
by the existing `org.springframework.*` mapping. Only `com.webauthn4j:*` needs an
explicit entry — *smaller* than the pre-existing ZXing situation that `TotpQrCode`'s javadoc
documents as the reason the QR is drawn client-side. WebAuthn cannot be done client-side-only, so this
entry is unavoidable.

**The dependency resolves, verified without adding it:**

| Check | Result |
|---|---|
| `org.springframework.security:spring-security-webauthn:7.2.0` on Maven Central | **exists** (HTTP 200 on the `.pom`); the 7.2.0 train is published |
| Its transitive set | `spring-security-core`/`-web` 7.2.0, `spring-core`/`-beans`/`-web` 7.1.0, and **`com.webauthn4j:webauthn4j-core:0.31.11.RELEASE`** |

So the surface is exactly one new third-party group — `com.webauthn4j` — and only that coordinate
needs an explicit `pgp-keys-override.list` entry. Spring's own artifacts are already covered.

Two remaining checks for phase 1:

- `webauthn4j` pulls Jackson and `jackson-dataformat-cbor`. **Confirm the CBOR module is covered by
  `pgp-keys-override.list`**, not just the `webauthn4j` coordinates themselves.
- Adding this puts a new dependency **in the login path**, under the Critical/High advisory severity
  gate shipped in #1771. Expect the dependency scan to be part of the review.

### 7.6 Migration of the existing population: none required

WebAuthn is an *additional* factor type. Nobody enrolled with TOTP has to do anything; recovery codes
are shared across factor types by construction (`RecoveryCodeService.redeemFor(userId, ...)`).
This is the main reason the small version is the right one — FIDO2-only would require a forced
re-enrolment campaign for the entire population.

## 8. Out of scope (deliberately)

- **FIDO2-only / passkey-first login.** Usernameless discoverable credentials mean offering a passkey
  *before* identifying the account, which inverts `MfaLoginSuccessHandler` and
  `MfaPendingLogin` entirely, and overturns AUTH-NFR-02's "the factor is applied to the
  password path only". Milestone M9-2 already gates it on measured passkey coverage. **Months, not
  weeks.** Skip.
- **Attestation / AAGUID policy (`ATTESTED:<aaguid>`).** Deferred until hardware is issued
  (§5.4).
- **Mandated hardware keys for admins.** A programme decision, not a feature: procurement line,
  lost-key replacement process, support runbook, and an answer for "what does an admin do at 2 a.m. on
  a conference trip". ADR 0011 already declined this under W1 vs W2 and every one of those reasons
  still holds.
- **A FIDO server vendor.** Rejected in §2 / ADR 0011 A1.

## 9. Open questions for the deciders

1. ~~**Is the `mfa-second-factor.sql` release cut imminent?**~~ ✅ **Answered 2026-11-27** — the
   columns went into the draft before the cut. No migration will be needed.
2. ~~**Is sticky sessions actually configured on HAProxy?**~~ ✅ **Answered 2026-11-27** — yes.
3. **Are staging credentials expected to work on production?** They will not, by design (§7.2). If
   someone believes otherwise, that belief needs correcting in the ops doc before rollout.
4. **Does any cohort need `DEVICE_BOUND` on day one?** If yes, phase 6 ships with the feature;
   if no, it is configuration waiting for a decision.

## 10. Documentation to update as part of this work

| File | Change |
|---|---|
| `docs/adr/0011-mfa-authentication-authority.md` | **Correct W1 condition 1** — `server.forward-headers-strategy` is *not* a WebAuthn prerequisite (§4). Add the vault-key-blast-radius argument (§5.6) |
| `docs/plans/MILESTONE-MFA-self-governed-2fa.md` | §1a row 1 — same correction. Mark the M3-2 spike resolved by this plan |
| `docs/operations/installation-and-administration.md` §1.6 | `rp-id`/`allowed-origins` per environment; staging-credential caveat; sticky-session requirement |
| `docs/migrations/NEXT.md` | If the schema change lands after the cut: the two `ALTER`s with apply/verify/rollback |
| M6-2 helpdesk runbook | "passkey on another device", "synced vs device-bound", "lost all factors" |
