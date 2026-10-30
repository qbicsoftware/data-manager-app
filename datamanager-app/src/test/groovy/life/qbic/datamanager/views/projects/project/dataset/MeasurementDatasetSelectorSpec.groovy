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

    @Unroll
    def "the header summarises across datasets instead of repeating their sum (#ngs.#pxp.#ip)"() {
        given: "an experiment with the given per-method measurement counts"
        def progress = progressWith(ngs, ngsWithData, ngsBytes, pxp, pxpWithData, pxpBytes,
                ip, ipWithData, ipBytes)

        expect: "the line used to print the total measurement count, which was exactly the sum of"
        // the cards beneath it — a duplicate dressed as a summary. It now carries what no single
        // card can: how many datasets exist and how much data has arrived in total.
        MeasurementDatasetMain.summaryLine(progress) == expected

        where:
        ngs | ngsWithData | ngsBytes | pxp | pxpWithData | pxpBytes | ip | ipWithData | ipBytes || expected
        6   | 2           | 1024L    | 0   | 0           | 0L       | 0  | 0           | 0L      || "1 dataset \u00b7 6 measurements \u00b7 1.00 KB"
        6   | 2           | 1024L    | 3   | 3           | 1024L    | 0  | 0           | 0L      || "2 datasets \u00b7 9 measurements \u00b7 2.00 KB"
        6   | 2           | 1024L    | 3   | 3           | 1024L    | 4  | 4           | 1024L   || "3 datasets \u00b7 13 measurements \u00b7 3.00 KB"
        6   | 0           | 0L       | 0   | 0           | 0L       | 0  | 0           | 0L      || "1 dataset \u00b7 6 measurements \u00b7 no data yet"
        6   | 2           | 0L       | 0   | 0           | 0L       | 0  | 0           | 0L      || "1 dataset \u00b7 6 measurements \u00b7 data size not recorded"
        0   | 0           | 0L       | 0   | 0           | 0L       | 0  | 0           | 0L      || "No measurements registered yet"
        1   | 1           | 1024L    | 0   | 0           | 0L       | 0  | 0           | 0L      || "1 dataset \u00b7 1 measurement \u00b7 1.00 KB"
    }

    @Unroll
    def "a card names itself a dataset (#segment -> #expected)"() {
        given: "an experiment measured by one or more methods"
        def selector = new MeasurementDatasetSelector()

        when:
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(segment, 6, 2, 1024L)])
        def label = firstLabelText(selector)

        then: "each card *is* a dataset, so a bare method name would read as a filter or category"
        label == expected

        where:
        segment                                           || expected
        MeasurementDatasetSegment.GENOMICS                || "Genomics dataset"
        MeasurementDatasetSegment.PROTEOMICS              || "Proteomics dataset"
        MeasurementDatasetSegment.IMMUNOPEPTIDOMICS       || "Immunopeptidomics dataset"
    }

    @Unroll
    def "count and size share one line, because both answer how big the dataset is (#total.#available.#bytes)"() {
        given:
        def selector = new MeasurementDatasetSelector()

        when:
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(
                MeasurementDatasetSegment.GENOMICS, total, available, bytes)])
        def line = quantitiesLineText(selector)

        then: "the count alone sat above the ring as its unexplained denominator; beside the size it"
        // is one of the two measures of the dataset's extent, and together they answer the question
        // a reader has before downloading.
        line == expected

        where:
        total | available | bytes          || expected
        6     | 2         | 1024L          || "6 measurements \u00b7 1.00 KB"
        1     | 1         | 1024L          || "1 measurement \u00b7 1.00 KB"
        6     | 0         | 0L             || "6 measurements \u00b7 No data yet"
        6     | 2         | 0L             || "6 measurements \u00b7 Size not recorded"
    }

    /** The quantities line of the selector's first card. */
    private static String quantitiesLineText(MeasurementDatasetSelector selector) {
        def cardsDiv = (com.vaadin.flow.component.html.Div) selector.getChildren().toList().first()
        def card = (com.vaadin.flow.component.html.Div) cardsDiv.getChildren().toList().first()
        // headline row, then the quantities span
        return ((com.vaadin.flow.component.html.Span) card.getChildren().toList().get(1)).getText()
    }

    @Unroll
    def "a card states the dataset size for download planning (#available.#bytes -> #expected)"() {
        given:
        def summary = new MeasurementDatasetSelector.DatasetSummary(
                MeasurementDatasetSegment.GENOMICS, 10, available, bytes)

        expect: "size decides whether the data fits the reader's disk and link, so it is stated —"
        // and the two cases that are not a number must not read as a free download.
        MeasurementDatasetSelector.sizeText(summary) == expected

        where:
        // FileSizeFormatter divides by 1024 while labelling the units KB/MB/GB, so these are
        // binary values under decimal names. The expectations follow the formatter, which is the
        // app-wide one and not this page's choice.
        available | bytes              || expected
        2         | 4_200_000_000L     || "3.91 GB"
        2         | 1024L              || "1.00 KB"
        0         | 0L                 || "No data yet"
        2         | 0L                 || "Size not recorded"
    }

    /** The label text of the selector's first card. */
    private static String firstLabelText(MeasurementDatasetSelector selector) {
        def cardsDiv = selector.getChildren().toList().first()
        def card = ((com.vaadin.flow.component.html.Div) cardsDiv)
                .getChildren().toList().first()
        def headlineRow = ((com.vaadin.flow.component.html.Div) card)
                .getChildren().toList().first()
        def headline = ((com.vaadin.flow.component.html.Div) headlineRow)
                .getChildren().toList().first()
        return ((com.vaadin.flow.component.html.Span) ((com.vaadin.flow.component.html.Div) headline)
                .getChildren().toList().first()).getText()
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
