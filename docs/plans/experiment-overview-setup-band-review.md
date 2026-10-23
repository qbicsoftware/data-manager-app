**Reviewed:** `/Users/sven1103/git/data-manager/img_11.png` (single capture, one pass).

## 1. What I see

A white page on the Lumo/datamanager theme. Top-left, a large bold page title **"Overview"**. Below it, two large rounded white cards with a thin light border and generous internal padding, stacked with equal gaps. **Band 1 "Setup"**: a bold band heading, then a full-width grey explanatory sentence ("The experiment name and its sample origins. The origins become the Species, Specimen and Analyte options in the sample-registration template."), then a four-column grid of bold field labels — `Experiment name` / `Species` / `Specimen` / `Analyte` — each with a regular-weight grey value beneath: a name that wraps to three lines ("This is a very awesome experiment to find out, how the wizard fell down to Middle Earth"), `Petunia sp.` / `Homo sp.` / `Mus bufo` stacked on three lines, `Liver`, `DNA`. Below, a solid primary-blue button **`Edit setup`** aligned to the card's left content edge. **Band 2 "Design"**: bold heading `Design` with an inline pale-green pill **`Groups defined`** to its right, *no* explanatory sentence, a three-column grid — `Experimental groups` (`— 0 samples registered`), `Variables` (`Test (100)`), `Confounding variables` (`None yet`) — and a second solid primary-blue button **`Edit design`** at the same left edge. At the very bottom edge a **third card's top border is clipped**.

## 2. Setup band as "read what I configured"

- **Label→value binding works** for the two single-value columns: bold label, small gap, plain value directly beneath, left edges flush. `Liver` and `DNA` are unambiguous.
- **`Experiment name` is the weak case.** A three-line wrapped paragraph under a label reads as body copy, not as a value; the wrap width is dictated by the column (much narrower than the card), so the name breaks mid-phrase ("to find / out, how…"). A long name will keep growing the band vertically — this is the item most likely to look broken with real data.
- **`Species` reads as a wrapped sentence, not a list.** Three species on three lines with no bullets, commas, separators or ordering cue. A reader can mis-read "Homo sp." as continuation text. This is the field most likely to be misread, and it's also the field where a per-item identifier affordance (info icon / link / hover) would be expected — I see **no affordance of any kind** on the ontology-backed values, so the "reachable, but don't print the raw ID" intent is currently invisible on this page.
- **Four-column grid holds, but lopsidedly**: the four columns are equal width, yet columns 3 and 4 hold 5 and 3 characters while column 1 wraps to three lines. The right ~quarter of the grid is empty dead space, so the band reads top-left-heavy.
- Labels and band heading are close in size/weight — the band boundary is carried by the card border, not by typography (see §3).

## 3. Sibling grammar — inconsistencies

- **Different column counts and widths.** Setup: four columns; Design: three wider columns. None of Design's column starts line up with Setup's, so the two bands share a card style but **not a grid rhythm** — the page has no vertical lines the eye can track.
- **Asymmetric heading row.** `Design` carries a state tag and no note; `Setup` carries a note and no tag. Consequence: the first content row of each band sits at a different offset from its heading, and the *second* band gets the only colour on the page.
- **State tag placement/meaning.** The green pill sits beside the heading, slightly below the heading's cap line, and green conventionally signals success/validation while it actually means "groups exist". It is also the only coloured element besides the two buttons, so **the eye lands on the second band first** — inverted reading order on a "read the page top to bottom" surface. A neutral/grey tag, or none, would fix this.
- **Button placement is consistent** (bottom-left of both bands, same size, same style) — this part reads well as a sibling pattern, though the vertical position differs because Setup's content is taller.
- Purpose text exists only on Setup; if "the note" is the band's pattern, Design needs an equivalent (or Setup's should be dropped and hoisted next to the page title). Today the two bands are "note band" vs "tag band".

## 4. The two buttons

- **Two equally-emphasised primary buttons on a read-only page is wrong.** The strongest, most saturated elements on the screen are the actions, not the content — the hierarchy is inverted for a page whose only job is to be read. They also compete with each other: nothing tells the reader which edit is the "next" thing.
- **Labels are adequate but jargon-y.** `Edit setup` / `Edit design` mirror the band headings and are clearly distinguishable *as destinations* — good. But "setup"/"design" are internal surface names; `Edit experiment setup` and `Edit experimental design` would be self-describing when seen out of context (e.g. tab title, screen reader list of buttons).
- **Suggested emphasis:** make both non-primary (tertiary/quiet/outline) at band level, or keep exactly one primary if one action is genuinely the expected next step after reading; or offer "Edit" as a per-band header affordance. Two solid blues implies "choose one of two equal next steps", which is a decision the page doesn't need to force.

## 5. Broken / accidental-looking

- 🟠 **`— 0 samples registered`**: the leading em dash is unexplained and reads like an empty-value marker, but this is a real count. It undercuts the one piece of data-driven content in the band.
- 🟠 **`Test (100)`**: bare parenthetical number with no unit — 100 what? (variables, entries, values). Ambiguous without hovering.
- 🟠 **`Species` as three undifferentiated lines** (see §2) — the most likely genuine misread on the page.
- 🟡 **Green `Groups defined`** conveys success but means existence; only colour on page pulls focus to the second band.
- 🟡 **Long, narrow, 3-line experiment name** — wrap dictated by column width, not readability.
- 🟡 **Clipped third card at the bottom edge** — the page is not "two bands"; content continues. The screenshot looks unfinished, and the bottom band's identity/spacing is unverifiable.
- 🔵 Right-quarter dead space in both grids; grid weight sits top-left in an otherwise centred card.

## 6. Not knowable from one capture

- An experiment with **no origins set** (all three of Species/Specimen/Analyte empty) — does the band collapse, show placeholders, or look broken?
- A **very long term list per field** (e.g. 12 species, 8 analytes): does the grid wrap into rows, truncate, or push the button off-card?
- The **empty-experiment state** and what the third (clipped) band is.
- **Hover/focus/hover-to-reveal** behaviour on ontology-backed values (the identifier affordance, if any), and keyboard focus ring on both primary buttons.
- **Narrow viewport**: does the four-column grid reflow or produce horizontal scroll? Does the green tag drop under the heading?
- Dark mode; and whether clicking `Edit setup`/`Edit design` gives any confirmation that the read-only page is left (route naming parity).

## Overall

The bands are clean and legible, and the label→value pattern does its job for short values. The page fails as a *reading* surface in three ways: **actions out-shout content** (two solid primary blues), **the second band out-shouts the first** (the only coloured tag), and **the two bands don't share a grid** so they read as cousins rather than siblings. Highest-impact single change: **demote both edit buttons to quiet/tertiary and make the state tag neutral**, which immediately re-centres the eye on the configured values — then fix the `Species` list and the em-dash count inside Setup.