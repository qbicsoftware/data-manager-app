# Footer Redesign — Worker Implementation Report

## Task
Implement the oracle-approved footer redesign in `datamanager-app`:
"Full-bleed footer bar + centered 1400px content shell + brand block (Data Manager + UT logo),
link groups distributed via grid."

## Files changed (exact paths)

1. **`datamanager-app/src/main/java/life/qbic/datamanager/views/general/footer/FooterComponent.java`** (modified)
2. **`datamanager-app/frontend/themes/datamanager/components/footer.css`** (reworked)

No other files were touched by this worker. The parallel header/navigation worker's files
(`navbar.css`, `DataManagerMenu.java`, `UserMainLayout.java`, `LandingPageLayout.java`,
`ProjectMainLayout.java`, `ExperimentMainLayout.java`, `SettingsMainLayout.java`, `HomeLink.java`)
were left untouched and appear separately in `git status`.

## Summary of each change

### FooterComponent.java
- Added constants `UT_LOGO_PATH = "login/university-tuebingen-logo.svg"`,
  `BRAND_NAME = "Data Manager"`, `BRAND_SUBTITLE = "University of Tübingen Life Science Data Management"`.
- Constructor now wraps the existing link groups + bottom bar in a new `Div.app-footer-inner`
  (the capped content shell): `inner.add(brand(), linkGroups(...), bottomBar()); add(inner);`.
- New `brand()` builder: `Span.app-footer-brand-name` ("Data Manager" — exact navbar wording),
  `Span.app-footer-brand-sub` (verbatim LandingPageTitleAndLogo subtitle), and an
  `Anchor.app-footer-ut-logo` -> `https://uni-tuebingen.de/` with `target="_blank"`,
  `rel="noopener noreferrer"`, `aria-label="University of Tübingen (opens in new tab)"`,
  wrapping the UT logo `Image` (alt "University of Tübingen").
- `getUTLogo()` streams the SVG via the **same `DownloadHandler`/`DownloadResponse` pattern** as
  `LandingPageTitleAndLogo` (never a plain relative URL — breaks on deep routes). A guard throws
  `IllegalStateException` if the classpath resource is missing.
- Extracted `linkGroups(...)` and `bottomBar()` into private static helpers; `linkGroup`,
  `externalLink`, `contactLink` unchanged. Service-parameter passing unchanged (still `@Value`).
- Class Javadoc updated to document the brand block and the capped-shell/full-bleed layout.

### footer.css
- Header comment rewritten documenting the "full-bleed bar + capped centered content shell"
  pattern; references `grid-templates.css` and `settings.css` as the established pattern.
- `.app-footer`: **vertical padding only** (`padding: var(--lumo-space-l) 0 var(--lumo-space-m)`)
  — horizontal gutters moved into the inner shell; bar stays full-bleed (settings `grid-area:
  data-manager-footer` spans all tracks, untouched).
- `.app-footer-inner` (new): `box-sizing:border-box; width:100%; max-width:
  var(--content-shell-max-width); margin-inline:auto; padding-inline: var(--lumo-space-xl);
  display:grid; grid-template-columns: minmax(0,1fr) minmax(0,2fr); grid-template-areas:
  "brand links" "bottom bottom"; column-gap: var(--spacing-08); row-gap: var(--lumo-space-m);`
- Brand styles: `.app-footer-brand` (flex column, gap `--spacing-02`), brand name/sub
  (`--lumo-header-text-color`, `--lumo-secondary-text-color`), `.app-footer-ut-logo img`
  (`display:block; height:1.75em; width:auto;`), hover opacity 0.85.
- `.app-footer-links` now `grid-area:links` + `justify-content: space-between;
  align-items: flex-start` (kept wrap + original gap tokens).
- `.app-footer-bottom` now `grid-area:bottom` (rest unchanged).
- Focus-visible widened from `.app-footer-group a` to `.app-footer a:focus-visible` so the UT
  logo link gets the focus ring too.
- New `@media (max-width: 900px)` single-column: `"brand" "links" "bottom"`, links
  `justify-content: flex-start`.
- Existing `@media (max-width: 600px)` adjusted to the inner shell (`padding-inline:
  var(--spacing-04)`, `.app-footer` `padding: var(--spacing-04) 0`, smaller UT logo `1.5em`),
  keeping the existing group stack.
- Only existing Lumo/custom tokens used — no new colors, no new dependencies. All existing
  group/link/hover/underline styles preserved verbatim.

## Checklist against the oracle strategy
- ✓ `login/university-tuebingen-logo.svg` served via DownloadHandler (resource verified present
  at `datamanager-app/src/main/resources/login/university-tuebingen-logo.svg`)
- ✓ Hardened external anchor: `https://uni-tuebingen.de/`, `target=_blank`,
  `rel="noopener noreferrer"`, aria-label, focus-visible, alt="University of Tübingen"
- ✓ Inner shell capped at `--content-shell-max-width` (1400px), centered
- ✓ Link groups `justify-content: space-between` on desktop, `flex-start` on tablet
- ✓ 900px single-column breakpoint + adjusted 600px query
- ✓ labelled `<nav>` landmarks + `aria-label`s preserved (Legal/Resources/Contact)
- ✓ RouterLink legal links preserved; copyright bar preserved
- ✓ Footer bar itself NOT capped (settings grid + landing layout safe)
- ✓ `settings.css`, `LandingPageTitleAndLogo.java`, requirements/ADRs NOT touched
- ✓ Prototype scope unchanged — no shared-state risk

## Tests
- A `FooterComponentSpec.groovy` was initially written to pin the brand block/anchor/aria
  contract but was **removed**: constructing `FooterComponent` requires an active `UI`
  (DownloadHandler) and a `VaadinService` whose router resolves the two internal
  `RouterLink` routes. A bare `UI.setCurrent` was insufficient, and a `VaadinServletService`
  stub needs a servlet container/servlet context — low-level Vaadin plumbing that is out of
  scope for a Spock unit spec and adds no value for the CSS-driven layout change. The oracle
  report explicitly labelled the Spock spec optional ("if the team wants regression
  protection"). **No broken/failing test file remains.**
- Full `datamanager-app` unit test suite: **229 tests run, 0 failures, 0 errors — BUILD SUCCESS.**

## Formatter status
- No automated formatter is configured (no `fmt-maven-plugin`, no Spotless, no prettier CLI,
  no formatter step in CI). `GoogleStyle.xml` is an IntelliJ config only. The Java change follows
  the file's existing Google-Java-Style conventions (2-space indent, 100-col wrap) manually;
  the CSS follows the file's existing token/comment conventions.

## Verification
- `./mvnw -q -pl datamanager-app -am compile -o` — passed.
- `./mvnw -pl datamanager-app -o test` — 229 tests pass, BUILD SUCCESS.
- `footer.css` is imported via `custom.css` → `styles.css`, so the rework is live with no
  other wiring needed.
- Runtime visual smoke check (spring-boot:run with `-Pdevelopment`) was **not** performed — the
  app requires MariaDB, Artemis, SMTP, vault config; starting it was not feasible in this
  environment. Recommend a manual/visual pass (≥1400px, 601–900px, ≤600px; light+dark; keyboard
  focus) before merge.

## Deviations from the oracle strategy (with justification)
1. **Spock spec dropped.** Oracle-labelled optional; blocked by Vaadin RouterLink/DownloadHandler
   plumbing that is unsupported by this repo's test setup (no testbench/mockito, no servlet
   mocks). Parent steering confirmed: "If anything still fights you after ONE more attempt, drop
   the spec entirely". Two attempts were made, then deleted — no broken test remains.
2. No other deviations. CSS matches the oracle's authoritative snippet; Java matches the oracle's
   target structure exactly.

## Diff summary
```
 .../themes/datamanager/components/footer.css       | 109 +++++++++++++++++++--
 .../views/general/footer/FooterComponent.java      |  77 ++++++++++++++-
 2 files changed, 175 insertions(+), 11 deletions(-)
```

## Open risks / next steps
- No automated regression protection for the footer DOM structure (spec dropped). If desired
  later, add a Vaadin TestBench/UI-unit test with the proper servlet harness as a separate task.
- Visual QA across breakpoints/themes is recommended before merge (requires the dev stack).
- `git status` shows other modified/untracked files owned by the parallel header worker —
  not part of this task.