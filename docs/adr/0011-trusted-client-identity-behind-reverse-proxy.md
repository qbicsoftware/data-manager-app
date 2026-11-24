# 0011 — Trusted client identity behind the reverse proxy, and per-origin auth throttling

* Status: **proposed** — awaiting human review (AGENTS.md §12 requires approval before an ADR is
  accepted; this record exists so the decision is reviewable rather than buried in a diff)
* Deciders: project team; Product Owner for the threshold defaults
* Date: 2026-11-17

Technical Story: [#1691](https://github.com/qbicsoftware/data-manager-app/issues/1691) · parent story
`FEAT-AUTH-FLOWS-03` ([#1675](https://github.com/qbicsoftware/data-manager-app/issues/1675)) · feature
`FEAT-AUTH-FLOWS` ([#1672](https://github.com/qbicsoftware/data-manager-app/issues/1672)) · requirement
`AUTH-NFR-03`

## Context and Problem Statement

`AUTH-NFR-03` requires authentication failures to be uniform in content and timing, and throttled
**per account and per request origin**. Account scope shipped in #1690. Origin scope was deferred
because the application could not determine a client address at all: it runs behind a TLS-terminating
HAProxy, `server.forward-headers-strategy` was unset (`NONE`), and nothing read
`X-Forwarded-For`. `request.getRemoteAddr()` was therefore the proxy — one value for every user in the
deployment.

Keying an origin bucket on that value would not weaken the control, it would invert it: one anonymous
attacker filling the single bucket would lock out the entire user population. So the gap was recorded
rather than filled, and the residual risk — an attacker can lock out a *known* victim by spamming
their address — was documented in the operations guide §1.8.

Two questions had to be answered before the gap could be closed:

1. **Which component may rewrite the client address?** The answer determines whether an origin bucket
   is a control or a decoration.
2. **How do the account and origin buckets combine,** given that the requirement asks for both
   criteria that pull in opposite directions: *an origin spraying many accounts must be throttled*, and
   *a victim spammed from someone else's origin must still be able to log in*?

## Decision Drivers

* `AUTH-NFR-03`: per-account **and** per-origin throttling, with a response indistinguishable from an
  unthrottled one.
* A bucket keyed on an attacker-controlled value is strictly worse than no bucket: it appears to work
  while being bypassable, which is how a control gets trusted and then relied upon.
* A bucket keyed on the proxy is worse still: it converts a rate limit into a global denial of service.
* The authority for the client address must be the servlet container, not the application, or the two
  can disagree during an incident.
* One origin may legitimately be a whole institution behind one NAT, so the origin threshold cannot be
  the account threshold.
* Every consumer of the client address — this throttle, the absolute links in confirmation and reset
  mail, and the OAuth2 redirect URI — needs the same three conditions to hold. Whatever is decided here
  also unblocks passkeys and the ORCID `{baseUrl}` redirect.
* The change alters how absolute URLs are built, and there is no staging HAProxy environment. It must be
  independently revertable at every step.

## Considered Options

### Option A — `ForwardedHeaderFilter`, i.e. `server.forward-headers-strategy=framework`

The application parses the headers itself via Spring's `ForwardedHeaderFilter`.

### Option B — Tomcat `RemoteIpValve`, i.e. `server.forward-headers-strategy=native`

The servlet container rewrites the address, gated on a trusted-proxy allowlist.

### Option C — Read `X-Forwarded-For` in the application and parse it ourselves

Bypass both, and have the throttle read the header directly.

### Option D — Keep account scope only, and shorten the block duration

Accept the lockout risk and mitigate it by configuration (§1.8 option 2).

### For the combination rule, additionally considered

* **D1 — OR the buckets.** "Either bucket over threshold blocks."
* **D2 — AND the buckets.** "Both buckets must be over threshold to block."
* **D3 — Conditional attribution.** Block on the origin bucket; block on the account bucket only when
  the requesting origin is itself implicated.

## Decision Outcome

**Option B** for the transport, and **D3** for the combination.

### Why Option B

`RemoteIpValve` acts only when the connecting peer is inside
`server.tomcat.remoteip.internal-proxies`, then walks `X-Forwarded-For` **right-to-left**, skipping
internal proxies, and takes the first non-internal entry — discarding everything to its left as
client-supplied. Verified in tomcat-embed-core 11.0.27, `RemoteIpValve.process`:

```java
for (idx = remoteIpHeaderValue.length - 1; idx >= 0; idx--) {
```

`ForwardedHeaderFilter` (Option A) instead takes the **leftmost** entry with no trust check
(`ForwardedHeaderUtils`, `getLeftMostValue(forHeader)`). Because HAProxy's `option forwardfor`
*appends*, the leftmost entry is the client's own — so under Option A the client chooses the value that
becomes its rate-limit identity, and can mint a fresh bucket per request.

Option C fails the same way and adds a second problem: the application would be re-implementing a trust
boundary that the container already implements correctly, and the two would inevitably drift.

**Operational consequence:** HAProxy must also *replace* the header
(`http-request set-header X-Forwarded-For %[src]`), not merely append. The valve makes that
defence-in-depth rather than the only defence, which is why both are required.

### Why D3, and not OR

D1 satisfies the first criterion and violates the second, because the attacker's spam is exactly what
filled the victim's account bucket. D2 satisfies neither usefully: an attacker spraying many accounts
never fills any single account bucket.

D3 blocks when:

```
refuse ⇔ origin bucket is blocked
       ∨ (account bucket is blocked ∧ this origin has itself produced ≥ login.max-attempts + 1 failures)
       ∨ (account bucket is blocked ∧ no trusted origin is available)
```

The attribution threshold is **derived** from the account limit, not configured. Filling an account
bucket costs an attacker exactly `max-attempts + 1` failures from one origin, so an attacker who does
the damage necessarily crosses it — and a legitimate user's own fumbling cannot, because the account
bucket stops them at `max-attempts`. A separately tunable number would allow the pair to be set such
that the attacker stays below it, silently restoring the lockout. This is a safety property, not
tidiness; the spec that pins it exists because an off-by-one in this derivation was caught during
review.

**Accepted cost, stated rather than hidden:** while an account is blocked, an origin that has never
been seen still gets `max-attempts` attempts against it before it is treated as the source of the
damage. That bound is the price of the second criterion; the alternative is the lockout being fixed.

### Origin keys are grouped networks, not addresses

A bucket keyed on a single address is evaded for free by an attacker holding an allocation: a
residential IPv6 prefix is commonly a `/56` or `/48`, and an IPv4 `/24` is a normal hosting allotment.
Origin keys therefore use the IPv4 `/24` or IPv6 `/64`, with IPv4-mapped IPv6 folded to IPv4 first (a
dual-stack client reaches the server both ways and must not get two buckets). Keys are hashed exactly
like account keys — `scope NUL network` — so `auth_attempt` never holds a readable address.

The `+1` and the grouping are both why the origin threshold (default 60) must sit well above the
account threshold (default 10).

### Origin scope ships disabled, and a half-configuration fails startup

`qbic.auth-throttle.login.origin.enabled` defaults to `false` and
`server.forward-headers-strategy` is left unset. Merging this changes nothing in production.

When an operator sets `enabled=true`, a class (`OriginTrustConfiguration`) refuses to start the
application unless the strategy is `native`. Both wrong combinations — `framework` (client controls its
own key) and `none` (one bucket for everyone) — are silent, and both are worse than account scope
alone. This is the issue's *blocking check* ("if `getRemoteAddr()` still shows a proxy, stop") expressed
where it cannot be skipped, rather than as a runbook line.

When the origin cannot be resolved at runtime (no header, untrusted peer, unparseable value), the
resolver answers `ClientOrigin.unknown()` and the throttle falls back to account scope — never to
"allow". A degraded deployment therefore loses origin scope and keeps account scope, and logs a warning
once. It does not silently key everyone on the proxy.

### Positive Consequences

* `AUTH-NFR-03` is satisfied for its origin clause. A victim spammed from another origin can log in.
* The trust boundary lives in one place (the container), configured by one property, and the
  application never parses a forwarded header.
* The same three conditions unblock passkeys and the ORCID `{baseUrl}` redirect (milestone plan §1a).
* No schema change: `auth_attempt.scope` is an open `varchar`, so the origin bucket is a second row.
* Discovery of the client address is now a port (`ClientOriginResolver`), so the throttle is testable
  without an HTTP container and the specs can assert the *negative* properties — that a proxy or
  forged address never becomes a key.

### Negative Consequences

* Two settings must agree, and one of them is not in this repository. A deployment that sets
  `native` but does not make HAProxy replace the header still works, but relies on the valve — worth
  knowing during an incident.
* Origin scope is **off after this change**, so the issue's benefit is not realised until an operator
  performs the canary rollout in ops §1.7. The code being present is not the control being active; the
  ops doc says so explicitly and the verification checklist includes a query that distinguishes the two.
* While an account is blocked, a previously unseen origin can still spend `max-attempts` guesses against
  it (see above).
* Only the login flow is origin scoped. The resend and registration flows would need their own origin
  thresholds calibrated against the population behind one NAT before switching on; shipping a guessed
  number would either throttle institutions or do nothing. The mechanism is flow-agnostic, so adding
  one is a line in `originScopeFor` plus three properties.
* The origin bucket is never cleared on success, by design (clearing it would let an attacker with any
  account reset their origin's tally between guesses), so it retains rows until #1669's purge lands.

## Pros and Cons of the Options

### Option A — `framework`

* Good, because it needs no container-specific configuration.
* Bad, because it takes the leftmost `X-Forwarded-For` entry with no trust check.
* Bad, because HAProxy appends, so the client writes that entry. **Bypassable.**

### Option B — `native` (chosen)

* Good, because it refuses the header from an untrusted peer and discards client-supplied entries.
* Good, because the trust boundary is a single documented property.
* Bad, because it is Tomcat-specific; a container change would need this revisited.

### Option C — parse in the application

* Good, because it needs no container configuration.
* Bad, because it re-implements a trust boundary that already exists correctly, and would drift.
* Bad, because it inherits the same leftmost-vs-rightmost trap by default.

### Option D — keep account scope only

* Good, because it needs no proxy change.
* Bad, because it does not satisfy `AUTH-NFR-03`, and the lockout risk is a takeover *enabler*: an
  attacker who can lock out a victim can also time attempts around the lockout window.

## Links

* Requirement: `AUTH-NFR-03` in `docs/requirements.md`
* Implementation plan and acceptance-criteria mapping:
  [`docs/plans/plan-1691-origin-scope-throttling.md`](../plans/plan-1691-origin-scope-throttling.md)
* Operator contract and rollout checklist:
  [`docs/operations/installation-and-administration.md`](../operations/installation-and-administration.md)
  §1.2, §1.4, §1.5, §1.7, §1.8
* Migration context (no schema change):
  [`sql/migrations/auth-attempt-throttling.sql`](../../sql/migrations/auth-attempt-throttling.sql)
* Related: [ADR-0002](0002-invenio-rdm-api-client-credentials.md) (external credential handling),
  [ADR-0005](0005-developer-email-error-notifications.md) (fail-closed startup checks — the same
  pattern `OriginTrustConfiguration` follows)
* Blocks: WebAuthn/passkeys and the ORCID `{baseUrl}` redirect —
  [`docs/plans/MILESTONE-MFA-self-governed-2fa.md`](../plans/MILESTONE-MFA-self-governed-2fa.md) §1a
