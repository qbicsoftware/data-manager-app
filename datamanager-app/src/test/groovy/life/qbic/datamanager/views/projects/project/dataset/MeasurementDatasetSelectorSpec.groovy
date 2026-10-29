package life.qbic.datamanager.views.projects.project.dataset

import life.qbic.projectmanagement.application.query.AssayType
import life.qbic.projectmanagement.application.query.ExperimentProgress
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Pins the dataset selector's summary arithmetic and the measurement-code shortening.
 *
 * <p>Both are places where a plausible-looking value can be wrong without failing: the waiting
 * count is a subtraction that must never go negative, and a truncated identifier must still be
 * usable to tell two rows apart. The truncation is also the one display decision that hides
 * information, so it is tested rather than eyeballed.</p>
 */
class MeasurementDatasetSelectorSpec extends Specification {

    private static ExperimentProgress progressWith(int ngs, int ngsWithData, long ngsBytes,
                                                   int pxp, int pxpWithData, long pxpBytes,
                                                   int ip, int ipWithData, long ipBytes) {
        return new ExperimentProgress("exp-1", "proj-1", "Experiment",
                10, 5,
                ngs + pxp + ip,
                ngs, pxp, ip,
                ngsWithData + pxpWithData + ipWithData,
                ngsWithData, pxpWithData, ipWithData,
                ngsBytes + pxpBytes + ipBytes,
                ngsBytes, pxpBytes, ipBytes)
    }

    def "a dataset is emitted only for a method the experiment actually measured"() {
        given: "an experiment measured by genomics only"
        def progress = progressWith(6, 2, 4096L, 0, 0, 0L, 0, 0, 0L)

        when:
        def summaries = MeasurementDatasetSelector.DatasetSummary.of(progress)

        then: "an unmeasured method is not an empty dataset"
        summaries.size() == 1
        summaries.first().segment() == MeasurementDatasetSegment.GENOMICS
    }

    def "every measured method becomes its own dataset"() {
        given:
        def progress = progressWith(6, 2, 4096L, 3, 3, 100L, 0, 0, 0L)

        when:
        def summaries = MeasurementDatasetSelector.DatasetSummary.of(progress)

        then:
        summaries*.segment() == [MeasurementDatasetSegment.GENOMICS,
                                 MeasurementDatasetSegment.PROTEOMICS]
    }

    @Unroll
    def "waiting is total minus available and never negative (#total of #available)"() {
        given:
        def summary = new MeasurementDatasetSelector.DatasetSummary(
                MeasurementDatasetSegment.GENOMICS, total, available, 0L)

        expect: "a count that cannot go negative, whatever the read model reports"
        summary.waiting() == expected

        where:
        total | available || expected
        6     | 2         || 4
        6     | 6         || 0
        0     | 0         || 0
        1     | 0         || 1
        // Defensive: inconsistent upstream counts must not produce a negative "waiting" figure.
        2     | 5         || 0
    }

    def "a summary rejects negative counts rather than rendering nonsense"() {
        when:
        new MeasurementDatasetSelector.DatasetSummary(MeasurementDatasetSegment.GENOMICS, -1, 0, 0L)

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "a measurement code is shortened to a bounded width with an ellipsis (#code)"() {
        expect:
        MeasurementDatasetDetailsComponent.shortMeasurementCode(code) == expected

        where:
        code                                          || expected
        "NGSQ2IMMU721AB-2540000945958"                || "NGSQ2IMMU721AB-25\u2026"
        "MS-123"                                      || "MS-123"
        null                                          || "\u2014"
        ""                                            || "\u2014"
        "NGSQ2IMMU620AT-2540306555616"                || "NGSQ2IMMU620AT-25\u2026"
    }

    @Unroll
    def "a pooled row never renders a bare count without saying it is a pool (#poolLabel -> #expected)"() {
        given: "a pooled measurement, with and without a user-typed pool label"
        def row = new life.qbic.projectmanagement.application.query.MeasurementDataRow(
                "mid-1", "exp-1", "PROTEOMICS", "MS-1", "PX run", "QBiC", null,
                poolLabel, 50, java.time.Instant.EPOCH,
                life.qbic.projectmanagement.application.query.MeasurementDataRow.DataStatus.AVAILABLE,
                5L, 1024L, Set.of("raw"), java.time.Instant.EPOCH)

        when: "the rule is applied without needing a component instance"
        def rendered = MeasurementDatasetDetailsComponent.pooledCellText(row)

        then: "the word 'Pool' appears, so the pill is not unexplained different-looking text"
        rendered.startsWith("Pool")

        and: "the user's own label is used when they gave one"
        rendered == expected

        where:
        poolLabel || expected
        "group A"  || "Pool: group A"
        "2.0"      || "Pool: 2.0"
        null       || "Pool of 50"
        ""         || "Pool of 50"
    }

    def "a single dataset is rendered as a statement, not as a selected choice"() {
        given: "an experiment measured by one method only — the common case"
        def selector = new MeasurementDatasetSelector()

        when:
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(
                MeasurementDatasetSegment.GENOMICS, 6, 2, 0L)])

        then: "the card carries no radio role, because there is nothing to choose between"
        def card = cardsOf(selector).first()
        card.getElement().getAttribute("role") == null
        card.getClassNames().contains("measurement-dataset-card--single")

        and: "it is not marked as selected, which would claim a choice was made"
        !card.getClassNames().contains("measurement-dataset-card--selected")

        and: "the group is not announced as a set of options"
        !selector.getChildren().toList().first()
                .getElement().hasAttribute("role")
    }

    def "several datasets are rendered as a radio group"() {
        given:
        def selector = new MeasurementDatasetSelector()

        when:
        selector.setDatasets([
                new MeasurementDatasetSelector.DatasetSummary(
                        MeasurementDatasetSegment.GENOMICS, 6, 2, 0L),
                new MeasurementDatasetSelector.DatasetSummary(
                        MeasurementDatasetSegment.PROTEOMICS, 13, 2, 0L)])

        then: "now there is a choice, so the semantics and the affordance appear"
        def cards = cardsOf(selector)
        cards.size() == 2
        cards.every { it.getElement().getAttribute("role") == "radio" }

        and: "selecting one marks exactly that card"
        selector.selectSilently(MeasurementDatasetSegment.PROTEOMICS)
        cards[1].getClassNames().contains("measurement-dataset-card--selected")
        !cards[0].getClassNames().contains("measurement-dataset-card--selected")
    }

    def "the single card is never marked selected, even when one is activated"() {
        given:
        def selector = new MeasurementDatasetSelector()
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(
                MeasurementDatasetSegment.GENOMICS, 6, 2, 0L)])

        when: "the page activates its only dataset"
        selector.selectSilently(MeasurementDatasetSegment.GENOMICS)

        then: "the statement stays a statement"
        !cardsOf(selector).first().getClassNames()
                .contains("measurement-dataset-card--selected")
    }

    /** The dataset cards rendered by the selector. */
    private static List<com.vaadin.flow.component.html.Div> cardsOf(
            MeasurementDatasetSelector selector) {
        List<com.vaadin.flow.component.html.Div> found = []
        for (var child : selector.getChildren().toList()) {
            for (var card : ((com.vaadin.flow.component.html.Div) child).getChildren().toList()) {
                found.add((com.vaadin.flow.component.html.Div) card)
            }
        }
        return found
    }

    def "shortening two long codes keeps their distinguishing prefix readable"() {
        given: "two measurements of the same sample, differing only in the timestamp"
        def first = MeasurementDatasetDetailsComponent.shortMeasurementCode(
                "NGSQ2IMMU721AB-2540000945958")
        def second = MeasurementDatasetDetailsComponent.shortMeasurementCode(
                "NGSQ2IMMU721AB-2540000999999")

        expect: "the shortened forms are still distinguishable by their prefix"
        first.length() <= 18
        first.startsWith("NGSQ2IMMU")
        second.startsWith("NGSQ2IMMU")
    }
}
