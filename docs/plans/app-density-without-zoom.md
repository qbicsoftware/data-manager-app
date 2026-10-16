# App density without the zoom

**Status:** proposal · **Owner:** TBD · **Story / Feature / ADR:** TBD

The app renders inside `zoom: 0.9` (`main.css`, `#main-layout`). This is a density choice: at 100%
the design's type and spacing carry less content than a project page needs. The zoom is a *hack* in
the specific sense that it states an accident ("everything is 10% too big") instead of an intent
("body text is 14.5px"), and it does so by creating a second coordinate space inside one
application.

This document is the plan to replace it: the mechanism, the token set, how to verify nothing was
missed, and the order.

---

## 1. What the zoom is, and what is inside it

```css
#main-layout { zoom: var(--app-zoom); }   /* --app-zoom: 0.9 */
```

`#main-layout` is the AppLayout **content slot** (`DataManagerLayout.setContent`). It holds:

| inside the zoom (renders at 90%) | outside (renders at 100%) |
|---|---|
| the announcement strip | the chrome — identity bar and navigation row |
| the routed page (all views) | the account menu |
| the footer | |
| the side drawers and dialogs' anchors | |

The chrome is outside because it is added with `addToNavbar`, which puts it in the **navbar slot** —
a sibling of `#main-layout`, not a descendant. So the app has had two type scales since the zoom was
introduced: the header renders at the design's sizes and the page at 90% of them. Nobody chose that;
it is a consequence of where the zoom is attached.

**Evidence that it applies** (from `img_4.png`, 1766px viewport): the chrome's content column measured
1366px, which fixes the device pixel ratio at 1 (1400 − 2×16 = 1368). The page content measured
**1230px**, and 1368 × 0.9 = 1231. Two centred columns, one 10% narrower than the other, is only
possible if the zoom applies to one side and not the other.

## 2. What it has cost

- **Three mechanisms exist only because of it:** `--app-zoom`, `--content-shell-rendered-width`,
  `--content-shell-rendered-gutter`, plus an `@supports (zoom: 1)` branch. `1400px` means two
  different widths depending on which side of the boundary it is written on, so the chrome's column
  has to be stated post-zoom.
- **Subpixel hairlines.** At 90%, a `1px` border is 0.9 device px: the renderer antialiases or snaps
  it, and two rules at different subpixel offsets can land differently. The design is full of
  hairlines.
- **Silent failure.** Firefox below 126 (ESR 115 included) parses `zoom` as unknown and ignores it,
  with no error and no fallback — which is why the `@supports` branch had to be written.
- **Cap ambiguity.** `--content-shell-max-width` does not mean what it says, anywhere.
- **A measurement gap for client-side code.** Inside a zoomed subtree `getBoundingClientRect()`
  returns visual (scaled) coordinates while `offsetWidth`/`clientWidth`/`scrollWidth` return the
  element's own unscaled units. Vaadin's overlays attach to `document.body` — outside the zoom — so
  anchor and overlay coordinates sit in different spaces.

## 3. The mechanism decision

Four options. The recommendation is **D, then C**.

### A. Keep the zoom (status quo)

Keeps completeness — it catches Vaadin's own web components by construction, which hand-written
tokens only catch if you remember every one — and keeps every aspect ratio exactly. Pays with §2.

### B. Hand-override the Lumo tokens at 0.9×

One coordinate space, expressed in the design's vocabulary. But it is the same multiplier written
out twenty times, it is only as complete as the list, and it has to be re-checked on every Lumo
upgrade. **Not recommended as the primary mechanism** — it is what C replaces.

### C. Change the root font size — *the mechanism*

```css
html { font-size: 90%; }
```

Lumo's size-carrying tokens are `rem`-based (§4), so this scales the type scale, the spacing scale
and the control-height scale *together* — the proportionality the zoom provides — in one standard
declaration. The `em`-based families (icons, radii) follow their element's font size automatically.

What it does better than the zoom:

| | `zoom: 0.9` | `html { font-size: 90% }` |
|---|---|---|
| type, spacing, control heights scale together | yes | yes |
| em-based icons and radii follow | yes | yes |
| hairlines stay 1px | **no** | **yes** |
| engines | Chrome/Edge/Safari/Opera/Firefox 126+ | all |
| needs an `@supports` fallback | **yes** | no |
| coordinate spaces | **two** | one |
| respects the user's browser font-size setting | no (scales px too) | **yes** — this is what `rem` is for |

Cost: anything expressed in `px` does not scale, so it needs an explicit decision (§5). That list is
short here — the theme's type is `rem`-driven except for a single `font-size: 12px`, and the
app's own spacing tokens are already `rem` — which is what makes C low-risk for this codebase
specifically, rather than merely tidier.

C is the structural fix, and it is also the prerequisite for a design one. Once the zoom is gone and
there is a single coordinate space, replacing the percentage with *chosen* `rem` values per token
(0.875rem body rather than 90% of 1rem, 0.8125rem small rather than 0.7875rem) is a mechanical
follow-up that states the intent outright. That refinement is B, done with `rem` values so it still
composes with the user's font-size preference — and it is much easier to make once C has removed the
second coordinate space. B is therefore the follow-up to C, not its alternative.

### D. Adopt Lumo's compact preset — *not a replacement; see the numbers*

Lumo ships one: `Lumo.COMPACT_STYLESHEET` (Vaadin ≥ 24,
`vaadin-lumo-theme-*/META-INF/resources/lumo/presets/compact.css`), applied with `@StyleSheet`. The
Vaadin docs describe it as reducing font sizes "and proportionally all Vaadin components".

It is proportional in a different sense than the zoom is. Against what the app renders today:

| family | compact | today (Lumo × 0.9) | delta |
|---|---|---|---|
| control heights `size-*` | 2 / 2.5 / 2.75rem | 2.25 / 2.75 / 3.5 × 0.9 | **within ±5%** |
| body and small type `m`/`s`/`xs` | 14 / 13 / 12px | 14.4 / 12.6 / 11.7px | **within ±3%** |
| display type `xl`/`xxl`/`xxxl` | 18 / 22 / 28px | 19.8 / 25.2 / 36px | −9% to −22% |
| `space-m` / `space-s` | 10 / 5px | 14.4 / 7.2px | **−31%** |
| `line-height-m` | 1.4 | 1.625 | −14% |

So it keeps the controls and the reading text where they are, tightens the padding by a third, and
shrinks the display sizes. That is a different *rhythm*, not the same design 10% smaller — the cards
would get visibly tighter while the buttons stayed the same size.

Worth ten minutes only if reconsidering the rhythm is on the table. It is not a drop-in for the
density the app has, and spending a screenshot round to prove that would be wasteful.

## 4. The token set

Exact defaults, read from `vaadin-lumo-theme-25.3.1.jar` (the version this repo resolves):

| family | unit | defaults | composes from root font size |
|---|---|---|---|
| `--lumo-font-size-{xxs…xxxl}` | rem | .75 / .8125 / .875 / 1 / 1.125 / 1.375 / 1.75 / 2.5 | yes |
| `--lumo-space-{xs…xl}` | rem | .25 / .5 / 1 / 1.5 / 2.5 | yes |
| `--lumo-size-{xs…xl}` | rem | 1.625 / 1.875 / 2.25 / 2.75 / 3.5 | yes |
| `--lumo-line-height-{xs,s,m}` | unitless | 1.25 / 1.375 / 1.625 | n/a — follows type |
| `--lumo-icon-size-{s,m,l}` | **em** | 1.25 / 1.5 / 2.25 | yes, via the element's font size |
| `--lumo-border-radius-{s,m,l}` | **em** | .25 / .25 / .5 | yes, via the element's font size |

The families the theme actually references, by count: `space-m` 197, `space-s` 162, `space-l` 97,
`space-xs` 87, `font-size-s` 81, `border-radius-m` 56, `font-size-xs` 26, `font-size-m` 23,
`icon-size-s` 20, `space-xl` 18, `icon-size-m` 17, `font-size-l` 17, `icon-size-l` 16,
`border-radius-s` 15, `font-size-xxl` 10, and the rest in single digits.

**Correction to record:** an earlier claim in this work said `--lumo-font-size-xs` renders at 10.8px
under the zoom. It is `0.8125rem` = 13px, so **11.7px**. (`10.8px` is `--lumo-font-size-xxs`.) The
conclusion — the small end drops below what it is for — stands; the figure was wrong.

The app's own tokens in `variables.css` are already `rem`-based (`--spacing-01…12`,
`--card-readable-width: 26rem`) and scale with C without change.

## 5. What does not compose, and needs an explicit decision

These are `px` and therefore unaffected by C. They are also exactly the places the zoom was
silently papering over, so each is a real decision rather than a mechanical conversion.

| what | where | decision |
|---|---|---|
| `--content-shell-max-width: 1400px` | `variables.css` | restate as the intended rendered width (1260px today) — this is the one that makes `--content-shell-rendered-*` unnecessary |
| px type | **exactly one**: `font-size: 12px` in `all.css:853` | convert to a type token, or keep deliberately. There is no other px type in the theme, which is why option C reproduces the zoom's type almost exactly |
| fixed px box sizes, ~23 declarations | `all.css` (`250px`, `1000px`), `card.css` (`250px`, `300px`), `dialog.css` (`175px`, `400px`, `700px`), `connect-dataset-sidebar.css` (`460px`, `720px`), `page-area.css`, `toast.css`; plus `max-width` at `600px`, `900px`, `1023px`, `1024px`, `1200px`, `1400px` and `min-width` at `380–480px` | decide per case: some should scale with density, some are viewport-proportional on purpose |
| `px` media-query breakpoints | throughout | do not scale; re-test around each one, because denser content reaches overflow sooner |
| `1px`/`2px` hairlines and borders (the bulk of 332 `px` literals) | throughout | **leave as `px`** — this is the improvement, not a gap |

## 6. Verification method

The migration is only safe if it can be *proved* complete. Both mechanisms agree on everything except
the `px` literals, so a screenshot diff is a complete finding list rather than noise.

1. **Baseline.** Run with the zoom. Capture every surface at one fixed viewport (1440×900, DPR 1):
   project list, summary, datasets, settings, access, experiment overview, samples, measurements,
   raw data, groups, login — plus one dialog open and one dropdown open (the overlay positioning is
   the one place the coordinate-space change could bite).
2. **Mechanical token check.** For each size-carrying token the theme references, read
   `getComputedStyle` on a probe element before and after. They must be identical. This catches a
   token that did not scale in seconds, without looking at anything.
3. **No `px` type.** `grep -rnE "font-size: *[0-9.]+px"` over the theme must be empty, or every hit
   must be a recorded decision from §5.
4. **Screenshot diff.** Classify every difference: hairline thickness (expected, better), the shell
   column (expected, restated), anything else — wrapping changes, boxes off by more than 1px,
   clipped text, overlaps — is a finding.
5. **Breakpoint sweep.** Step across each `px` media query ±1px; confirm nothing overflows.

### Acceptance criteria

- no `zoom` declaration anywhere in the theme;
- `--content-shell-rendered-width`, `--content-shell-rendered-gutter` and the `@supports` branch
  deleted;
- `--content-shell-max-width` is one literal value that means what it says;
- nothing renders below the agreed minimum text size (decide it — today `-xs` renders at 11.7px);
- hairlines render at 1px;
- the chrome and the page share one type scale, so the header/page inconsistency cannot exist;
- every page passes the diff with only expected differences;
- the app renders identically in Firefox ≤125 and ≥126 — the silent-failure class is gone.

## 7. Order

1. **Decide the density.** `--app-zoom: 0.9` was never chosen; it was inherited. Pick the number the
   app should render at, and record it as a decision rather than a default. Everything below depends
   on it.
2. **Baseline capture** (§6.1).
3. **Consider the compact preset** only if reconsidering the rhythm is wanted (§3 D — it is not a
   replacement for the current density). Otherwise skip to 4.
4. **Set the root size** (§3 C) and restate the `px` geometry (§5) — these are one change, because
   the second is unreadable without the first.
5. **Delete the zoom and its three supporting mechanisms.**
6. **Verify** (§6), then land it as its own PR.
7. **Only then** continue the remaining project-surface reworks. Every stylesheet written under the
   zoom inherits its ambiguity, so the surfaces still to do are the cheapest they will ever be to
   move — and the most expensive if this lands after them.

## 8. Risks and rollback

- **Risk: an unforeseen layout regression on a surface nobody captured.** Mitigated by §6.1 covering
  every surface including overlays.
- **Risk: the compact preset changes proportions unacceptably.** It is a branch, discarded on
  evidence.
- **Rollback** is small and mechanical: re-add `zoom: var(--app-zoom)` on `#main-layout`, re-add the
  `@supports` block, restore `--content-shell-rendered-*`. Landing this as its own PR, separate from
  UI work, is what makes that clean.

## 9. Open questions

1. **What is the intended density?** The number to settle first (§7.1).
2. **Should the chrome share the page's scale?** Today it does not, by accident. Now that it would be
   a deliberate choice rather than a coordinate-space side effect, 14px navigation and a 14px user
   name may be fine at the app's own scale.
3. **Are the `px` breakpoints still right?** Denser content reaches overflow sooner.
4. **Governance.** Per AGENTS §12, an architectural decision of this kind is ADR territory and needs
   human approval before an ADR is created and the index in `docs/adr/README.md` is updated. Decide
   whether this is recorded as an ADR, a Story, or both, before implementation.
