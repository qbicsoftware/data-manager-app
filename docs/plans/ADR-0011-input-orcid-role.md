# Input for ADR 0011 — what the ORCID slice settled

**For:** [#1667](https://github.com/qbicsoftware/data-manager-app/issues/1667) — *ADR 0011: MFA
authentication authority = the application*
**Status:** input note, **not** the ADR. AGENTS.md §12 requires human approval before an ADR is created,
so this records the facts the ADR must account for and leaves the decision and its wording to the owners.

---

## 1. The decision that is already made in code

`AUTH-R-07`, implemented and merged, settles part of the question ADR 0011 asks:

> The ORCID iD is **data about the account**, in the same category as the full name and the email
> address. It is never an input to the authentication decision.

This is not a preference. It follows from `AUTH-R-01` (a second factor for *every* interactive login,
no exemptions) and `AUTH-C-01` (the second factor must not depend on an external IdP). A provider that
can authenticate is a path the factor does not cover.

**What ADR 0011 should therefore record as context:** the external-provider option was not rejected on
convenience grounds. It was rejected because an enforcement point outside the application makes the
requirement unstatable for the population whose home organisation does not enforce MFA, and because the
provider cannot be mandated for external collaborators.

## 2. Alternatives, and what the slice showed about them

| Alternative | Status after this slice |
|---|---|
| **Application owns the factor** (chosen direction) | Consistent with the merged code: one login path, one principal type, the app decides. |
| **bwIDM2 / RegApp / FeLS federated step-up** | Out of scope for the milestone with a recorded revisit trigger. Unchanged by this slice — but note the federation question is now *only* about retiring the password as first factor, not about ORCID, which no longer participates in authentication. |
| **ORCID `amr` claim** | Dead. The claim is Member-API only, and ORCID cannot be mandated for all users. Additionally the provider is no longer in the login path at all. |

ADR 0011 should state the first two; the third is now a non-option rather than a considered one.

## 3. Facts the ADR's consequences section should reflect

- **There is exactly one principal type.** `QbicUserDetails`. `QbicOidcUser` is deleted, along with the
  success handler and user-details service that produced it.
- **The filter chain contains a client grant, not a login.** `oauth2Client()`, not `oauth2Login()`. This
  is the structural guarantee behind *"linking shall never by itself create a session"*.
- **The session is not a place to park a principal.** `OidcLinkController` used to store an
  `Authentication` so a handler could restore it; that is removed deliberately, because
  `SECOND_FACTOR_PENDING` will need the session to mean something precise.
- **Factor storage is owned by `identity`.** The schema drafts (`user_second_factor`,
  `user_recovery_code`) sit in that bounded context, matching `AUTH-NFR-01`.

## 4. What ADR 0011 still has to decide (not answered here)

1. **Factor store ownership and encryption.** `AUTH-NFR-01` requires secrets encrypted with a key outside
   the database. The existing vault (used for personal access tokens) is the obvious carrier — the ADR
   should say so explicitly or reject it.
2. **The enforcement switch.** `MfaPolicy` as a bean with `off`/`optional`/`required` and per-user
   override. The milestone proposes this; the ADR should record why policy is code-configured rather
   than database-configured.
3. **The break-glass posture.** `R8` in the milestone notes that vault-key loss is a total lockout unless
   recovery codes live outside the vault-encrypted path. That is an ADR-level consequence, not a
   task-level one.
4. **Whether passkeys are in scope for the milestone.** `AUTH-R-02` makes WebAuthn optional; the ADR
   should state whether the milestone ships it or only the spike.

## 5. Blocking relationship

The milestone treats ADR 0011 as the M0 gate before M2 (factor storage). The ORCID slice has now removed
the obstacle that made that sequencing necessary — there is one authentication path to protect — but it
has not written the ADR, and no agent should. Until it exists, M2's schema has no recorded rationale.
