# Implementation Plan — FEAT-USER-GROUPS-08 (#1566): View and Manage Groups on the Project Access Page

**Status:** 🔵 Planned (ready for review)
**Parent story:** [#1566](https://github.com/qbicsoftware/data-manager-app/issues/1566) — `FEAT-USER-GROUPS-08: View and Manage Groups on the Project Access Page`
**Parent feature:** [#1558](https://github.com/qbicsoftware/data-manager-app/issues/1558) — `FEAT-USER-GROUPS` (EPIC 3 — Sharing onto projects & effective access)
**Strategy / architecture source:** [`docs/user-groups-strategy.md`](../user-groups-strategy.md) — §4.3 (ACL plumbing), §4.5 (UI surface), §4.6 (revocation NFR ≤60s), §5.1 (leakage guard)
**Predecessor (already merged):** `FEAT-USER-GROUPS-06` (#1564, PRs #1585/#1586 — share a group onto a project; read-only Groups section; ACL write/read path)
**Branch:** `development` (HEAD `98380cd79`)

---

## 0. Delta-first reality check — most of this story's backbone already exists

Story 08 is **not** a from-scratch surface. Story 06 intentionally deferred exactly two things to this
story (06 plan §2 out-of-scope: *"Role edit / revoke UI for groups (story 08)"* and *"Membership
visibility / member counts (story 08 / #1566 and story 14 / #1572)"*). The per-AC gap analysis against
`development` HEAD:

| AC | Requirement | Exists today? | Gap for this story |
|---|---|---|---|
| **AC1** | ADMIN sees People section (unchanged) + Groups section (name/description/role) | ✅ **Done** — `ProjectAccessComponent.createProjectGroupGrid()` (read-only grid: name, description, role) + `listSharedGroups()` (README-gated, returns `SharedProjectGroup(groupId, groupName, groupDescription, projectId, projectRole)`); People grid untouched | **Verify only.** No code change required for the listing itself |
| **AC2** | Change group role (READ/WRITE/ADMIN) or revoke; revocation effective at next authorization check (≤60s NFR) | ⚠️ **Backend done** — `changeAuthorityAccess(projectId, authority, role)` + `removeAuthorityAccess(projectId, authority)` exist, `@PreAuthorize(ADMINISTRATION)`-gated, OWNER-guarded, delete-stale+insert-new semantics. **UI missing** — no per-row role/revoke affordances; group grid is read-only. **≤60s cross-node NFR NOT met** — only local `evictCachedAcl()` (D5 from 06); other instances keep a stale `acl_cache` entry for up to the 600s TTL (`ehcache3.xml`); no JMS/Artemis broadcast exists | **Main work of this story** — per-row role editor + revoke (ADMIN-only) **and** the broadcast eviction mechanism the 06 plan explicitly deferred (PR #1585 body: *"cross-instance broadcast eviction tracked as a story-08 follow-up"*) |
| **AC3** | Non-member expands a shared group → sees name/description only, no member list or counts | ✅ **Structurally satisfied** — `SharedProjectGroup`/`GroupInfo` carry **no** membership data; `GroupInformationService` exposes no cross-user membership (`listMyGroups` is caller-scoped only); the access page never invokes a membership API | **Decision: no expansion surface.** See D2; guard tests prove no membership API is ever called from the access path |
| **AC4** | Plain-READ user sees groups as name/description only; surface not widened beyond the READ-graded collaborator list | ✅ **Structurally satisfied** — `listSharedGroups` is `@PreAuthorize(READ)`; the page route gate (`ProjectAccessMain.beforeEnter`) is *stricter* (`changeProjectAccess` = READ && ADMINISTRATION → READ-only users get `NotFoundException` today) | **Defense-in-depth only.** Keep the group surface READ-graded in the service and UI so a future READ-opened page cannot leak; see §5.3 |

**Net:** this story delivers (a) the group **role-edit/revoke UI**, (b) the **cross-instance
`acl_cache` eviction broadcast** (the 06-deferred half of D5, required by the ≤60s NFR), and (c)
**guard tests** for the AC3/AC4 leakage invariants. Everything else is verification.

---

## 1. Governance & traceability

- ✅ **Requirements registry:** `docs/requirements.md` contains **zero** `GROUP-*` entries; the PO-owned
  dedicated requirements PR is pending (feature #1558 governance note). Precedent is set and
  confirmed by the merged story-06 PR (#1585): **implementation proceeds** per PO direction
  (*PO-approved 2026-09-22, feature #1558 tracking*; PR #1574/#1581/#1585 carried the same
  justification). This story follows suit:
  - ❌ **Do not** modify `docs/requirements.md` in this PR.
  - ✅ PR body carries the **PO-direction justification block** (mirror #1585 wording): the `GROUP-*`
    IDs are draft proposals pending the dedicated requirements PR; the PO directed implementation to
    proceed for this feature; formal entries are added by the PO in a separate docs PR.
  - ✅ PR references requirement IDs **`GROUP-R-08`, `GROUP-R-09`, `GROUP-R-06`, `GROUP-NFR-01`**.
- ✅ **PR semantics:** `Related to #1566` — **NOT** `Closes #1566` (story stays open until EPIC-3
  siblings land; exact precedent: PR #1585 used *"Related to #1564"*). Title/body reference the
  **stable story id `FEAT-USER-GROUPS-08`**, never the issue number.
- ✅ **`docs/features.md` / external story file:** untouched (precedent: 06; the referenced
  `docs/features/FEAT-USER-GROUPS-stories.md` does not exist in-repo).
- ⚠️ **ADR:** the `GrantedAuthoritySid` + retrieval-strategy approach and the **broadcast eviction
  mechanism (D4 below)** are recorded in this PR's description; ADR creation itself requires human
  approval (AGENTS.md §12) and stays a follow-up docs PR (precedent: 06).

---

## 2. Goal & scope

Deliver per AC:

- **AC1 — verify only:** the existing Groups section already lists shared groups with name,
  description and grant role for project ADMIN. Add regression coverage only if gaps are found
  (06 specs already cover the rendering; extend for the new action affordances, §5.2).
- **AC2 — new:** per-row **role change** (READ/WRITE/ADMIN, never OWNER) and **revoke** for groups
  in `ProjectAccessComponent`, wired to the existing `changeAuthorityAccess` /
  `removeAuthorityAccess` service methods (ADMIN-gated at service + UI). Revocation/role-change is
  effective at the next permission check on the executing node (existing `GroupAwareSidRetrievalStrategy`
  + local eviction) **and** the new broadcast eviction makes it ≤60s on **all** instances (D4).
- **AC3 — no new surface + guard:** the Groups section carries **no member list and no member
  counts** — by construction (`SharedProjectGroup`, `GroupInfo`, `GroupInformationService` expose
  none) and by guard tests (§7). Per-story-14 deferral, no member-roster UI is built here.
- **AC4 — defense-in-depth + guard:** the group surface stays **READ-graded** at the service
  (`listSharedGroups` `@PreAuthorize(READ)`) and the UI never renders beyond name/description/role;
  edit/revoke affordances are ADMIN-gated (`userPermissions.changeProjectAccess`), even though the
  current route gate already prevents READ-only users from opening the page.

**Explicitly OUT of scope (guardrails, trace where the work lives):**

- ❌ **Membership roster / member counts UI** — story 14 (#1572) + strategy §4.5 (members visible
  only to members/ADMIN/QBiC-admin). Do **not** call any membership API from the access page.
- ❌ **Notifications / emails** (EPIC 4) — story 09 (#1567). AC2's "takes effect at the next
  authorization check" is a **security-semantics** requirement, not an email directive. The current
  `removeAuthorityAccess`/`changeAuthorityAccess` fire **no** domain events and this story adds
  none; revocation emails belong to EPIC 4. Authoritative: strategy §4.4 + feature #1558 note
  (*"revocation and dissolution emails"* live in EPIC 4's story 09).
- ❌ **Effective-access query engine / composition** (maximal of direct + group + system role) —
  story 07 (#1565).
- ❌ **Member-removal-from-group semantics, orphaned-ACE/_membership_ cleanup, dissolution
  ACE/group_membership cleanup** — story 13 (#1571, EPIC 6). Note the overlap: #1571 also covers
  *"group grant revoked from a project → ≤60s"* — the **broadcast mechanism planned here is shared
  infra**; #1571 consumes it, it must not be duplicated (see §6.4 handoff note).
- ❌ **Project-card group display / SQL-view rework** (`project_userinfo`, `project_overview` are
  principal-only) — story 12; recorded follow-up from 06.
- ❌ **Quick-share from the project overview** — 06 plan §8a iteration 2 (reuses the dialog; separate
  PR, stays open).
- ❌ `docs/requirements.md` edits, new ADR creation, story renumbering.

---

## 3. Architecture & key decisions

| # | Decision | Choice | Why / evidence |
|---|---|---|---|
| D1 | SID construction on the UI write path | **`GroupSidProvider.GROUP_SID_PREFIX + groupId`** (i.e. `"GROUP_" + groupId`) in `ProjectAccessComponent`, never a raw/derived string from group name | Single-source rule (strategy §4.3 hidden-dep #7): the exact string that `GroupSidProviderImpl` emits and `listSharedGroups` parses. The existing share path already does exactly this (`onGroupSharedConfirmed`, L~397). A mismatch silently denies access → re-use the constant. |
| D2 | AC3/AC4 "expand a shared group" semantics | **No per-row expansion surface in this story.** The role column already communicates the grant; name/description are already visible; there is no member data to reveal; an expansion row would be empty for non-members and redundant for ADMINs | AC3/AC4 are *disclosure* constraints, not *features*. The flat read-only/action grid satisfies them with the smallest surface (zero leakage risk). A member-roster surface, if the PO wants one, is story 14 (#1572) + strategy §4.5. Verified desirable: 06's grid already renders name + description + role inline. |
| D3 | Role-edit UI mechanism | **Mirror the People grid's `Grid.Editor` pattern**: an **Action column** (per-row, ADMIN-gated) containing an **Edit** button opening the inline `Select<ProjectRole>` editor (READ/WRITE/ADMIN, no OWNER), and a **Remove** button opening `AlertDialog.danger(...)` confirmation → `removeAuthorityAccess` | Consistency with the existing, spec-covered People flow (`renderProjectRoleComponent`, `changeProjectAccessCell`, `AlertDialog.danger` precedent). The People grid proves the exact editor lifecycle (`editItem` → value-change → `save`/`closeEditor` → refresh). |
| D4 | ≤60s revocation NFR on multi-instance | **Broadcast eviction via a dedicated Artemis/JMS topic.** New application-level port `AclEvictionPublisher.publishAclEviction(ProjectId)` (project-management) + `project-management-infrastructure` implementation publishing an `IntegrationEvent` (`type="aclCacheEvicted"`, content `{"projectId": ...}`) via `JmsTemplate` (pub-sub is globally enabled: `spring.jms.pub-sub-domain=true`); `datamanager-app` gains a `@JmsListener` consumer that evicts `acl_cache` for the project `ObjectIdentity` on **every** instance. Local `evictCachedAcl` stays (belt-and-braces on the writer). | **The 06-deferred half of D5** (PR #1585 body + strategy §4.6): `acl_cache` is process-local EHCache, TTL 600s, and `updateAcl` evicts only the writer. Without broadcast, other instances honor a revoked group grant for up to **600s > 60s NFR**. Precedent for JMS producer/consumer: identity's `MessageDispatcher` + `project-management-infrastructure`'s `MessageConsumer` (which already has `spring-jms`). Mechanism must be asserted by an IT (≤60s window). See §6.4 for the #1571 handoff. |
| D5 | Where the eviction listener lives | Consumer in **`datamanager-app/.../security/`** (it owns the `AclCache` bean defined in `AclSecurityConfiguration`); publisher port in `project-management` application layer, impl in `project-management-infrastructure` | The `AclCache` bean is defined in `datamanager-app`; `ProjectAccessServiceImpl` must not depend on JMS (application-layer purity — the port keeps it clean). `datamanager-app` is the only deployable composition root, so every instance runs the listener. |

> **Cache-read hygiene (carried over from 06):** never pass real SID filters to `readAclById` in the
> group paths — the read is full-set under Spring's "no SID filtering" contract; a manually cached
> filtered `Acl` trips the `Error: SID-filtered element detected…` assertion. `listSharedGroups`
> already reads with `null` sids — keep it that way.

---

## 4. File changes

### 4.1 CREATE

| Path | Responsibility |
|---|---|
| `project-management/src/main/java/life/qbic/projectmanagement/application/authorization/acl/AclEvictionPublisher.java` | Port (application layer): `void publishAclEviction(ProjectId projectId)`. No Spring/JMS types in the signature. |
| `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/communication/AclEvictionMessagePublisher.java` | `@Component implements AclEvictionPublisher`; `JmsTemplate.convertAndSend(topic, ObjectMapper.writeValueAsString(IntegrationEvent.create("aclCacheEvicted", Map.of("projectId", id))))`; topic via `@Value("${qbic.broadcasting.acl-eviction.topic}")` (mirror `MessageDispatcher`, which already injects a topic the same way). |
| `datamanager-app/src/main/java/life/qbic/datamanager/security/AclCacheEvictionListener.java` | `@Component` with `@JmsListener(destination = "${qbic.broadcasting.acl-eviction.topic}")`; parse JSON → `IntegrationEvent` → verify type `aclCacheEvicted` → `aclCache.evictFromCache(new ObjectIdentityImpl(Project.class, projectId))`. Injects the existing `AclCache` bean (defined in `AclSecurityConfiguration.aclCache(...)`). Parse errors logged, not thrown (do not break the consumer on an unrelated message). |
| `project-management/src/test/groovy/life/qbic/projectmanagement/application/authorization/acl/AclEvictionPublisherSpec.groovy` | Spock: publisher serializes `IntegrationEvent` with type `aclCacheEvicted` + projectId content over a mocked `JmsTemplate`/topic (constructor-injectable seam). |
| `datamanager-app/src/test/groovy/life/qbic/datamanager/security/AclCacheEvictionListenerSpec.groovy` | Spock: listener parses the event and evicts the project OID from a mock `AclCache`; ignores unknown event types; tolerant of malformed JSON. |
| `datamanager-app/src/test/groovy/life/qbic/datamanager/security/AclEvictionPropagationIT.groovy` (or `**/*IT.groovy`, `it` profile) | **NFR proof:** publish via the real `AclEvictionPublisher` (or `JmsTemplate`) with a running broker and assert the `@JmsListener` consumer evicts the ACL for the target project — wall-clock asserted **< 60s** (expected ≪1s). Uses the `it` profile's started app + Artemis env (see §7). |
| CSS (extend `datamanager-app/frontend/themes/datamanager/components/page-area.css`) | `.project-access-component .project-group-role-select` (reuse `.project-role-select`-style layout) — or reuse the existing classes and only adjust the action cell via the already-present `.change-project-access-cell`. |

### 4.2 MODIFY

| Path | Change |
|---|---|
| `project-management/src/main/java/life/qbic/projectmanagement/application/authorization/acl/ProjectAccessServiceImpl.java` | **(a)** Inject an **optional** `AclEvictionPublisher` (constructor param or `ObjectProvider` so existing `ProjectAccessServiceSpec` mocks keep working — prefer a third constructor arg defaulting to a no-op via overload, matching the `aclCache` `@Autowired(required=false)` precedent). **(b)** In `addAuthorityAccess` (~L307), `removeAuthorityAccess` (~L327) and `changeAuthorityAccess` (~L371): after the existing `evictCachedAcl(projectId)`, call `publisher.publishAclEviction(projectId)`. **(c)** **No other backend change**: OWNER guard, duplicate check, delete-stale+insert-new role semantics, error messages all verified present. |
| `project-management/src/main/java/life/qbic/projectmanagement/application/authorization/acl/ProjectAccessService.java` | **No change.** `changeAuthorityAccess` / `removeAuthorityAccess` / `listSharedGroups` / `SharedProjectGroup` already exist and are correct for AC2/AC1. |
| `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/access/ProjectAccessComponent.java` | **The main UI work.** In `createProjectGroupGrid()` (~L337): add an **Action column** via `addComponentColumn` (key `"actions"`) rendering — only when `userPermissions.changeProjectAccess(context.projectId().orElseThrow())` — an **Edit** button (mirror People grid: guard against `!changeProjectAccess` inside the click handler too, `displayError(INVALID_ROLE_EDIT, ...)` on violation; opens the group-grid editor) and a **Remove** button (mirror People grid: `AlertDialog.danger(this, "Remove group from project", "Are you sure you want to remove the group <name> from the project?", "Remove group", "Keep group", () -> revokeGroup(projectGroup))`). Add editor plumbing to `projectGroupGrid` (create `Editor<ProjectGroup>` + `Binder`, register the role editor component offering `READ`/`WRITE`/`ADMIN` — **no OWNER**). Add `changeGroupRole(ProjectGroup, ProjectRole)` → `projectAccessService.changeAuthorityAccess(projectId, GroupSidProvider.GROUP_SID_PREFIX + groupId, role)` and `revokeGroup(ProjectGroup)` → `projectAccessService.removeAuthorityAccess(projectId, GroupSidProvider.GROUP_SID_PREFIX + groupId)`; on `ApplicationException` → `displayError("Invalid group access change", <message>)` (mirror `onGroupSharedConfirmed`); always `refreshProjectGroupGrid()` (and refresh users — a READ-only group role change does not affect the People grid, only refresh groups). |
| `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/access/AddCollaboratorToProjectDialog.java` | **No change** — already supports group selection + role w/ no OWNER + already-shared filtering (06). The role-edit/revoke UI lives on the component, not the dialog. |
| `datamanager-app/src/main/resources/application.properties` | Add under the Artemis block: `qbic.broadcasting.acl-eviction.topic=ProjectAccessAclEvictions` (topic name is a string; keep it distinct from `qbic.broadcasting.identity.topic=User`). |
| `datamanager-app/src/test/groovy/life/qbic/datamanager/views/projects/project/access/ProjectAccessComponentSpec.groovy` | **Update + extend.** The 06 spec asserts `!columnKeys.contains("action")` for the ADMIN case — for 08 the ADMIN case now expects an action column. Add: (1) ADMIN sees Edit/Remove for a shared group; (2) Edit fires `changeAuthorityAccess(projectId, "GROUP_<id>", newRole)` and refreshes; (3) Remove fires `removeAuthorityAccess(projectId, "GROUP_<id>")` after confirm; (4) `changeProjectAccess=false` renders an **empty** action cell (no Edit/Remove) — the AC4 gating test; (5) **leakage guard:** `0 * groupInformationService.listMyGroups(_)` and `0 * groupInformationService.*` beyond `findGroupById`/`listPublicDirectory` when rendering the Groups section (AC3/AC4). |
| `project-management/src/test/groovy/life/qbic/projectmanagement/application/authorization/acl/ProjectAccessServiceSpec.groovy` | Extend with: `changeAuthorityAccess` role-change semantics (READ→ADMIN deletes stale + inserts new); `removeAuthorityAccess` removes the group's ACEs; `removeAuthorityAccess`/`changeAuthorityAccess` are no-ops (no throw) for a non-shared group (stale-UI race); `changeAuthorityAccess(..., OWNER)` rejected (existing test already covers the grant path — add the remove-path guard note); eviction publisher invoked after each write (mock publisher). |

---

## 5. Key design details

### 5.1 AC2 — role change & revoke from the Groups section

- **SID string:** always `GroupSidProvider.GROUP_SID_PREFIX + groupId` (D1). This is the identical
  string `listSharedGroups` parses back and `GroupAwareSidRetrievalStrategy` produces for the
  affected members ⇒ role change/revoke is effective at the **next** `hasPermission(...)` on the
  writer instance via the retrieval strategy (no session invalidation), and on **other** instances
  via the broadcast eviction (D4).
- **Role options:** READ / WRITE / ADMIN only — OWNER is not offered in the UI **and**
  `rejectAuthorityOwnership` already throws at the service boundary for any non-principal SID
  (defense-in-depth, verified `ProjectAccessServiceImpl:119-137` + existing spec).
- **Semantics verified:** `changeAuthorityAccess` deletes no-longer-valid permission ACEs and
  inserts only the additional ones (read → admin keep-common-files correctly); `removeAuthorityAccess`
  deletes all matching `GROUP_<id>` ACEs. Both are `@PreAuthorize(ADMINISTRATION)`. `SharedProjectGroup`
  carries `groupId` + `projectRole` — exactly what the editor needs, no extra lookup.
- **Error paths:** duplicate/stale-race (`ApplicationException` from a concurrent change) →
  `displayError` + refresh; non-shared-group revoke (race) → `removeAuthorityAccess` is a no-op,
  refresh clears the row. No exception surface.
- **Effective access:** after a group role downgrade, affected members' effective access shrinks to
  the maximal of their *remaining* grants at the next check — that maximal-composition is story 07,
  but the *safety* (no one retains a revoked group role) is guaranteed here by the retrieval strategy
  derivation; story 07 only adds the observability/query layer.

### 5.2 AC1 — verify-only, plus spec updates

`createProjectGroupGrid` already renders group name, description (ellipsis, via
`setPartNameGenerator` + `.group-description-row` CSS) and `Role: <label>`. No listing changes.
The only ripple: the 06 `ProjectAccessComponentSpec` case asserting the *absence* of an action
column must flip to assert its *presence* for ADMIN (and absence without the ADMIN gate).

### 5.3 AC3/AC4 — leakage invariants (guard tests)

- The access page's group surface may only use `SharedProjectGroup` data + `GroupInformationService.findGroupById`
  (already the case in `listSharedGroups`). `GroupInfo` exposes id/name/description/type **only**;
  `GroupInformationService.listMyGroups` is caller-scoped; no cross-user membership accessor exists
  on the public facade — membership cannot leak.
- **Guards:** spec assertions that rendering the Groups section never invokes `listMyGroups` /
  `listPublicDirectory` (only `findGroupById` through the service) — `0 * groupInformationService.*`
  beyond allowed calls (AC3/AC4). `listSharedGroups` READ-gate stays; the route's ADMIN gate is a
  *stricter* over-gate (fine — defense-in-depth, documented).

### 5.4 D4 — broadcast eviction mechanics (≤60s NFR)

- **Write:** `ProjectAccessServiceImpl` (all three authority writes) → `AclEvictionPublisher.publishAclEviction(projectId)`
  → `AclEvictionMessagePublisher` → `jmsTemplate.convertAndSend(topic, IntegrationEvent)`.
  `spring.jms.pub-sub-domain=true` makes the destination a topic (all instances receive a copy).
- **Read/consume:** `AclCacheEvictionListener` (`datamanager-app`) on the same topic → evict
  `ObjectIdentityImpl(Project.class, projectId)` from the injected `AclCache` bean. Idempotent,
  harmless on the writer (already evicted). `IntegrationEvent` parse mirrors `MessageConsumer`.
- **Topic config:** `qbic.broadcasting.acl-eviction.topic=ProjectAccessAclEvictions` (new property,
  distinct from the identity topic `User`).
- **NFR budget:** broker hop is sub-second on the LAN; the ≤60s budget is asserted end-to-end
  in `AclEvictionPropagationIT` (publish → consume → evict observed). Deployment multi-node
  propagation = fan-out of the pub-sub broker, covered by this mechanism-level proof; the 06-plan
  open question "single-node vs multi-node" is resolved by the `it`-profile test + this being the
  reviewer decision (see §7, open question 3).
- **⚠️ Handoff to story 13 (#1571):** #1571's AC "group grant revoked → ≤60s" consumes **this**
  mechanism; #1571 must not re-implement the broadcast. The only #1571-specific new work is
  member-removal/dissolution **cleanup** (orphaned ACEs/`group_membership` rows) — the ACL eviction
  itself is shared. Record in the PR body so the next story reuses it.

---

## 6. Test strategy

| Level | Spec / IT | Proves |
|---|---|---|
| Unit (Spock) | `ProjectAccessServiceSpec` (extended) | AC2 backend: role change delete-stale+insert-new; revoke removes ACEs; non-shared no-op; OWNER rejected on change path (already covered, keep); publisher invoked after each authority write |
| Unit (Spock) | `ProjectAccessComponentSpec` (updated+extended) | **AC1** rendering (existing, updated for action column); **AC2** UI: Edit fires `changeAuthorityAccess("GROUP_<id>", role)`, Remove fires `removeAuthorityAccess("GROUP_<id>")` after confirm; **AC4** `changeProjectAccess=false` → empty action cell, no add-control; **AC3/AC4 guard** `0 * groupInformationService.listMyGroups(*)` while rendering groups |
| Unit (Spock) | `AclEvictionPublisherSpec` (new) | D4: event type `aclCacheEvicted`, projectId content, sent to configured topic on the mock `JmsTemplate` |
| Unit (Spock) | `AclCacheEvictionListenerSpec` (new) | D4: parses event → evicts the project OID from mock `AclCache`; ignores unknown types; malformed JSON tolerated |
| Unit (Spock) | `AddCollaboratorToProjectDialogSpec` | Regression only — group share flow already covered (04-passing); no change expected |
| Integration (`it`) | `AclEvictionPropagationIT` (new) | **NFR:** publish → listener evicts within measured < 60s window (assert `System.nanoTime()` delta; assert eviction observable via `AclCache.getFromCache` or a spy) |

> Follow existing conventions: Spock `*Spec.groovy` matching `**/*Spec.class` (surefire), component
> specs headless without Spring context (`PinnedProjectsComponentSpec` / existing
> `ProjectAccessComponentSpec` precedent), `*IT.groovy` under the `it` profile (failsafe; app started by
> `spring-boot-maven-plugin`). No `@SpringBootTest` in unit specs.
>
> **Known environment caveat (pre-existing, not caused by this diff):** per PR #1585, the
> `project-management` Spock suite has a local Mockito "MockitoMockMaker could not be instantiated"
> issue that prevents some specs from running locally; they pass in CI. Keep specs CI-compatible
> (mock interfaces/classes via constructor injection, avoid static mocks) and report the same caveat
> in the PR body.

---

## 7. Suggested tasks (traceability)

- **T1 — AC2 UI:** `ProjectAccessComponent` group-grid editor + Action column (Edit/Remove,
  ADMIN-gated, OWNER excluded) + `changeGroupRole`/`revokeGroup` service calls + refresh + spec
  updates (new + flipped assertions).
- **T2 — D4 broadcast eviction:** port `AclEvictionPublisher`, `AclEvictionMessagePublisher`
  (with topic property), `AclCacheEvictionListener`, `application.properties` topic, wiring in
  `ProjectAccessServiceImpl` (optional publisher + call after the writes), unit specs, IT.
- **T3 — Leakage/regression hardening:** guard tests (`0 * listMyGroups(...)`), READ-gated
  no-action-cell test, dialog regression, full `mvn verify`, format.

---

## 8. Build & verify

```bash
./mvnw -pl project-management -am clean verify       # service role-change/revoke specs + publisher spec
./mvnw -pl datamanager-app -am clean verify          # UI component specs + listener/guard specs
./mvnw -pl datamanager-app -am verify -Pit           # AclEvictionPropagationIT (≤60s NFR) — requires broker env (ARTEMIS_MODE, ARTEMIS_BROKER_URL, ...)
./mvnw spring-boot:run -pl datamanager-app -Pdevelopment   # dev-mode smoke: change a group's role, revoke it, verify member access flips at next check (and second instance if running)
```

Format with the Google Java Style formatter (`GoogleStyle.xml` / project formatter) before
committing.

---

## 9. Commit sequencing (single PR, incremental)

1. **Writer hardening:** optional `AclEvictionPublisher` in `ProjectAccessServiceImpl` + publisher
   call on the three authority writes + `ProjectAccessServiceSpec` extension + `AclEvictionPublisherSpec`.
   Compile + verify.
2. **Broadcast receiver:** `AclCacheEvictionListener` + topic property + `AclCacheEvictionListenerSpec`.
   Verify.
3. **IT:** `AclEvictionPropagationIT` under `it` — assert ≤60s publish→evict. Runs separately via
   `-Pit`.
4. **UI (main delta):** group-grid editor + Action column + `changeGroupRole`/`revokeGroup` +
   `ProjectAccessComponentSpec` updates.
5. **Harden:** leakage-guard tests, regression sweep, format, full `verify`.

---

## 10. Risks & open questions (for the human reviewer)

1. **Broadcast mechanism placement decision [Reviewer decision]:**
   Implement the Artemis/JMS topic broadcast now (recommended — this story's AC2 carries the ≤60s NFR
   and the 06 PR explicitly deferred cross-instance propagation to a story-08 follow-up), or confirm a
   **single-node deployment** where eviction-on-write + next-check already meet the NFR (then D4 is
   deferred entirely, and story 13's NFR work still needs it — the mechanism stays on the critical
   path either way). **Recommendation: implement now.** Applies to the `it`-profile IT, which needs
   broker env vars.
2. **AC3/AC4 "expand" wording [Confirm]:** Plan reads AC3/AC4 as *disclosure constraints* and builds
   **no expansion surface** (flat grid satisfies them while minimizing surface). Confirm the PO does
   not expect an expandable row in this story — member-roster UI is story 14 (#1572).
3. **NFR test rigging [Confirm]:** The IT proves mechanism-level publish→consumer→evict ≤60s on one
   instance with a broker. A true two-node end-to-end assert is impractical in CI; confirm this
   level of evidence is accepted for the NFR (multi-node fan-out is broker-semantics, already proven
   by the pub-sub contract). Alternative: a second started instance in the same `it` run (heavier).
4. **Relationship to #1571 (EPIC 6):** the group-grant-revoke half of #1571's NFR is delivered by D4
   here; #1571 keeps member-removal/dissolution cleanup + revocation emails. Confirm no requirement
   on this story's PR to close work traced to #1571.
5. **`evictCachedAcl` redundancy:** `JdbcMutableAclService.updateAcl` may already evict on the writer
   via the configured `AclCache`; the explicit `evictCachedAcl` (06) is kept as belt-and-braces. No
   change.
6. **Role-change logging/audit:** role-level changes are audit-log-only per EPIC 4 definition (no
   emails). The service currently logs `debug`; consider an explicit audit log line in
   `changeAuthorityAccess`/`removeAuthorityAccess` (cheap, aligns with EPIC 4 "Role-level changes are
   audit-log-only"). **[Small, optional — include if cheap.]**
7. **`ObjectProvider` vs constructor param** for the optional publisher in
   `ProjectAccessServiceImpl`: prefer an `@Autowired(required=false)` setter (identical to the
   existing `aclCache` seam) so existing mocks compile unchanged.

**Follow-up dependencies (tracked, NOT done here):**
- Story 13 (#1571): consume D4's mechanism for member-removal path; orphaned-ACE/`group_membership`
  cleanup; revocation emails (EPIC 4). Record the D4 handoff in this PR's body.
- Story 14 (#1572): member visibility for members/ADMIN (strategy §4.5) — the *only* place a member
  roster may surface.
- Story 12: SQL-view rework for group grants on project cards (principal-only views today).

---

## 11. Traceability

- Story: **`FEAT-USER-GROUPS-08`** (#1566) — EPIC 3, parent feature #1558.
- Requirement IDs: **`GROUP-R-08`** (view groups on project), **`GROUP-R-09`** (manage role/revoke
  grants), **`GROUP-R-06`** (share/effective access underpinning), **`GROUP-NFR-01`** (≤60s
  revocation) — draft proposals pending the PO's dedicated requirements PR; same documented precedent
  as PR #1574/#1581/#1585 (§1).
- PR body: `Related to #1566`, PO-direction justification block, D4 mechanism summary + #1571
  handoff note, ADR-follow-up note (D1/D4 recorded; ADR creation pending human approval).
