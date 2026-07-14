# 0004 — FAIR Signposting integration deferred

* Status: approved
* Deciders: project team (interviewed via [`interview-feat-dataset-connection.md`](interview-feat-dataset-connection.md))
* Date: 2026-07-13

Technical Story: [Connect associated InvenioRDM datasets with Data Manager projects](https://github.com/qbicsoftware/data-manager-app/issues/1466) (FEAT-DATASET-CONNECTION)

## Context and Problem Statement

The feature originally envisioned integration with published InvenioRDM datasets via
**FAIR Signposting** — an HTTP-header-based discovery mechanism where resources advertise
machine-readable metadata links via RFC 8288 `Link:` headers (e.g.,
`Link: <doi:...>; rel="describedby"`). FAIR Signposting would have let Data Manager follow
a signpost to discover structured metadata without calling the InvenioRDM API directly.

An upstream bug in InvenioRDM's FAIR Signposting serialization has been reported. The fix
is slated for a future major release. Adopting FAIR Signposting today would mean either:

(a) Implementing around the bug (fragile, incorrect behaviour for signposted metadata), or
(b) Delaying the feature until the upstream fix lands, which is on an unknown timeline
    that is likely more than 1-2 years out.

The team must decide whether to (i) proceed now with a conventional InvenioRDM REST API
integration, (ii) wait for the upstream fix, or (iii) pursue some hybrid path.

## Decision Drivers

* The upstream bug blocks FAIR Signposting-based integration today.
* The fix's timeline is ~1-2 years out, making a hold-the-feature strategy unacceptable.
* FAIR Signposting, when correctly implemented, is a metadata-discovery mechanism — it
  does not replace the need to fetch actual record data. It is a companion to (not a
  replacement for) the REST API.
* Data Manager's InvenioRDM integration, designed via
  [ADR-0002](0002-invenio-rdm-api-client-credentials.md), uses a source-agnostic
  `DatasetSource` port that can naturally accommodate a future `InvenioRdmSignpostingSource`
  adapter alongside or in place of the REST adapter.
* There is no current business requirement to surface signposted metadata through a
  different channel than the REST API; the REST API provides all the record data we need.

## Considered Options

* [A] Implement the feature using FAIR Signposting today — infeasible because of the bug.
* [B] Hold the entire feature until the upstream fix is released (timeline: 1-2+ years).
* [C] Implement the feature using the InvenioRDM REST OpenAPI now; defer FAIR Signposting
  as a future enhancement that can be picked up when the upstream fix is stable.

## Decision Outcome

**Chosen option: C.**

The feature proceeds against the conventional InvenioRDM REST OpenAPI, as documented in
[ADR-0002](0002-invenio-rdm-api-client-credentials.md). Integration with FAIR Signposting
is **explicitly deferred**, not abandoned.

### Positive Consequences

* The feature can ship on the timeline stakeholders need (now), not the upstream bug-fix
  timeline.
* The source-agnostic `DatasetSource` port design in [ADR-0002](0002-invenio-rdm-api-client-credentials.md)
  already leaves room for a future `InvenioRdmSignpostingSource` adapter. When the upstream
  fix lands, adding the new adapter is a localized change; the aggregate, storage, and
  lifecycle semantics defined in [ADR-0001](0001-associated-datasets-domain-model.md) and
  [ADR-0003](0003-connection-lifecycle-stewardship.md) do not change.
* The aggregate shape, DB schema, sealed metadata hierarchy, connection lifecycle,
  credential security, and notification model are all independent of the integration
  transport. Replacing or complementing the REST client with a Signposting client is an
  infrastructure-layer change, not an aggregate-layer change.

### Negative Consequences

* The project does not use FAIR Signposting today, which means Data Manager does not model
  the discovery mechanism that the FAIR ecosystem is converging toward. When Signposting
  is revisited, the team must re-acquire context on how to integrate it (mitigated by the
  existence of this ADR, which records the "why now, why not yet" rationale).
* If the upstream bug takes longer to fix than expected (>2 years), the deferral
  decision remains valid but may need a formal revisit — the horizon in this ADR is
  explicit.

## Revisit Criteria

This ADR should be revisited (and possibly superseded) when:

1. InvenioRDM ships a stable major release with a working FAIR Signposting
   serialization bug fix.
2. A new user story explicitly calls for Data Manager to support FAIR Signposting
   discovery (e.g., a story requiring metadata to follow linked-data chains across
   platforms).

Until either criterion is met, the REST-based integration in
[ADR-0002](0002-invenio-rdm-api-client-credentials.md) remains the chosen path.

## Links

* Deferment of: the originally intended integration mechanism for
  [ADR-0002](0002-invenio-rdm-api-client-credentials.md).
* Refinable by a future ADR when Signposting becomes the primary integration (likely
  to supersede part of ADR-0002, not ADRs 0001 or 0003).
* Depends on [ADR-0001](0001-associated-datasets-domain-model.md) — the aggregate and
  storage shape are independent of whether the integration is REST or Signposting.
