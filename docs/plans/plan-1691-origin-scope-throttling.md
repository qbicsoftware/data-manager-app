# Plan — #1691 trusted client identity and origin-scope throttling

**Issue:** [#1691](https://github.com/qbicsoftware/data-manager-app/issues/1691) · milestone
`MILESTONE-PROXY` · Parent story `FEAT-AUTH-FLOWS-03` (#1675) · Requirement `AUTH-NFR-03`

**What this PR does:** Phase D of the issue's staged plan — the code side of origin-scope
throttling — plus the app-side configuration that Phase C asks for. Phases B and C themselves are
HAProxy/deployment actions and are **not** in this repository.

**What this PR deliberately does not do:** flip `server.forward-headers-strategy` on. That is a
production behaviour change (it also changes how absolute links are built) and the issue stages it
so the operator can roll back at each point. See §4.

---

## 1. Verified facts this design rests on

Each was read out of the sources actually on the classpath, not from memory.

| Fact | Where verified |
|---|---|
| `RemoteIpValve` walks `X-Forwarded-For` **right-to-left**, skipping `internal-proxies`, and takes the first non-internal entry — everything to its left is discarded as client-supplied | `tomcat-embed-core` 11.0.27 `RemoteIpValve.process`, `for (idx = remoteIpHeaderValue.length - 1; idx >= 0; idx--)` |
| The valve only acts when the connecting peer is in `internal-proxies` (or `trusted-proxies`); otherwise `getRemoteAddr()` is left alone | same method, `if (isInternal \|\| isTrustedProxy(originalRemoteAddr))` |
| Boot wires the valve when `server.forward-headers-strategy=native` (or `remoteip.*` is set) | `TomcatWebServerFactoryCustomizer.customizeRemoteIpValve`, spring-boot-tomcat 4.1.2 |
| Boot's `remoteip.internal-proxies` default already covers RFC1918, loopback, link-local, CGNAT and ULA | `TomcatServerProperties.Remoteip` |
| `ForwardedHeaderFilter` (`framework`) takes the **leftmost** entry with no trust check | documented in §1.5 of the ops doc; the issue records the source line |
| The authentication failure event carries the same `Authentication` the filter built, so its `details` — a `WebAuthenticationDetails` holding `request.getRemoteAddr()` — is available in the listener | `ProviderManager.prepareException`, `WebAuthenticationDetails` |

**Consequence:** `native` is the only strategy for this stack, exactly as the issue states, and a
bucket keyed on an address that is still an internal proxy is the failure mode to design against.

## 2. The rule that satisfies both acceptance criteria

The two criteria in the issue pull in opposite directions:

1. *spam from one origin against many accounts → that origin is throttled*
2. *attacker fills a victim's account bucket from their own origin → the victim logging in from a
   different origin is **not** blocked*

A naive "either bucket over threshold blocks" satisfies (1) and **violates** (2), because the
victim's account bucket is full. The implementation therefore combines the buckets like this:

```
refuse  ⇔  origin bucket is blocked                                   # (1)
        ∨ (account bucket is blocked ∧ the requesting origin is itself implicated)   # (2)
        ∨ (account bucket is blocked ∧ origin scope is unavailable)   # legacy behaviour
```

*"Implicated"* means the requesting origin has accumulated at least `login.max-attempts + 1`
failures in the current window — the number that fills a whole account bucket. That threshold is
**derived, not configured**: an attacker who fills the bucket necessarily crosses it, while a
legitimate user's own fumbling cannot, because the account bucket stops them at `max-attempts`.
A separately tunable number would let the two be set such that the attacker stays below it, which
silently restores the lockout this clause exists to remove. (An off-by-one in this derivation was
caught while writing the spec for it, which is why the spec pins the boundary explicitly.)

The cost of the rule, stated rather than hidden: while an account is blocked, an origin that has
never been seen gets `max-attempts` attempts against it. That is the deliberate price of
criterion (2), and it is bounded by the origin threshold as well.

## 3. Changes

| Area | Change |
|---|---|
| `identity` / `IpNetworks` | Strict literal parser (never `InetAddress.getByName` on attacker input, which would resolve hostnames) + CIDR matching + address grouping: IPv4 `/24`, IPv6 `/64`, IPv4-mapped IPv6 folded to IPv4 first |
| `identity` / `ClientOrigin`, `ClientOriginResolver` | Value type carrying the grouped network, and the port that supplies it. `ClientOrigin.unknown()` is the honest "we could not establish a trusted origin" value |
| `identity` / `AuthAttemptThrottle` | `Scope.LOGIN_ORIGIN`; per-attempt write to both buckets; the combined decision above; origin key hashed like the account key (`scope NUL network`); the attribution threshold derived from the account limit |
| `datamanager-app` / `HttpClientOriginResolver` | Servlet adapter. Returns `unknown()` when the resolved address is unparseable **or still inside `internal-proxies`** — i.e. when the valve did not rewrite it |
| `datamanager-app` / `OriginTrustConfiguration` | Boot gate: `origin.enabled=true` without `server.forward-headers-strategy=native` **fails startup** |
| Call sites | `LoginThrottleFilter` (pre-check) and `AuthAttemptEventListener` (write), both taking the same `ClientOriginResolver`. The resend and registration views are **unchanged**: their throttle calls keep account scope, because their origin thresholds need telemetry first |
| `SecurityConfiguration` | Injects the resolver and hands it to the filter, so the filter and the listener cannot resolve the origin differently — the failure that would leave the control present, maintained and never firing |
| Config | `qbic.auth-throttle.login.origin.{enabled,max-attempts,window,block-duration}`, plus a documented (commented-out) `server.forward-headers-strategy` / `server.tomcat.remoteip.*` block |

### Invariants kept from the account-scope work

- **Never clear the origin bucket on success.** Clearing it would let an attacker reset their
  origin's tally by logging into their own account between guesses.
- **A blocked attempt is still recorded**, so hammering does not shorten a block.
- **The key is a hash**, never a raw address (`scope NUL network`, SHA-256).
- **Throttled and unthrottled responses stay indistinguishable** — the `Outcome` returned for
  rendering keeps describing the *account* bucket, and no caller learns which bucket fired.

## 4. Why nothing changes in production until the operator acts

`origin.enabled` defaults to `false` and `server.forward-headers-strategy` is left unset (`NONE`).
So after this merge:

- account-scope throttling behaves exactly as it does today;
- no origin bucket is written;
- absolute links are still built from the local request.

Operator sequence (ops doc §1.4–§1.7), each step independently revertable:

1. HAProxy: `http-request set-header X-Forwarded-For %[src]` (replaces, so a client value cannot survive).
2. App, one node: `server.forward-headers-strategy=native` + `qbic.auth-throttle.login.origin.enabled=true`.
3. Verify the §1.7 checklist, then roll to the other nodes.

If the header is missing or the peer is not a trusted proxy, the resolver refuses rather than
falling back to the proxy address — so a half-finished rollout degrades to account scope, never to
a global bucket. That is the issue's "blocking check", implemented as a code path rather than as a
step in a runbook.

## 5. Acceptance criteria → evidence

| Criterion | Where it is asserted |
|---|---|
| Repeated attempts from one origin against many accounts → that origin is throttled | `AuthAttemptOriginThrottleSpec` "an origin flooding many accounts is throttled, and stops being able to probe at all"; end-to-end through the filter in `LoginThrottleWiringSpec` "an origin that floods many accounts is rejected before any password work" |
| …without affecting other origins | same spec, "a throttled origin does not throttle a different origin" |
| Victim is not blocked when logging in from a different origin | same spec, "a clean origin is NOT blocked by another party's damage"; through the filter in `LoginThrottleWiringSpec` "a clean origin is not rejected because another party filled the account bucket" |
| Client-supplied `X-Forwarded-For` never becomes a bucket key | `HttpClientOriginResolverSpec` — internal-proxy addresses and non-literals yield `unknown()`; `OriginTrustConfigurationSpec` refuses a strategy in which the client could write the key |
| Throttled response indistinguishable | `LoginThrottleWiringSpec` — same handler, same `BadCredentialsException`, same constant work, for the origin path as for the account path |
| Origin keys are hashed, never raw | `AuthAttemptOriginThrottleSpec` "the origin key is a hash, never the network in readable form" |
| IPv4/IPv6 cannot rotate freely | `IpNetworksSpec` — `/24` and `/64` grouping, mapped-v4 folding, dual-spelling stability |
| A success cannot reset an origin's tally | `AuthAttemptOriginThrottleSpec` "a success clears the account bucket but NEVER the origin bucket" |
| A misconfiguration fails startup instead of silently degrading | `OriginTrustConfigurationSpec` |
| Concurrent increments still exact | existing `AuthAttemptCounterConcurrencySpec` still runs the same upsert; origin buckets reuse it |
