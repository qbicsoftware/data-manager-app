# Council Decision — Placement of User Group Management and Org Groups

> **Status:** 🟢 Decision — recorded for PO review (no code changes in this document)
> **Date:** 2026-09-27
> **Type:** Advisor-council (parent-supervised) decision record on information architecture /
> UI placement only. No backend, ACL, or role-model changes are implied.
> **Related:** [`docs/user-groups-strategy.md`](../user-groups-strategy.md),
> [`docs/plans/FEAT-USER-GROUPS-10-my-groups-view.md`](../plans/FEAT-USER-GROUPS-10-my-groups-view.md)

---

## 1. Question and scope

> User group management is currently hooked into the Settings hub (`SettingsMainLayout`;
> `settings/groups`, `settings/groups/new`, `settings/groups/:groupId`), and org groups
> (admin-managed groups, e.g. NGS labs) will be hooked there too. Is the Settings location the
> right place from a user perspective, and does it allow good cross-linking within the app?

**Scope:** information architecture / UX placement of the user-facing group surface and the
planned admin org-group surface. No backend, ACL, or role-model recommendations beyond what is
needed to ground placement.

## 2. Council roster and process

| Advisor | Lens | Context | Pass 1 run | Pass 2 run |
|---|---|---|---|---|
| `council-architecture` | DDD bounded-context placement, module structure, IA, long-term maintainability | fresh (profile default) | `2aea1fe5-23e2-4835-b822-e177595a72b3` | `4fb8e65c-cd33-4fd8-a8fa-5236f2db194a` |
| `council-product` | Product, UX, governance, operational rollout | fresh (profile default) | `321457bd-cc07-4790-96d1-2c12e0805868` | `a127697d-5c55-45ca-abb1-cc24698a6fa6` |
| `council-security` | Security, authorization, access-control lens | fresh (profile default) | `edfc1499-44aa-49e6-af68-9902fb1291d6` | `5a945c62-e527-455a-b32c-ae0bddce4baf` |

- **Passes:** 2 (independent reports → one cross-exam). Convergence reached after Pass 2; no
  material dispute remained that both affected the recommendation and could be settled by
  advisor evidence.
- **Fallbacks:** none required (all three `council-*` profiles available and resumable).
- **Method note:** Pass 2 had to be relaunched once because the initial attempt passed both
  `resume` and `agent` to `runs.all`, which the runtime rejects as mutually exclusive. No advisor
  work was lost; the retained Pass 1 children were resumed cleanly.

## 3. Recommendation (converged)

### 3.1 User-facing groups — Settings is defensible short-term, wrong long-term; extract the whole surface, not a remnant

The Settings hub is personal-account chrome: navbar "Settings", `AccountOverviewHeader`
"Your account settings", and tabs Profile / **My Groups** / API Tokens / External Providers.
Groups are collaborative, access-granting resources (member rosters, roles, dissolve; the
leave-group flow revokes effective access to every project the group is shared with). All three
advisors converge on **full extraction** of the group mini-area into a dedicated top-level
**Groups** area:

- Routes: `groups`, `groups/new`, `groups/:groupId` under their own `GroupsMainLayout` (own
  `RouterLayout`), reachable from primary nav / home and the avatar menu.
- `settings/groups* → groups*` redirects via the existing `ForwardingView` precedent
  (`SettingsLandingRedirect`, `LegacyProfileRedirect`, `LegacyPersonalAccessTokenRedirect`).

The read-only-"My Groups"-remnant hybrid was **rejected**: architecture called it "worse than
either extreme" (couples the tab *and* balkanizes the surface); security retracted it ("buys
nothing security-wise, retains both costs: the `beforeEnter` tab-special-case and the two-hub
concept split"); product also moved to full extraction.

### 3.2 Admin org-group management — never in the personal Settings hub

Role-gated, visually distinct surface. The push for the app's *first* dedicated `/admin/groups`
area was folded into the lighter option (no broader admin console exists in the codebase — no
admin views/nav/controllers; announcements are DB-seeded read-only; offers are project-scoped
uploads):

1. **Reserve the `/admin/*` URL namespace now** (near-zero cost pre-release).
2. Render org-group governance as a visually distinct **ROLE_ADMIN-gated section of the
   top-level Groups area** (consistent with existing per-view role-gating precedents such as
   `isOfferSearchAllowed`).
3. Promote to a dedicated admin area only when the PO can name 2+ further admin surfaces
   (announcement authoring, user management).

Security rationale for keeping admin out of the shared hub: a dedicated area admits **one
enforceable, testable invariant** (a layout-level `BeforeEnter` guard or base class covering
every descendant route), whereas a hub section requires per-view gates plus render-gated nav
tabs — doubling the misconfiguration surface. Honest caveat: placement is never the security
boundary; `GroupService` / `GroupManagementServiceImpl` are. The gate must exist at the
application layer regardless.

### 3.3 Cross-linking — do it regardless of placement

- **Project Access → Groups grid → group detail** (member-aware)
- **Group detail → "Projects shared with this group"** (makes the blast radius visible before
  dissolve; today the dissolve UI warns generically without listing projects)
- **Project-card group-name chips → group detail** (group names are public via
  `listPublicDirectory()` — leaks nothing)
- **Profile / avatar ↔ groups**

Leak-free baseline: preserve `GroupDetailMain`'s NotFound reroute for **non-members**; first
delivery routes **members** to group detail and gives non-members **no link** (the
DPA/visibility policy §1a requires only that member lists/counts never reach non-members — it
does not require a non-member-readable detail view). A read-only non-member view is **not
release-blocking**; if added later for project access-administration holders, it must be an
application-layer visibility rule (per strategy §5.1), never a UI loosening.

### 3.4 Timing — move now only if the destination is committed this sprint

The one genuine residual dispute (move now vs. sequence with the admin rollout) was settled by
product's refinement, which all three accepted:

- The group views are **merged to `development` but not yet released** (HEAD pom `1.17.0`;
  `@since 1.19.0` My Groups / `@since 1.20.0` group detail; commits landed in #1580/#1581).
  Moving in the pre-release window = **zero redirect debt**.
- ⚖️ **Decision rule:** move **now** *if and only if* the top-level Groups destination is
  committed this sprint; otherwise keep-and-sequence (move both surfaces together with the admin
  rollout) — this avoids a second move and re-wiring twice. The cost asymmetry is small either
  way because the `ForwardingView` redirect precedent exists.

## 4. Accepted / rejected feedback

**Accepted**
- Full extraction over the remnant hybrid (all three).
- `NotFound`-for-all non-members as the leak-free baseline; member-only cross-links.
- "Already shipped" is weak inertia — the views are weeks old and unreleased.
- Reserve the `/admin/*` URL namespace.

**Rejected / refined**
- "Move-now is required" — sequencing is acceptable; move-now is cheaper **only** because
  unreleased.
- "The `beforeEnter` tab-special-case is a smell" — it is the standard Vaadin sub-route-highlight
  pattern and disappears automatically under extraction.
- **Self-retraction (architecture):** the move is **not** a requirements change (introduces no
  new capability) → **no `docs/requirements.md` PR**; it *is* a scope change to story #1568's
  acceptance criteria → PO sign-off + GitHub issue / external-stakeholder-doc update per the
  story lifecycle.

## 5. Owner decisions (not evidence-settlable)

1. **Commit the top-level Groups destination this sprint** — the linchpin for move-now.
2. Whether a broader admin console is planned → decides role-gated section vs. dedicated
   `/admin` area.
3. Whether to extend member-list visibility to project access-administration holders
   (strategy §5.1 says yes; the application layer does not implement it today).
4. Evidence-free audience judgment: decide deliberately (groups primary/occasional?) and record
   the assumption — no navigation analytics exist and none are planned pre-rollout.
5. Route-stability policy for `/settings/groups*` in notifications (revocation/dissolution
   emails): avoid emitting URLs you plan to move, or ship redirects.
6. `/settings/groups*` redirect retention: indefinite vs. drop after a release cycle.

## 6. Risks

- Full extraction before the destination is ready strands the area without a natural top-level
  home (product).
- Moving later multiplies re-wiring cost across inbound links, admin flows, shared URLs, and
  tests (security, architecture).
- Cross-links that are not permission-aware could leak member rosters; the application layer
  must keep the gates.
- Deciding without analytics is evidence-free; record the assumption so it is revisited when
  telemetry exists.

## 7. Confidence

High on all code-level claims (each advisor verified layout composition, route registration,
project-access group grid without links, member-gated detail, application-layer authorization
gates, existing redirect precedent, release state directly in the repository). Medium where the
recommendation depends on the not-yet-committed destination and the evidence-free audience
judgment.

## 8. Evidence

Advisor sources (each verified in-repo): `SettingsMainLayout`, `SettingsNavigationComponent`,
`MyGroupsMain`, `NewGroupMain`, `GroupDetailMain`, `AppRoutes.GroupsRoutes`, `DataManagerMenu`,
`ProjectAccessComponent`, `AddCollaboratorToProjectDialog`, `SettingsLandingRedirect` /
`LegacyProfileRedirect` / `LegacyPersonalAccessTokenRedirect` / `ForwardingView`,
`user-groups` application-layer services, `docs/user-groups-strategy.md`, 
`docs/plans/FEAT-USER-GROUPS-10-my-groups-view.md`, `docs/features.md`, `pom.xml`.

---

## 9. Handover — proposed refactor (candidate scope for a worker)

> The council decision itself does **not** authorise implementation — per §5 the PO must decide
> the destination and timing first. The following is the concrete shape should the extraction be
> approved. It is recorded here so the decision and the handover stay traceable.

### 9.1 New top-level Groups area (if approved)

- **New layout** `views/groups/GroupsMainLayout.java` (extend `DataManagerLayout`, mirror
  `UserMainLayout`'s navbar pattern minus the project-title link; own vertical nav with
  "My Groups" highlight across sub-routes).
- **Route moves** (update `AppRoutes.GroupsRoutes`): `MY_GROUPS = "groups"`,
  `NEW_GROUP = "groups/new"`, `GROUP_DETAIL = "groups/:groupId"`; move `MyGroupsMain`,
  `NewGroupMain`, `GroupDetailMain`, `MyGroupsComponent`, `GroupMembersComponent`,
  `NewGroupForm` from `views/settings/` to `views/groups/` (update packages + imports +
  tests: `MyGroupsComponentSpec`, `GroupMembersComponentSpec`, `NewGroupFormSpec`).
- **Nav entry:** add "Groups" to the avatar menu (`DataManagerMenu` submenu: "Settings",
  "My Groups" → `MyGroupsMain`, "Log Out") and/or primary nav.
- **Legacy redirects:** new `@Route("settings/groups")` /
  `@Route("settings/groups/new")` `ForwardingView`s → new targets.
- **Drop** the `beforeEnter` special-case in `SettingsMainLayout` mapping
  `NewGroupMain`/`GroupDetailMain` → `MyGroupsMain` (only `My Groups` tab stays until the tab is
  removed).

### 9.2 Org-groups admin surface (separate, later)

- Reserve `/admin/*`; role-gated ROLE_ADMIN section (or dedicated area if PO confirms a broader
  console). Do **not** place in the personal Settings hub.

### 9.3 Cross-linking (do regardless of placement — separate stories)

- Access-page group rows → group detail (member-aware); group detail → "projects shared with
  this group"; project-card chips → group detail; profile/avatar ↔ groups.
- Keep the non-member NotFound default.