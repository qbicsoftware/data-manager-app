# PRD conflict — the authentication constraint is now inverted

**Status:** ⚠️ **open, PO action required** · Raised 2026-11-17 with `FEAT-AUTH-FLOWS-05` (#1677)
**File to change:** [`docs/prd.md`](../prd.md) §*Technical Constraints*, line 100
**Not edited by an agent.** The PRD is product-owned; this note exists so the discrepancy is not lost.

---

## The conflict

`docs/prd.md` §Technical Constraints currently states:

> **Authentication:** ORCID OAuth2 integration for researcher identity; local password-based
> authentication as fallback

Release `1.20.5` inverts this. After `AUTH-R-07` / #1677:

- **Local password authentication is the primary and only** way to sign in.
- ORCID is **not** an identity provider at all. It is an attribute of an existing account — the iD
  displayed next to the user's name and connected from *Settings → Profile*.

The PRD does not merely rank the two differently; it names ORCID as the researcher-identity mechanism,
which is exactly the role that was removed.

## Why it was inverted

An external provider cannot be the enforcement point for a second factor (`AUTH-C-01`), and a login path
outside the password cannot be covered by a second-factor check applied to the password path. While
`oauth2Login()` was configured, the milestone's goal — *no password-only login* — was unachievable
because a second, factor-free path existed.

`AUTH-R-07` also states the second half: linking an external identity shall never by itself create an
authenticated session. That is now a property of the filter chain rather than of a handler's discipline.

## Suggested replacement text

> **Authentication:** local password-based authentication for all accounts, with a mandatory second
> factor. ORCID (OAuth2) is integrated as an optional account attribute for researcher identification;
> it is not an authentication method.

The second-factor clause is deliberate: by the time this is read, `MILESTONE-MFA` will have made 2FA
mandatory, and leaving it out would make the constraint stale again immediately.

## Related

- `AUTH-R-07` in [`docs/requirements.md`](../requirements.md)
- `AUTH-C-01` (second-factor enforcement must not depend on an external IdP)
- ADR 0011 (MFA authentication authority) — still unwritten, and the M0 gate
- [`MILESTONE-MFA-self-governed-2fa.md`](MILESTONE-MFA-self-governed-2fa.md) §M1 decision **D1b**
