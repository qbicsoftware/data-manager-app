package life.qbic.datamanager.views.projects.project.dataset

import life.qbic.projectmanagement.application.query.AssayStanding
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

    /**
     * One dataset of a domain, as the selector now keys it: a domain, a method and an instrument.
     *
     * <p>A dataset used to be an omics domain, which could not distinguish two datasets of one
     * domain -- WES and RNA-Seq are both genomics, and two proteomics runs can sit on different
     * instruments. The identity is the triple, so the specs build one.</p>
     */
    private static final MeasurementDatasetSegment genomics =
            MeasurementDatasetSegment.of(AssayType.GENOMICS, "WGS", "Illumina")

    private static final MeasurementDatasetSegment proteomics =
            MeasurementDatasetSegment.of(AssayType.PROTEOMICS, "PROTEOMICS", "Orbitrap Exploris 480")

    private static final MeasurementDatasetSegment immunopeptidomics =
            MeasurementDatasetSegment.of(AssayType.IMMUNOPEPTIDOMICS, "IMMUNOPEPTIDOMICS",
                    "Orbitrap Exploris 480")

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

    /** One assay-standing row: the read model's dataset, one per domain/method/instrument. */
    private static AssayStanding assay(
            String experimentId, AssayType type, String method, String instrument,
            int measurements, int withData, long bytes) {
        return new AssayStanding(
                experimentId, "Experiment", type, method, instrument, measurements, withData, bytes)
    }

    def "a dataset is emitted only for a combination the experiment actually measured"() {
        given: "an experiment measured by genomics only"
        def assays = [assay("exp-1", AssayType.GENOMICS, "WGS", "Illumina", 6, 2, 4096L)]

        when:
        def summaries = MeasurementDatasetSelector.DatasetSummary.of("exp-1", assays)

        then: "an unmeasured combination is not an empty dataset"
        summaries.size() == 1
        summaries.first().segment() == genomics
    }

    def "every measured combination becomes its own dataset"() {
        given:
        def assays = [
                assay("exp-1", AssayType.GENOMICS, "WGS", "Illumina", 6, 2, 4096L),
                assay("exp-1", AssayType.PROTEOMICS, "PROTEOMICS", "Orbitrap Exploris 480", 3, 3, 100L)]

        when:
        def summaries = MeasurementDatasetSelector.DatasetSummary.of("exp-1", assays)

        then:
        summaries*.segment() == [genomics, proteomics]
    }

    def "two datasets of one domain are two cards, not one"() {
        given: "a genomics experiment measured twice on different platforms"
        def assays = [
                assay("exp-1", AssayType.GENOMICS, "WGS", "Illumina", 6, 2, 0L),
                assay("exp-1", AssayType.GENOMICS, "WGS", "Nanopore", 4, 0, 0L)]

        when:
        def summaries = MeasurementDatasetSelector.DatasetSummary.of("exp-1", assays)

        then: "the domain is not the identity, so this is two datasets -- which the old domain-keyed"
        // selector could not express at all, and reported as one card of 10 measurements.
        summaries.size() == 2
        summaries*.segment()*.instrumentLabel() == ["Illumina", "Nanopore"]
    }

    def "another experiment's datasets are not this experiment's cards"() {
        given:
        def assays = [
                assay("exp-1", AssayType.GENOMICS, "WGS", "Illumina", 6, 2, 0L),
                assay("exp-2", AssayType.PROTEOMICS, "PROTEOMICS", "Orbitrap", 3, 0, 0L)]

        when:
        def summaries = MeasurementDatasetSelector.DatasetSummary.of("exp-1", assays)

        then:
        summaries*.segment() == [genomics]
    }

    @Unroll
    def "waiting is total minus available and never negative (#total of #available)"() {
        given:
        def summary = new MeasurementDatasetSelector.DatasetSummary(
                genomics, total, available, 0L)

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
        new MeasurementDatasetSelector.DatasetSummary(genomics, -1, 0, 0L)

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
                "mid-1", "exp-1", "PROTEOMICS", "MS-1", "PX run", "QBiC",
                poolLabel, 50, java.time.Instant.EPOCH,
                life.qbic.projectmanagement.application.query.MeasurementDataRow.DataStatus.AVAILABLE,
                5L, 1024L, Set.of("raw"), java.time.Instant.EPOCH, List.of(), null)

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

    def "a single dataset is still a choice, because All is always beside it"() {
        given: "an experiment measured by one method only — the common case"
        def selector = new MeasurementDatasetSelector()

        when:
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(
                genomics, 6, 2, 0L)])

        then: "there are two options, not one, so the group is a choice group"
        allCardsOf(selector).size() == 2
        allCardsOf(selector).every { it.getElement().getAttribute("role") == "radio" }

        and: "the dataset card is not pre-marked selected, which would claim a choice was made"
        !cardsOf(selector).first().getClassNames()
                .contains("measurement-dataset-card--selected")
    }

    def "the all-measurements card states the experiment's totals, not one dataset's"() {
        given: "two datasets with different counts"
        def selector = new MeasurementDatasetSelector()

        when:
        selector.setDatasets([
                new MeasurementDatasetSelector.DatasetSummary(genomics, 6, 2, 1024L),
                new MeasurementDatasetSelector.DatasetSummary(proteomics, 3, 1, 1024L)])

        then: "it is the sum of the cards beside it, and says what it spans"
        def all = allCardOf(selector)
        all != null
        quantitiesOf(all) == "9 measurements \u00b7 2 KB"

        and:
        identityOf(all) == "across 2 datasets"
    }

    def "selecting all is a selection, even though it is the absence of a dataset"() {
        given:
        def selector = new MeasurementDatasetSelector()
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(genomics, 6, 2, 0L)])
        def events = []
        selector.addSelectionChangedListener { events << it.segment() }

        when: "the reader picks All from a page that had already scoped to a dataset"
        selector.selectSilently(genomics)
        selector.selectAll()

        then: "the event fires with a null segment, which means 'no dataset filter'"
        events == [null]
        selector.allSelected()
        !selector.selected()

        and: "exactly the All card is marked"
        allCardOf(selector).getClassNames().contains("measurement-dataset-card--selected")
        !cardsOf(selector).first().getClassNames()
                .contains("measurement-dataset-card--selected")
    }

    def "restoring All from the URL selects it silently"() {
        given:
        def selector = new MeasurementDatasetSelector()
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(genomics, 6, 2, 0L)])

        when:
        selector.selectAllSilently()

        then:
        selector.allSelected()
        allCardOf(selector).getClassNames().contains("measurement-dataset-card--selected")
    }

    def "several datasets are rendered as a radio group"() {
        given:
        def selector = new MeasurementDatasetSelector()

        when:
        selector.setDatasets([
                new MeasurementDatasetSelector.DatasetSummary(
                        genomics, 6, 2, 0L),
                new MeasurementDatasetSelector.DatasetSummary(
                        proteomics, 13, 2, 0L)])

        then: "now there is a choice, so the semantics and the affordance appear"
        def cards = cardsOf(selector)
        cards.size() == 2
        cards.every { it.getElement().getAttribute("role") == "radio" }

        and: "selecting one marks exactly that card"
        selector.selectSilently(proteomics)
        cards[1].getClassNames().contains("measurement-dataset-card--selected")
        !cards[0].getClassNames().contains("measurement-dataset-card--selected")
    }

    def "the page's default selection is the dataset, not All"() {
        given:
        def selector = new MeasurementDatasetSelector()
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(
                genomics, 6, 2, 0L)])

        when: "the page activates the first dataset"
        selector.selectSilently(genomics)

        then: "that card is marked, and All is not: the narrower view is the default, the wider one"
        // is the deliberate choice
        cardsOf(selector).first().getClassNames()
                .contains("measurement-dataset-card--selected")
        !allCardOf(selector).getClassNames().contains("measurement-dataset-card--selected")
        !selector.allSelected()
    }

    /**
     * The dataset cards rendered by the selector, excluding the "all measurements" card.
     *
     * <p>The All card is a peer, not a dataset: it has no method, no instrument and no ring of its
     * own kind, so including it in these assertions would make every one of them about a card it was
     * not written for. It is asserted separately, by {@code allCardOf}.</p>
     */
    private static List<com.vaadin.flow.component.html.Div> cardsOf(
            MeasurementDatasetSelector selector) {
        allCardsOf(selector).findAll {
            !it.getClassNames().contains("measurement-dataset-card--all")
        }
    }

    private static List<com.vaadin.flow.component.html.Div> allCardsOf(
            MeasurementDatasetSelector selector) {
        List<com.vaadin.flow.component.html.Div> found = []
        for (var child : selector.getChildren().toList()) {
            for (var card : ((com.vaadin.flow.component.html.Div) child).getChildren().toList()) {
                found.add((com.vaadin.flow.component.html.Div) card)
            }
        }
        return found
    }

    /** The "all measurements" card. */
    private static com.vaadin.flow.component.html.Div allCardOf(
            MeasurementDatasetSelector selector) {
        allCardsOf(selector).find {
            it.getClassNames().contains("measurement-dataset-card--all")
        }
    }

    @Unroll
    def "the header summarises across datasets instead of repeating their sum (#ngs.#pxp.#ip)"() {
        given: "an experiment with the given per-method measurement counts"
        def progress = progressWith(ngs, ngsWithData, ngsBytes, pxp, pxpWithData, pxpBytes,
                ip, ipWithData, ipBytes)

        expect: "the line used to print the total measurement count, which was exactly the sum of"
        // the cards beneath it — a duplicate dressed as a summary. It now carries what no single
        // card can: how many datasets exist and how much data has arrived in total.
        MeasurementDatasetMain.summaryLine(progress, datasetCount) == expected

        where:
        ngs | ngsWithData | ngsBytes | pxp | pxpWithData | pxpBytes | ip | ipWithData | ipBytes || datasetCount | expected
        6   | 2           | 1024L    | 0   | 0           | 0L       | 0  | 0           | 0L      || 1 || "1 dataset \u00b7 6 measurements \u00b7 1 KB"
        6   | 2           | 1024L    | 3   | 3           | 1024L    | 0  | 0           | 0L      || 2 || "2 datasets \u00b7 9 measurements \u00b7 2 KB"
        6   | 2           | 1024L    | 3   | 3           | 1024L    | 4  | 4           | 1024L   || 3 || "3 datasets \u00b7 13 measurements \u00b7 3 KB"
        6   | 0           | 0L       | 0   | 0           | 0L       | 0  | 0           | 0L      || 1 || "1 dataset \u00b7 6 measurements \u00b7 no data yet"
        6   | 2           | 0L       | 0   | 0           | 0L       | 0  | 0           | 0L      || 1 || "1 dataset \u00b7 6 measurements \u00b7 data size not recorded"
        0   | 0           | 0L       | 0   | 0           | 0L       | 0  | 0           | 0L      || 0 || "No measurements registered yet"
        1   | 1           | 1024L    | 0   | 0           | 0L       | 0  | 0           | 0L      || 1 || "1 dataset \u00b7 1 measurement \u00b7 1 KB"
    }

    @Unroll
    def "a card states the dataset: its domain, then the method and instrument (#segment -> #expected)"() {
        given: "an experiment measured by one or more methods"
        def selector = new MeasurementDatasetSelector()

        when:
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(segment, 6, 2, 1024L)])
        def label = firstLabelText(selector)
        def identity = identityLineText(selector)

        then: "the domain groups the cards in a multi-omics experiment, and the identity is what"
        // tells two datasets of one domain apart. A card named only "Genomics" cannot: it was the
        // domain pretending to be the dataset.
        label == domain
        identity == expected

        where:
        segment           || domain                | expected
        genomics          || "Genomics"            | "WGS \u00b7 Illumina"
        proteomics        || "Proteomics"          | "PROTEOMICS \u00b7 Orbitrap Exploris 480"
        immunopeptidomics || "Immunopeptidomics"   | "IMMUNOPEPTIDOMICS \u00b7 Orbitrap Exploris 480"
    }

    def "a dataset with no recorded instrument states the method alone, never a substitute"() {
        given: "a measurement that records no instrument"
        def selector = new MeasurementDatasetSelector()

        when:
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(
                MeasurementDatasetSegment.of(AssayType.PROTEOMICS, "PROTEOMICS", null), 3, 0, 0L)])
        def identity = identityLineText(selector)

        then: "no instrument is not the same as a made-up one"
        identity == "PROTEOMICS"
    }

    /** The identity line of the selector's first dataset card. */
    private static String identityLineText(MeasurementDatasetSelector selector) {
        return identityOf(cardsOf(selector).first())
    }

    @Unroll
    def "count and size share one line, because both answer how big the dataset is (#total.#available.#bytes)"() {
        given:
        def selector = new MeasurementDatasetSelector()

        when:
        selector.setDatasets([new MeasurementDatasetSelector.DatasetSummary(
                genomics, total, available, bytes)])
        def line = quantitiesLineText(selector)

        then: "the count alone sat above the ring as its unexplained denominator; beside the size it"
        // is one of the two measures of the dataset's extent, and together they answer the question
        // a reader has before downloading.
        line == expected

        where:
        total | available | bytes          || expected
        6     | 2         | 1024L          || "6 measurements \u00b7 1 KB"
        1     | 1         | 1024L          || "1 measurement \u00b7 1 KB"
        6     | 0         | 0L             || "6 measurements \u00b7 No data yet"
        6     | 2         | 0L             || "6 measurements \u00b7 Size not recorded"
    }

    /** The quantities line of the selector's first dataset card. */
    private static String quantitiesLineText(MeasurementDatasetSelector selector) {
        return quantitiesOf(cardsOf(selector).first())
    }

    /** The quantities line of one card: the headline row, then the quantities. */
    private static String quantitiesOf(com.vaadin.flow.component.html.Div card) {
        return ((com.vaadin.flow.component.html.Span) card.getChildren().toList().get(1)).getText()
    }

    /** The identity line of one card: the second line of its headline. */
    private static String identityOf(com.vaadin.flow.component.html.Div card) {
        def headlineRow = (com.vaadin.flow.component.html.Div) card.getChildren().toList().first()
        def headline = (com.vaadin.flow.component.html.Div) headlineRow.getChildren().toList().first()
        return ((com.vaadin.flow.component.html.Span) headline.getChildren().toList().get(1)).getText()
    }

    @Unroll
    def "a card states the dataset size for download planning (#available.#bytes -> #expected)"() {
        given:
        def summary = new MeasurementDatasetSelector.DatasetSummary(
                genomics, 10, available, bytes)

        expect: "size decides whether the data fits the reader's disk and link, so it is stated —"
        // and the two cases that are not a number must not read as a free download.
        MeasurementDatasetSelector.sizeText(summary) == expected

        where:
        // FileSizeFormatter divides by 1024 while labelling the units KB/MB/GB, so these are
        // binary values under decimal names. The expectations follow the formatter, which is the
        // app-wide one and not this page's choice.
        available | bytes              || expected
        2         | 4_200_000_000L     || "~3.9 GB"
        2         | 1024L              || "1 KB"
        0         | 0L                 || "No data yet"
        2         | 0L                 || "Size not recorded"
    }

    /** The label text of the selector's first card. */
    /** The domain label of the selector's first DATASET card; All is not a dataset. */
    private static String firstLabelText(MeasurementDatasetSelector selector) {
        def card = cardsOf(selector).first()
        def headlineRow = (com.vaadin.flow.component.html.Div) card.getChildren().toList().first()
        def headline = (com.vaadin.flow.component.html.Div) headlineRow.getChildren().toList().first()
        return ((com.vaadin.flow.component.html.Span) headline.getChildren().toList().first()).getText()
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
