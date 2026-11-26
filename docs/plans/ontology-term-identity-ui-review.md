## UI review — making a term's IRI reachable without shouting the CURIE

**Files reviewed:** `/Users/sven1103/git/data-manager/img_10.png` ("Edit experiment", setup), `/Users/sven1103/git/data-manager/img_8.png` ("Create experiment"). Both loaded fine.

### 1. What I see

Both screens: a left-aligned column of form fields on white, full-width tinted input rows, bold blue primary button top-right (`Save experiment metadata` in img_10; `Cancel` + `Create experiment` in img_8). Under **Experiment metadata** three stacked fields — **Species**, **Specimen**, **Analytes** — each dot-marked required, each holding **one** selected chip at the left of an otherwise empty grey field, with a chevron on the far right and the helper line "Type at least 2 letters to search." beneath. Chips are small tinted pills: the human label in bold blue (`Homo sp.`, `Liver`, `DNA`, `Acidithiobacillus sp. ORCS6`, `Whole Body Scan`) followed by a lighter grey parenthesised CURIE in two spans. Cropped sections (Experimental design, footer buttons) follow below.

### 2. Judging the current chip, honestly

- In `DNA (NCIT:C449)` the parenthesised CURIE is **as long as, or longer than, the label** — for the short, most common terms the noise wins the chip outright.
- `Acidithiobacillus sp. ORCS6 (NCBITaxon:304721)` reads as ~60% machine text. At a glance the eye parses the label, then hits a `:digit` punctuation cluster it cannot use while reading a form.
- The two-span split (bold blue label / lighter grey ID) **does help** — the grey is skippable, and the label still reads first. That is the one thing currently working. It is not enough: the characters still occupy width, and with 3 fields × several terms each you get a vertical column of `(...:NNNNN)` fragments running down the page — a staccato rhythm of colons and digits in what is otherwise a calm form. Visual cost estimate: ~9 parenthesised ID strings in one viewport at 3 terms per field, roughly half of all chip text mass rendered as unreadable-to-humans content. Repetition is what makes it grating, not any single chip.
- 🔴 **blocker (for the stated goal):** the chip answers a rare need (identifier) permanently and answers the common need (recognise my term) with noise.
- 🟡 The `✕` sits at the pill edge and reads slightly detached from the tint; inside a chip whose content is already two-tone, a third control tone adds ambiguity.
- 🟡 All six fields hold exactly **one** chip — the multi-term density case the complaint is really about is unproven by these captures.
- 🔵 Helper text "Type at least 2 letters to search." repeats under all three fields on img_10; it is redundant once a term is chosen.

### 3. Recommendation — ONE primary approach

**Make the term label inside the chip an external link to the term's full IRI, drop the parenthesised CURIE from the chip entirely, and move label + CURIE + ontology release into that link's tooltip/accessible name.** The external-link glyph (↗) after the label is the signal; the `✕` stays as-is for removal.

Why this and not the others:
- It satisfies the requirement literally: the identity *becomes the thing you click*, and the public term page is a strictly better answer than a string you retype into a browser.
- It removes ~half the chip text, so the same field row reads as a short list of human labels — exactly what img_10/img_8 look like they want to be.
- The tooltip regains the CURIE for power users at zero permanent cost, and can carry the currently-unused `description` and ontology version for free.

Explicitly rejected:
- **CURIE as a secondary smaller line** — the noise remains permanent (just smaller) and chip height roughly doubles; three stacked fields grow, pushing `Save`/`Create` context and the Experimental design section down. Fails the user's actual complaint.
- **Reveal ID only on focus/expansion (click the chip)** — invisible affordance, an extra click, and it collides with the `✕`/field-focus behaviour of a select. Undiscoverable on touch.
- **Hover tooltip alone, nothing clickable** — reachable but not resolvable; you still hand-copy an identifier. And a tooltip is nothing on a touch device. Acceptable only as a *fallback* if the link proves to break the combo box.
- **Per-term details popover** — heavier to build and dismiss; it duplicates what tooltip + term page already give. Good later home for synonyms/version, not the primary move.
- **Show `description` instead** — tempting ("Homo sp." / "human"), but it is not an identifier, so it does not meet "IRI is reachable"; and descriptions are often absent (likely for `Acidithiobacillus sp. ORCS6`), so chips become inconsistent in length and content.

Apply the same fix to the **dropdown rows** — there a two-line row (label bold, muted smaller CURIE), plus the ↗, is affordable and should be used: the list is where users pick, and the fix must not leave `label (CURIE)` noise only in the list.

### 4. Affordance and copy

- **Clickable:** the term label within the chip (and the row label in the dropdown). Opens the term's public IRI page in a new tab.
- **Signal:** persistent external-link glyph `↗` immediately after the label; hover/focus underline on the label; focus ring on the whole label for keyboard.
- **Tooltip (hover + focus):** line 1 `Homo sp.`; line 2 `NCBITaxon:2813599 · ontology release <version>`; line 3 `Open term page ↗`. Keep to three lines max.
- **Screen-reader name:** `Homo sp., NCBITaxon 2813599, opens the ontology term page in a new tab`.
- **Failure mode if never discovered:** the people who *need* the identifier still cannot get it, and everyone else loses the reassurance that a term is externally verifiable — the FAIR value of using public ontologies is silently deleted from the UI. Mitigation is the persistent glyph plus the always-visible focus ring; never rely on hover alone.
- **Watch the hit area:** a label-link plus `✕` inside a small pill risks mis-taps and sub-44px targets. Enlarge the pill's hit height or widen the gap between link and `✕`.

### 5. What these captures could NOT tell me — evidence needed next

1. **Hover state**: does a tooltip render, and is it clipped or overlapping the "Type at least 2 letters…" line when the chip sits at the field's bottom (all fields here look bottom-adjacent)?
2. **Focus ring + keyboard order** on a chip that now contains a link, a dropdown and a `✕`.
3. **Interaction risk**: does clicking the chip label open the link, or is the click swallowed by the combo box / does it focus the input? Needs a prototype before committing.
4. **Long-label behaviour** — no capture shows a label long enough to truncate or wrap; unknown whether the `↗`/ID survives ellipsis.
5. **Density**: a field with 5+ chips — do they wrap, and how tall does the three-field block become?
6. **Touch**: tooltip and hover are absent; confirm the link still works and is not the sole path to the ID.
7. **Dropdown row** with the new two-line treatment, and its contrast in the highlighted/selected state.
8. **Screen-reader output** for the composed chip, plus dark mode.

### Overall impression

The screens are clean and well-hierarchied; the chips are the one place where the UI talks to machines instead of people. Highest-impact single change: **strip the parenthesised CURIE from the chip, make the term label a link to the term's public page with an `↗`, and put label + CURIE + ontology release in its tooltip and accessible name** — the identity becomes reachable by design rather than displayed by default.