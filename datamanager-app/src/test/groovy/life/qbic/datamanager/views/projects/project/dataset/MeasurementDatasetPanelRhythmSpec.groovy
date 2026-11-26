package life.qbic.datamanager.views.projects.project.dataset

import spock.lang.Specification

/**
 * Pins the detail panel's spacing rhythm and de-duplication rules.
 *
 * <p>The panel was reported as "hard to scan", and the cause was measurable rather than aesthetic:
 * the gap between two fields was 4x the gap between a label and its value, while the gap between
 * two groups was only 2x the gap between two fields. The layout therefore announced that every line
 * was an equal peer, and the group headings were just more lines. Nothing threw, and no screenshot
 * review of an individual value could show it — only the ratio between the three distances.
 *
 * <p>These tests read the stylesheet and assert the ordering directly, because the next plausible
 * edit — "the groups need more padding" — can silently restore the inversion.</p>
 */
class MeasurementDatasetPanelRhythmSpec extends Specification {

    /** Lumo's spacing scale, in rem. Not redeclared by this theme, so named here. */
    private static final Map<String, Double> LUMO_SPACING = [
            "--lumo-space-xs": 0.25d,
            "--lumo-space-s" : 0.5d,
            "--lumo-space-m" : 1.0d,
            "--lumo-space-l" : 1.5d,
            "--lumo-space-xl": 2.5d
    ]

    private static String variablesCss() {
        def candidates = [
                new File("datamanager-app/frontend/themes/datamanager/components/variables.css"),
                new File("frontend/themes/datamanager/components/variables.css"),
                new File("../datamanager-app/frontend/themes/datamanager/components/variables.css")
        ]
        def found = candidates.find { it.exists() }
        assert found != null: "variables.css not found from ${new File('.').absolutePath}"
        return found.text
    }

    private static String css() {
        def candidates = [
                new File("datamanager-app/frontend/themes/datamanager/components/measurement-dataset.css"),
                new File("frontend/themes/datamanager/components/measurement-dataset.css"),
                new File("../datamanager-app/frontend/themes/datamanager/components/measurement-dataset.css")
        ]
        def found = candidates.find { it.exists() }
        assert found != null: "measurement-dataset.css not found from ${new File('.').absolutePath}"
        return found.text
    }

    /** Extracts the declaration block of one selector, or null when the selector is absent. */
    private static String blockOf(String selector) {
        String text = css()
        int start = text.indexOf(selector + " {")
        if (start < 0) {
            return null
        }
        int end = text.indexOf("}", start)
        return text.substring(start, end)
    }

    private static String remOf(String block, String property) {
        if (block == null) {
            return null
        }
        def quoted = java.util.regex.Pattern.quote(property)
        def matcher = (block =~ /(?m)^\s*${quoted}\s*:\s*([0-9.]+)rem;/)
        return matcher.find() ? matcher.group(1) : null
    }

    /**
     * Reads a spacing value, resolving the theme's named tokens to rem so a gap
     * expressed as a token can still be compared with one expressed in rem.
     */
    private static Double spacingOf(String block, String property) {
        if (block == null) {
            return null
        }
        def quoted = java.util.regex.Pattern.quote(property)
        def matcher = (block =~ /(?m)^\s*${quoted}\s*:\s*([^;]+);/)
        if (!matcher.find()) {
            return null
        }
        String raw = matcher.group(1).trim()
        def remMatcher = (raw =~ /^([0-9.]+)rem$/)
        if (remMatcher.find()) {
            return remMatcher.group(1) as Double
        }
        // A sub-rem distance written in px (the label-to-value gap is 2px on purpose: it must be
        // smaller than any spacing token). Normalise to rem so the ratios stay comparable.
        def pxMatcher = (raw =~ /^([0-9.]+)px$/)
        if (pxMatcher.find()) {
            return (pxMatcher.group(1) as Double) / 16.0
        }
        def tokenMatcher = (raw =~ /var\((--[a-z0-9-]+)\)/)
        if (tokenMatcher.find()) {
            String token = tokenMatcher.group(1)
            // Tokens come from two places: the theme's own scale (--spacing-0N) and Lumo's built-in
            // scale (--lumo-space-*). The latter is not redeclared in this theme's variables.css, so
            // resolve it from its documented rem value instead of looking for a declaration.
            // Declarations live in either the page stylesheet or the theme's variables file.
            for (String source : [css(), variablesCss()]) {
                def declared = (source =~ /(?m)^\s*${java.util.regex.Pattern.quote(token)}\s*:\s*([0-9.]+)rem;/)
                if (declared.find()) {
                    return declared.group(1) as Double
                }
            }
            def lumo = LUMO_SPACING[token]
            if (lumo != null) {
                return lumo
            }
        }
        return null
    }

    def "a field's label sits closer to its value than the next field does"() {
        given: "the field's internal gap and its own gap to the next field"
        def fieldBlock = blockOf(".measurement-data-detail-field")
        def fieldsBlock = blockOf(".measurement-data-detail-fields")

        expect:
        fieldBlock != null
        fieldsBlock != null

        and: "the label/value gap is smaller than the field-to-field gap"
        def inner = spacingOf(fieldBlock, "gap")
        // the container that stacks fields owns the field-to-field distance
        def between = spacingOf(fieldsBlock, "gap")
        inner != null
        between != null
        inner < between
    }

    def "a group boundary is wider than a field boundary"() {
        given:
        def fieldsBlock = blockOf(".measurement-data-detail-fields")
        def panelBody = blockOf(".measurement-data-detail-panel-body")
        def groupSeparation = blockOf(".measurement-data-detail-group + .measurement-data-detail-group")

        expect: "field-to-field"
        def fieldGap = spacingOf(fieldsBlock, "gap")

        and: "the group-to-group distance, which is the panel gap plus the extra padding"
        def panelGap = spacingOf(panelBody, "gap")
        def groupPadding = spacingOf(groupSeparation, "padding-top")
        fieldGap != null
        panelGap != null
        groupPadding != null

        and: "the group boundary is the wider one"
        // The separator adds padding on top of the flex gap; both count.
        (panelGap + groupPadding) > fieldGap
    }

    def "consecutive groups are separated by a hairline as well as whitespace"() {
        expect: "a short group next to a short group still reads as two groups"
        css().contains(".measurement-data-detail-group + .measurement-data-detail-group")
        blockOf(".measurement-data-detail-group + .measurement-data-detail-group")
                .contains("border-top:")
    }

    def "group titles are visually distinct from field labels"() {
        given:
        def title = blockOf(".measurement-data-detail-group-title")
        def label = blockOf(".measurement-data-detail-field-label")

        expect: "uppercase plus tracking, not weight alone at 12px"
        title.contains("text-transform: uppercase")
        title.contains("letter-spacing:")

        and: "the field label stays sentence case"
        !label.contains("text-transform")
    }

    def "values are larger than their labels so the value column reads as the content"() {
        given:
        def label = blockOf(".measurement-data-detail-field-label")
        def value = blockOf(".measurement-data-detail-field-value")

        expect:
        label.contains("--lumo-font-size-xs")
        value.contains("--lumo-font-size-m")
    }

    def "the sample listing is capped and scrolls rather than pushing the data section away"() {
        given:
        def list = blockOf(".measurement-data-detail-samples-list")

        expect: "a pool of fifty must not make the panel's data section unreachable"
        list != null
        list.contains("max-height:")
        list.contains("overflow-y: auto")
    }

    def "the close control has a hit area large enough to use"() {
        given:
        def close = blockOf(".measurement-data-detail-panel-close")

        expect: "an unadorned glyph is well under the minimum target size"
        close != null
        close.contains("min-width:")
        close.contains("min-height:")
    }
}
