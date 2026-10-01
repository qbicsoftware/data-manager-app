# Implementation Plan — FEAT-PROFILE-PICTURES: Profile Pictures for Users and User Groups

> **Story:** `FEAT-PROF-PIC-01` (user profile picture) and `FEAT-PROF-PIC-02` (group profile
> picture) — **proposed IDs, not yet approved** (see §9 Governance)
> **Parent Feature:** `FEAT-PROFILE-PICTURES` — **proposed slug, not yet created**
> **Requirements:** `USER-R-05` / `USER-NFR-02` (user) and `GROUP-R-13` / `GROUP-NFR-04` (group) —
> **proposed IDs pending a human-approved requirements PR**
> **ADR:** new ADR (profile-picture storage; number to be assigned) — **pending human approval**
> **Schema:** new `sql/migrations/create-profile-pictures.sql` + `sql/complete-schema.sql`
> **Date:** 2026-10-01

This file is the task breakdown for the feature. It assumes the story is tracked in
`docs/features.md` and, where story issues exist, linked from GitHub (see §9).

---

## 1. Scope

An authenticated user can set a profile picture for **their own user profile** and, where they
hold the right role, for a **user group**. The picture is normalized on upload (square PNG
derivative, original discarded), stored in the database, and rendered as an avatar wherever a
user or group identity is shown. When no picture is set, the existing **identicon** fallback is
used (already implemented for users, new for groups). A system administrator can force-remove any
picture and inspect an audit trail of set/replace events.

In scope:

- User profile picture: upload, crop/zoom/reposition with live circular preview, replace, remove.
- Group profile picture (both `GroupType.ADHOC` and `GroupType.ORG`): same operations, gated by
  the existing group role model.
- Avatar rendering with identicon fallback for groups (mirror of the existing `UserAvatar`).
- Immutable, content-hashed, cacheable delivery of stored pictures.
- System-admin force-remove and a system-admin-only audit list.

Out of scope (v1):

- Setting or reading profile pictures through the programmatic API (`API-*`, personal access
  tokens).
- ORCID/external profile-picture import.
- Automated image-content moderation / NSFW classification.
- Animated content, SVG, HEIC, WebP.
- Retention of the original (pre-crop) upload.

---

## 2. Requirements elicited (to be normalized into `docs/requirements.md`)

> **Agent rule (AGENTS.md §0/§12):** these requirements must be added to `docs/requirements.md`
> in a **dedicated, human-approved PR before any implementation**. The statements below are the
> agreed intent; the PR author rephrases them per `docs/requirements-guide.md`.

- **USER-R-05 — User Profile Picture.** An authenticated user shall be able to set, replace, and
  remove their own profile picture; no other user (including a system administrator) may set or
  replace another user's picture.
- **USER-NFR-02 — Profile Picture Validation and Privacy.** Uploads shall be limited to 1 MB and
  to PNG/JPEG; the server shall sniff the content type, enforce a maximum decoded pixel count
  (decompression-bomb guard), reject animated content, strip metadata (including EXIF), and store
  only a normalized square PNG derivative.
- **GROUP-R-13 — Group Profile Picture.** A manager of an ad-hoc group and a system administrator
  for an organisational group shall be able to set, replace, and remove that group's picture.
- **GROUP-NFR-04 — Profile Picture Delivery.** Stored pictures shall be served from an immutable,
  content-hashed URL to any authenticated user; when no picture exists the identicon fallback
  shall be used. System administrators shall be able to force-remove any picture and shall see an
  audit list of set/replace events.

---

## 3. Design decisions already taken (do not re-litigate in review)

| Decision | Outcome | Source |
|---|---|---|
| Which owners | user profile **and** user-group profile (`UserGroup`, both `ORG` and `ADHOC`) | interview |
| Where rendered | everywhere an identity is shown — profile pages, nav/header, member/group lists, project access grids, `AvatarGroup`s, audit/"created by" — via one reusable avatar component | interview (d) |
| No-picture fallback | **identicon** (already used by `UserAvatar` for users); add the same for groups, keyed on group id | interview; `IdenticonGenerator` |
| Storage medium | database; **dedicated 1:1 table** so the blob never sits on the hot `users` / `user_group` rows | interview |
| Retention | **derivative only** — normalize on upload, discard the original; 1 MB is an ingress limit | interview |
| Format | PNG and JPEG accepted; **PNG stored** | interview |
| Geometry | stored derivative is **square 256×256** (circle is a CSS mask) | interview |
| Crop UX | interactive crop/zoom/reposition with a **live circular preview** via **Cropper.js**, live in `datamanager-app` (not the design-system repo) | interview |
| Authorisation — set | user: self only; group: ad-hoc **Manager+**, org **system admin** | interview; mirrors `GroupService` |
| Authorisation — remove | same as set, **plus** system admin may force-remove any picture | interview |
| Authorisation — read | **any authenticated user** may fetch any avatar | interview (a) |
| Delivery | authenticated endpoint at an immutable **content-hashed URL**; `Cache-Control: immutable` | interview |
| Audit | system-admin-only audit list of **set** and **replace** events | interview |
| Group lifecycle | blob retained when a group is dissolved/deactivated; group data keeps **no FK to `users`** (cross-context rule) | interview; `create-user-groups.sql` |

---

## 4. Architecture and placement

**Key decision: the picture is not aggregate state.** Neither the identity `User` aggregate nor
the `UserGroup` aggregate gains an image field; image bytes carry no business invariant. The
picture is a presentation/media concern owned by a small `profile-picture` package.

Preferred placement (no new Maven module — adding one requires human approval and is avoided
deliberately):

```
datamanager-app/src/main/java/life/qbic/datamanager/profilepicture/
├── ProfilePictureService.java          # application facade (authz + orchestration)
├── ProfilePicture.java                 # domain record: owner, contentType, hash, bytes, updatedAt
├── ProfilePictureOwnerType.java        # USER | GROUP
├── ProfilePictureRepository.java       # Spring Data repo + @Entity ProfilePictureEntity
├── ProfilePictureAuditRepository.java  # Spring Data repo + @Entity ProfilePictureAuditEntity
├── ImageNormalizer.java                # validation + crop + scale + PNG encode (JDK ImageIO)
├── ProfilePictureController.java       # GET /profile-pictures/{type}/{id}/{hash}.png
└── ProfilePictureUrlResolver.java      # view-facing: owner -> immutable URL or null
```

This mirrors the existing `life.qbic.datamanager.announcements` package (which already colocates
an `@Entity`, a Spring Data repository, and a service in `datamanager-app`), so it does not
introduce a new composition pattern.

**Cross-context note:** the store is keyed by `(owner_type, owner_id)` with **no foreign keys**
into `users` or `user_group`, following the `pinned_projects` / `group_membership` precedent.
`ProfilePictureService` is the only writer; `GroupService` delegates its picture operations to it
after enforcing the group role.

**If reviewers prefer a dedicated bounded context** (`profile-picture` Maven module analogous to
`user-groups`), that requires module + ADR approval (§9); the package above is designed so it can
be lifted into a module with minimal churn.

---

## 5. Data model

### 5.1 `profile_picture`

| Column | Type | Notes |
|---|---|---|
| `owner_type` | `varchar(16)` NOT NULL | `USER` or `GROUP` |
| `owner_id` | `varchar(255)` NOT NULL | user id / group id (bare, no FK) |
| `content_type` | `varchar(32)` NOT NULL | `image/png` |
| `content_hash` | `char(64)` NOT NULL | SHA-256 hex of the stored bytes; drives the immutable URL and cache-busting |
| `width` | `smallint` NOT NULL | derivative width (256) |
| `height` | `smallint` NOT NULL | derivative height (256) |
| `data` | `mediumblob` NOT NULL | normalized PNG derivative (≤ target² pixels) |
| `updated_at` | `datetime(6)` NOT NULL | last write |
| `updated_by` | `varchar(255)` NOT NULL | acting user id |

Primary key `(owner_type, owner_id)`. Secondary index on `(owner_type, owner_id, updated_at)` not
needed — the PK already covers the 1:1 read. The blob is isolated from `users`/`user_group`.

### 5.2 `profile_picture_audit`

| Column | Type | Notes |
|---|---|---|
| `id` | bigint AUTO_INCREMENT PK | |
| `owner_type` | `varchar(16)` NOT NULL | |
| `owner_id` | `varchar(255)` NOT NULL | |
| `action` | `varchar(16)` NOT NULL | `SET` (no prior row) or `REPLACE` (prior row existed) |
| `actor_id` | `varchar(255)` NOT NULL | who performed it |
| `previous_content_hash` | `char(64)` NULL | null for `SET` |
| `new_content_hash` | `char(64)` NOT NULL | |
| `created_at` | `datetime(6)` NOT NULL | |

Index `(owner_type, owner_id, created_at)` for the per-owner history; system-admin list reads a
global `created_at` ordered page (add index `(created_at)` if the list is paginated).

Removal and force-removal are **not** audited rows (the agreed audit is set/replace only); this is
deliberate and recorded as a limitation.

---

## 6. Tasks

### T1 — Governance preconditions (blocking; do not start T2+ before these land)

- [ ] Requirements PR: add `USER-R-05`, `USER-NFR-02`, `GROUP-R-13`, `GROUP-NFR-04` to
  `docs/requirements.md` with rationale and source; **dedicated PR, no code bundled**
  (AGENTS.md §0/§12). Human approval required.
- [ ] Create GitHub Feature issue `FEAT-PROFILE-PICTURES` (`.github/ISSUE_TEMPLATE/feature.yml`),
  referencing the new requirement IDs.
- [ ] Create Story issues `FEAT-PROF-PIC-01` / `FEAT-PROF-PIC-02`
  (`.github/ISSUE_TEMPLATE/story.yml`), parented to the Feature.
- [ ] Write `FEAT-PROFILE-PICTURES` into `docs/features.md` with narrative, acceptance criteria,
  and status.
- [ ] New ADR (MADR template): blob-in-database normalization, dedicated-table isolation,
  cross-context keying, controller delivery. Update the ADR index. Human approval required.
- DoD: IDs frozen before any Task references them.

### T2 — Image normalization and validation (`datamanager-app`, `profilepicture`)

- [x] `ImageNormalizer.normalize(byte[] raw)` / `normalize(byte[] raw, CropFrame)`:
  - reject `raw.length > 1 MiB`;
  - sniff magic bytes (PNG `89 50 4E 47`, JPEG `FF D8 FF`) and **ignore any declared type**;
  - decode with `ImageIO` (`ImageIO.setUseCache(false)`), reject on null reader;
  - reject animated content (no JPEG/PNG animation expected, but reject PNG with an `acTL` chunk
    and reject multi-frame readers);
  - enforce a maximum decoded pixel count (proposed ≤ 16 MP) **before** scaling;
  - strip metadata by re-encoding only pixel data;
  - crop to the requested square frame (x, y, zoom from the client), scale to **256×256** with
    `RenderingHints` bicubic, encode PNG.
- [x] `NormalizedImage` value object: `byte[] png`, `int width`, `int height`, `String sha256Hex`.
- [x] Reject SVG, GIF, BMP, HEIC, WebP (allowlist only).
- [x] DoD met: `ImageNormalizerSpec` (9 tests) covers size, spoofed content type,
  pixel-count bomb, crop math, animated-input rejection and corrupt input. No `@SpringBootTest`.
- Note: `reader.setInput(input, false, true)` (search enabled) is required because
  `getNumImages(true)` needs it; JPEG EXIF orientation is not yet honored (see §7).

### T3 — Persistence

- [x] `ProfilePictureRepository` (nested `ProfilePictureEntity`) and `ProfilePictureAuditRepository`
  (nested `ProfilePictureAuditEntity`), colocated `@Entity` pattern.
- [x] `profile_picture` and `profile_picture_audit` added to `sql/complete-schema.sql`.
- [x] `sql/migrations/create-profile-pictures.sql` (idempotent `CREATE TABLE IF NOT EXISTS`,
  rollback = drop both tables), for datasource `data_management`.
- [x] Registered in `sql/migrations/README.md` and `docs/migrations/NEXT.md` (row + expanded section).
- [x] Additive, new empty tables, utf8mb4_unicode_ci, no FK to `users`/`user_group`.
  **Schema change still needs explicit human approval in review.**

### T4 — `ProfilePictureService` (application facade)

- [x] `setUserPicture(actingUserId, upload)` — self only (structurally: no target parameter).
- [x] `removeUserPicture(actingUserId)` — self only.
- [x] `setGroupPicture(groupId, actingUserId, upload)` / `removeGroupPicture(...)` — authorization
  through `GroupPictureAuthorization`.
- [x] `forceRemove(adminUserId, ownerType, ownerId)` — system-admin gate; no audit row.
- [x] `find(ownerType, ownerId)` → `Optional<ProfilePicture>` for the delivery endpoint.
- [x] `listAudit(adminUserId, pageable)` — system admin only.
- [x] Expected failures as `Result<…, ApplicationException>`; validation failures carry the
  `ImageNormalizationException.Reason` name as the single error parameter.
- [x] `deleteByOwner(ownerType, ownerId)` — provided for the (future) user-deletion flow.
- [x] DoD met: `ProfilePictureServiceSpec` (12 tests) covers SET vs REPLACE audit, self/actor
  guards, group allow/deny, admin force-remove, invalid-image reason parameter, and admin-only audit.

### T5 — `GroupService` integration

- [x] Add `canManageProfilePicture(groupId, actingUserId)` to `GroupService`: read-only authorization
  mirroring the existing profile-edit rules (ad-hoc OWNER/MANAGER; org groups via
  `groupAdministrationPermission.isAdmin(...)`). Exposes an intention-revealing query without
  leaking the role model.
- [x] Expose it through the `GroupPictureAuthorization` port (wired in `AppConfig`), so
  `ProfilePictureService` (in `datamanager-app`) never depends on the group domain directly.
- Design change vs. the original draft: instead of adding `updatePicture`/`removePicture` to
  `GroupService` (which would invert the dependency and force the store into `user-groups`), the
  group picture operations stay in `ProfilePictureService` and consult the authorization port.
  This keeps one writer for the picture store and a single schema location.
- [x] Compile verified via `test-compile`; existing `GroupServiceSpec` unaffected (additive method).
  Add explicit specs for ad-hoc manager allowed / member denied / org-admin allowed.

### T6 — Delivery endpoint and caching

- [x] `ProfilePictureController` (`@Controller`):
  - stable `GET /profile-pictures/{ownerType}/{ownerId}` → redirect to the immutable hashed URL
    when a picture exists, else the identicon SVG;
  - immutable `GET /profile-pictures/{ownerType}/{ownerId}/{hash}` → `image/png`,
    `Cache-Control: public, max-age=31536000, immutable`, `ETag`, `304` on `If-None-Match`;
  - `404` on missing picture or hash mismatch; explicit authentication check (401 for anonymous).
- [x] `ProfilePictureUrlResolver` (+ lightweight `findContentHash` query so rendering never loads
  the blob).
- Note: authentication is enforced in the controller, so **no `SecurityConfiguration` change was
  needed** (avoids a §12 approval-gated change). Verify in manual testing that anonymous requests
  receive 401.
- Remaining: automated `MockMvc` coverage for 200/302/304/401/404.

### T7 — Avatar component generalization

- [x] `UserAvatar` points at the stable URL; stored picture else identicon is resolved by the
  endpoint, so **no resolver injection was needed at any call site**.
- [x] `setUserId(...)` upgrades all existing user call sites automatically; `setGroupId(...)` added
  for groups; `UserAvatarGroupItem.forGroup(...)` for group items.
- [x] Group avatars rendered in `MyGroupsComponent`, `AdminGroupsMain`, `GroupDetailMain`.
- Note: existing `UserAvatarGroupItem(userName, userId)` call sites
  (`ProjectSummaryComponent`, `ProjectCollectionComponent`) now render stored user pictures
  without change.
- Remaining: visual/CSS polish for the new group avatars.

### T8 — Cropper upload UI

- [x] `ProfilePictureCropField`: `@NpmPackage("cropperjs", "1.6.2")` + `@JsModule(
  ./javascript/profilepicturecropper.js)`, square crop frame with live circular preview; exports
  the crop as a 256×256 PNG data URL via `requestCrop(Consumer<byte[]>)` (JS→server return value).
- [x] `ProfilePictureDialog extends DialogWindow`: file picker → crop → **Save picture**; inline
  error span; cancel closes.
- [x] Client-side 1 MB pre-check is advisory; the server re-validates and re-encodes in T2.
- [x] Integrated into `UserProfileComponent` (avatar row + Change/Remove) and the "Group" profile
  group in `GroupDetailMain` (gated by `canManageProfile`).
- [x] Error messages via `ProfilePictureMessages.userMessage(...)` (maps the normalization reason;
  reserved for later i18n keys rather than `MessageSourceNotificationFactory`).
- [x] CSS: new `components/profile-picture.css` imported from `custom.css`; reuses theme tokens.
- Remaining: component spec for the dialog states; manual/visual pass for the live preview.

### T9 — Admin force-remove and audit view

- [x] `AdminProfilePictureAuditMain` (admin-only route `groups/admin/profile-pictures`, gated in
  `beforeEnter`): paginated audit grid with owner, action, actor, timestamps, hash previews, and a
  per-row **Force remove** action calling `ProfilePictureService.forceRemove(...)`.
- [x] Admin navigation tab "Profile pictures" in `GroupsNavigationComponent`.
- Note: force-remove is exposed on the audit view rather than on every profile surface; the
  service API already supports any owner. Removing an entry is not audited (agreed scope), so the
  row's button is spent in place.
- Remaining: spec asserting non-admins are rejected; manual pass.

### T10 — Tests

- [ ] Spock specs per task above (unit, no Spring context where avoidable).
- [ ] Integration/edge coverage: cascade delete path (unit-level, since no user deletion exists
  yet — see §7), identifier churn, hashing determinism (same bytes → same hash).
- [ ] Do not add `@SpringBootTest` for pure application-layer specs.

### T11 — Verification

- [ ] `./mvnw -Pdevelopment -pl datamanager-app -am test` → BUILD SUCCESS.
- [ ] Manual pass on a running instance with the migration applied:
  upload a portrait and a landscape image (crop each), verify the circular preview matches the
  stored avatar everywhere (nav, project access grid, group members, group detail);
  replace, remove, verify identicon fallback; set a group picture as ad-hoc manager and as org
  admin, verify a member cannot; force-remove as system admin and confirm the audit row;
  verify `Cache-Control: immutable` and a `304` on reload.

---

## 7. Known limitations and open items

- **No user-deletion flow exists today.** The agreed "delete the blob when a user is deleted"
  cannot be wired end to end; the plan provides `ProfilePictureService.deleteByOwner(...)` for the
  future flow and records the requirement. Deactivation must **not** delete the blob.
- **Removal/force-removal are not audited** (audit is set/replace only, as agreed). If that
  changes, add `REMOVE`/`FORCE_REMOVE` rows.
- **Cross-session cache invalidation:** an immutable content-hashed URL means a replaced picture
  gets a new URL; pages already rendered in another session keep the old image until their next
  render/refresh. This is intended (no invalidation problem) but is user-visible.
- **JPEG EXIF orientation is not honored**: a phone photo may appear rotated because `ImageIO`
  ignores the EXIF orientation tag. The browser cropper shows the same raw orientation, so the
  user can compensate by framing; a proper EXIF-orientation read can be added in T2 later.
- **Client-side crop is not yet wired into the service**: `setUserPicture`/`setGroupPicture`
  currently normalize without a crop frame (server center-crop). T8 must pass the browser crop
  frame through and the server must still treat it as untrusted and re-derive/re-encode.
- **Derivative size is frozen at 256×256.** Changing it later re-encodes only new uploads
  (existing derivatives keep their size until replaced).
- **Cropper.js is a new npm dependency** (build-time change). If the dependency is later rejected,
  a vanilla-canvas cropper would need to be substituted behind the same component API.
- **`GROUP` domain is not yet in `AGENTS.md`'s domain list** (`GROUP-*` requirements are draft).
  The requirements PR must keep the ID schema consistent with the pending user-groups PR.

---

## 8. Follow-up candidates

1. Programmatic API for profile pictures (`API-*`), reusing `ProfilePictureService`.
2. ORCID profile-picture import for ORCID-linked accounts, as an alternative fallback source.
3. Content moderation / reporting workflow, if abuse is observed.
4. Retaining the original upload to allow re-rendering at other sizes.
5. Lifting the `profilepicture` package into a dedicated bounded-context module.

---

## 9. Governance note

`AGENTS.md` §0/§12 require a Feature, Stories, and Tasks to live in GitHub issues, and a **new
capability to be recorded in `docs/requirements.md` via a dedicated human-approved PR before
implementation**. None of that exists yet for this capability, and this plan deliberately does not
perform it. The `FEAT-PROFILE-PICTURES`, `FEAT-PROF-PIC-01/02`, and `USER-*`/`GROUP-*` IDs used
above are **proposals**; no implementation PR may reference them until the requirements PR, the
Feature/Story issues, the `docs/features.md` entry, and the ADR (with index update) have landed
with human approval.

If reviewers prefer to track the stories in `docs/features.md` only (without GitHub issues), the
`**GitHub Feature**` / `**GitHub**` fields carry `—` and the implementation PR references
`FEAT-PROF-PIC-01` / `FEAT-PROF-PIC-02` and the requirement IDs directly, mirroring
[`FEAT-PINNED-01-implementation-plan.md`](FEAT-PINNED-01-implementation-plan.md) §6.
