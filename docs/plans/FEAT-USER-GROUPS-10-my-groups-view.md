# Implementation Plan — FEAT-USER-GROUPS-10 (#1568): My Groups view

**Status:** 🔵 Planned (ready for implementation)
**Parent story:** [#1568](https://github.com/qbicsoftware/data-manager-app/issues/1568) — `FEAT-USER-GROUPS-10: My Groups View (account area)`
**Tasks bundled in this effort:** [#1575](https://github.com/qbicsoftware/data-manager-app/issues/1575) (My Groups view: read list + self-remove) + [#1576](https://github.com/qbicsoftware/data-manager-app/issues/1576) (Ad-hoc group creation — implemented as a route, not a dialog)
**Branch:** `development` (merged backend #1574 present)
**PR reference:** `Related to #1568` — **NOT** `Closes #1568` (story stays open until #1577/#1578 land)

---

## 0. Goal & scope

Implement the **My Groups view** in the settings hub and the **ad-hoc group creation surface** as a
**dedicated, shareable route** (`settings/groups/new`), against the already-merged `user-groups`
backend (PR #1574). This effort delivers:

- **AC 1** (my groups list shows name, description, my internal role; non-member groups absent)
- **AC 4** (plain member can self-remove, no management actions)
- **Creation ACs** from the backend story (now UI-exposed): create ad-hoc group → caller becomes
  OWNER → appears in my groups; duplicate case-insensitive name → rejected with clear message.
- **Structural AC 5** placeholder: ORG rows render membership-only (no owner-equivalent controls).

**Explicitly OUT of scope** (guardrails):
- ❌ No backend changes to `user-groups` / `user-groups-api` / `user-groups-infrastructure`.
- ❌ No real owner/manager management commands (appoint manager, add/remove members, rename,
  describe, dissolve) → these are **disabled stubs** only; real commands = #1577 (blocked on
  FEAT-USER-GROUPS-04 backend #1562).
- ❌ No org-group creation/admin flows (#1578 / #1559 / #1560).
- ❌ No `docs/requirements.md` edits, no new ADR, no ACL/SID changes.
- ❌ No public group directory / discoverability surface (#1563).
- ✅ Story #1568 stays OPEN after this PR.

---

## 1. Architecture & key decisions

| Decision | Choice | Why |
|---|---|---|
| Creation surface | **Route** `settings/groups/new` under `SettingsMainLayout` | Shareable URL for users + documentation, natural browsing/back (stakeholder decision; no dialog, no nested RouterLayout) |
| List rendering | `Div`-based rows (PersonalAccessTokenComponent pattern), not Grid | Matches settings-hub convention; list is small |
| Create form | Plain `Div` form component (`NewGroupForm`), no AppDialog | A page, not a dialog |
| Service injection | `GroupInformationService` (api interface, reads) + `GroupService` (concrete, commands) | Beans already wired in `AppConfig`; no new beans |
| Current user id | `AuthenticationToUserIdTranslationService.translateToUserId(Authentication)` → `Optional<String>` | Exact QBiC user id string expected by `GroupService`; mirror `UserProfileMain` |
| Row actions | Keyed by `groupType` × `myRole` | See §5 action matrix |
| Tests | Spock component specs with injectable seams (no Spring context) | `PinnedProjectsComponentSpec` precedent |

---

## 2. File changes

### 2.1 Files to CREATE (all in `datamanager-app`)

| Path | Responsibility |
|---|---|
| `src/main/java/life/qbic/datamanager/views/settings/MyGroupsMain.java` | `@Route(AppRoutes.GroupsRoutes.MY_GROUPS, layout = SettingsMainLayout.class)`; calls `SpringComponent`/`@UIScope`/`@PermitAll`, extends `Main`, `BeforeEnterObserver`. Resolves current user id, builds `SettingsSection("My Groups")` + `MyGroupsComponent`, hosts "New group" action → `UI.getCurrent().navigate(NewGroupMain.class)`. |
| `src/main/java/life/qbic/datamanager/views/settings/MyGroupsComponent.java` | The my-groups list panel: renders each `MyGroupMembership` as a row (name, description, type badge, role badge, actions), empty state, self-remove flow, refresh callback seam (`Supplier`/event seams for testability). |
| `src/main/java/life/qbic/datamanager/views/settings/NewGroupMain.java` | `@Route(AppRoutes.GroupsRoutes.NEW_GROUP, layout = SettingsMainLayout.class)`; `@PageTitle("Settings · New Group")`; resolves user id, renders `NewGroupForm`, handles submit → `createAdHocGroup`, on success navigate to `MyGroupsMain`; on `DUPLICATE_GROUP_NAME` → inline field error; generic error → toast. |
| `src/main/java/life/qbic/datamanager/views/settings/NewGroupForm.java` | Plain `Div` form: name `TextField` (required ≤80), description `TextArea` (optional ≤500), Create/Cancel buttons, client-side validation mirroring VOs, `isGroupNameAvailable` blur hint, listener/callback seam. |

### 2.2 Files to MODIFY

| Path | Change |
|---|---|
| `src/main/java/life/qbic/datamanager/views/settings/SettingsNavigationComponent.java` | `addTab("My Groups", VaadinIcon.USERS, MyGroupsMain.class);` |
| `src/main/java/life/qbic/datamanager/views/settings/SettingsMainLayout.java` | In `beforeEnter`: map `NewGroupMain.class → MyGroupsMain.class` before `selectTabFor(...)` so the My Groups tab stays active on `/settings/groups/new`. |
| `src/main/java/life/qbic/datamanager/views/AppRoutes.java` | Add `GroupsRoutes` nested class: `MY_GROUPS = "settings/groups"`, `NEW_GROUP = "settings/groups/new"`. |
| `src/main/resources/messages/toast-notifications.properties` | Add `user-groups.created.success.*` (+ `.error` generic, and a self-remove success/error if desired) keys, following the `project.created.success` format. |
| `frontend/themes/datamanager/components/…` (CSS) | Row/badge/disabled-stub styling (follow the app's component CSS convention under `frontend/themes/datamanager/components/`). |

---

## 3. Route registration & navigation

```java
@Route(value = AppRoutes.GroupsRoutes.MY_GROUPS, layout = SettingsMainLayout.class)
@SpringComponent @UIScope @PermitAll
@PageTitle("Settings · My Groups")
public class MyGroupsMain extends Main implements BeforeEnterObserver { ... }

@Route(value = AppRoutes.GroupsRoutes.NEW_GROUP, layout = SettingsMainLayout.class)
@SpringComponent @UIScope @PermitAll
@PageTitle("Settings · New Group")
public class NewGroupMain extends Main implements BeforeEnterObserver { ... }
```

- **"New group" action** on `MyGroupsMain`: `UI.getCurrent().navigate(NewGroupMain.class)`
  (programmatic navigation → browser history works; use the RouterLink alternative only if a
  visible link is preferred on the page).
- **Cancel/back** on `NewGroupForm`: `UI.getCurrent().navigate(MyGroupsMain.class)`.
  Browser back also works naturally (both routes share `SettingsMainLayout`). No `pushState` needed.
- **Deep-link to `/settings/groups/new`** without back-stack: Cancel still returns to the list → no dead-end.

---

## 4. User-id resolution & service injection

```java
// in both Mains — mirror UserProfileMain.beforeEnter
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String userId = userIdTranslator.translateToUserId(auth).orElseThrow();
```

- Inject via constructor (`@Autowired`):
  - `GroupInformationService` (interface from `life.qbic.usergroups.api`) — reads (`listMyGroups`, `isGroupNameAvailable`)
  - `GroupService` (concrete `life.qbic.usergroups.application.GroupService`) — commands (`createAdHocGroup`, `removeMembership`)
  - `AuthenticationToUserIdTranslationService` — user id
  - `MessageSourceNotificationFactory` — toasts
- The returned **`String` is exactly what `GroupService` expects** — no conversion to identity `UserId`.
- Only one `GroupInformationServiceImpl` exists → by-type autowiring resolves; `@Qualifier("groupInformationService")` only if a future ambiguity appears.

---

## 5. My-groups list component (MyGroupsComponent)

**Structure:** `SettingsSection("My Groups", description)` + right-aligned **"New group"** button
(`section.addAction(...)`, mirrors the Personal Access Token generate button) + `Div.my-groups-list`.

**Row rendering (per `MyGroupMembership`):** name (bold), description (secondary, omit if null),
type badge (`Org` / `Ad-hoc`), role badge (`Owner` / `Manager` / `Member`), action button(s) per matrix.

**Action matrix** (keyed by `groupType()` × `myRole()`):

| Type | Role | Actions |
|---|---|---|
| ADHOC | MEMBER | **"Leave group"** enabled → self-remove (confirm dialog) |
| ADHOC | OWNER | management actions **disabled** with tooltip "Available in an upcoming update — FEAT-USER-GROUPS-04"; no self-remove (owner-leave is #1577/#1563) |
| ADHOC | MANAGER | management actions disabled-with-tooltip (same stub); self-remove enabled (safe fallback; confirm with owner) |
| ORG | any | **No management controls**; name/description/role only (structural AC 5) |

**Tooltips on disabled buttons:** Vaadin 24 `disabled` components don't fire events — wrap each
disabled button in a `Span` + `Tooltip.forComponent(component, "…")`.

**Self-remove flow:**
1. "Leave group" → `AlertDialog.Builder().intent(WARNING).title("Leave group?").message("…").confirmButton("Leave", …).cancelButton("Cancel", …).build().open()`.
2. On confirm → `groupService.removeMembership(groupId, userId)`:
   - `.onValue(v -> { toast success; refresh(); })`
   - `.onError(e -> { toast error; })` — never swallow.
3. `refresh()` re-runs `groupInformationService.listMyGroups(userId)` and rebuilds the rows `Div`
   (small list — full rebuild is fine and automatically honors "non-member groups do not appear").

---

## 6. Create page (NewGroupMain + NewGroupForm)

**NewGroupForm:** `Div`-based page form (not a dialog):
- `TextField name` — required, `setRequiredIndicatorVisible(true)`, maxLength 80.
- `TextArea description` — optional, maxLength 500.
- Buttons: **Create** (submit) + **Cancel** (→ navigate to list).
- **Client-side validation** mirrors VOs: name trim + non-blank + ≤80; description trim + ≤500.
  Show field errors inline (`setError`, `setInvalid`). Optional `isGroupNameAvailable` hint on name blur.
- Callback seam for tests: submit fires an event / `Consumer<GroupDraft>` with (name, description);
  navigation behind an injectable `Runnable navigateToMyGroups`.

**Submit (`NewGroupMain`):**
```java
Result<GroupInfoProjection, ApplicationException> res =
    groupService.createAdHocGroup(userId, GroupName.from(name), GroupDescription.from(desc));
res.onValue(created -> { toast("user-groups.created.success", {name}); navigateToMyGroups(); })
   .onError(e -> {
     if (e.getErrorCode() == ErrorCode.DUPLICATE_GROUP_NAME)
       nameField.setError("A group with this name already exists. Names are unique (case-insensitive).");
     else toast("user-groups.created.error", …);   // generic, never swallow
   });
```
- `GroupName.from` / `GroupDescription.from` may throw validation exceptions (defensive check → field error).
- **On success navigate to the list** so the new group visibly appears with **OWNER** role
  (recommended; avoids duplicate submission on a stale empty form). Optionally toast first.
- Wires via seams for testability: `translateToUserId` + navigation + toast behind injectable
  `Supplier`/`Runnable` (like `PinnedProjectsComponent`).

---

## 7. Navigation integration (active-tab)

- Route constants in `AppRoutes.GroupsRoutes` (§2.2); `@Route` uses them.
- Tab added in `SettingsNavigationComponent` (§2.2).
- **Tab-highlight on `/new`:** `SettingsMainLayout.beforeEnter` maps `NewGroupMain.class → MyGroupsMain.class` first:
  ```java
  Class<?> target = event.getNavigationTarget();
  if (target == NewGroupMain.class) target = MyGroupsMain.class;
  settingsNavigationComponent.selectTabFor(target);
  ```
- Shared `SettingsMainLayout` keeps the settings chrome (aside/header) stable across both routes.

---

## 8. Tests (Spock component specs, no Spring context)

Follow `PinnedProjectsComponentSpec` precedent (inject fakes via constructor `Supplier`/callback seams).

| Spec | Covers |
|---|---|
| `MyGroupsComponentSpec.groovy` | renders rows (name, description, ORG/ADHOC badge, role badge); empty state; MEMBER → Leave present + invokes remove callback; OWNER → management disabled + tooltip, no self-remove; ORG → no management buttons; `refresh()` re-renders. |
| `NewGroupFormSpec.groovy` | validation (blank/>80 name, >500 desc → inline errors); Cancel event; submit carries name+description; `isGroupNameAvailable` blur callback. |
| `NewGroupMainSpec.groovy` | with seams: success → navigation to `MyGroupsMain`; duplicate → inline name error + no navigate; generic error → toast factory called + no navigate. |
| (optional) `MyGroupsMainSpec.groovy` | "New group" button triggers navigation seam to `NewGroupMain`. |

Locations: `datamanager-app/src/test/groovy/life/qbic/datamanager/views/settings/…` (mirror package).

---

## 9. Build & verify

```bash
./mvnw -pl datamanager-app -am clean verify      # full unit + Spock suite
./mvnw -pl datamanager-app -am compile           # fast compile check
./mvnw spring-boot:run -pl datamanager-app -Pdevelopment   # dev-mode smoke (mock backend)
```

Format with Google style (`GoogleStyle.xml` / project formatter) before committing.

---

## 10. Commit sequencing (single PR, incremental)

1. **Scaffold:** `AppRoutes.GroupsRoutes` + `MyGroupsMain` (empty `SettingsSection`) + nav tab + `@PageTitle`. Compile.
2. **Read list:** `MyGroupsComponent` rendering memberships + user-id resolution + empty state. Test `MyGroupsComponentSpec`.
3. **Create route:** `NewGroupMain` + `NewGroupForm` + `SettingsMainLayout` tab-mapping + glue + toast keys. Test `NewGroupFormSpec`/`NewGroupMainSpec`.
4. **Self-remove:** `AlertDialog` confirm + `removeMembership` + refresh + toast. Covered in `MyGroupsComponentSpec`.
5. **Polish:** CSS, format, full `verify`.

---

## 11. Risks / blockers

1. **Tab-highlight on `/new`** — must touch `SettingsMainLayout.beforeEnter` (else stale/no tab on the create page). Single point, 2 lines.
2. **`UI.getCurrent()` null in component tests** — navigation + toasts must be behind injectable seams, else Spock specs NPE. Concrete fix: `Runnable navigateToMyGroups` / injected `MessageSourceNotificationFactory` fakes.
3. **Message keys** — no `user-groups.*` keys exist yet; add to `toast-notifications.properties` (resource-only; not a requirements change).
4. **Route value collision** — `settings/groups` + `settings/groups/new` verified free; register via constants.
5. **Empty description / nulls** — guard `MyGroupMembership.groupDescription()` (may be null) before `Text(null)`.
6. **Spock naming** — name specs `*Spec.groovy` so surefire picks them up (`**/*Spec.class`).
7. **CSS aggregation** — confirm where theme imports component styles (`frontend/themes/datamanager/styles.css` or `@import`) when adding new CSS.
8. **Bean injection** — inject the api interface `GroupInformationService` (single impl); do not `new` it.

---

## 12. Traceability

This effort implements tasks **#1575** + **#1576** under story **#1568** (`FEAT-USER-GROUPS-10`).
- Requirement IDs referenced: `GROUP-R-05`, `GROUP-R-03`, `GROUP-R-02`, `GROUP-R-04`, `GROUP-NFR-02`
  (these are draft proposals pending the requirements PR per AGENTS.md §12; same precedent as PR #1574).
- PR must use `Related to #1568` (never `Closes #1568`) and carry the partial-AC progress note.
- After merge: post the partial-AC progress comment on #1568 (AC matrix from the story body).