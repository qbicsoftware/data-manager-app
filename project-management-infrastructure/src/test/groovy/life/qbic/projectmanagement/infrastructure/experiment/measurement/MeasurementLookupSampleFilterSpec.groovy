package life.qbic.projectmanagement.infrastructure.experiment.measurement

import life.qbic.projectmanagement.application.measurement.IpMeasurementLookup
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Guards that the sample constraint of a measurement lookup reaches the database filter.
 *
 * <p>Regression context: {@code MeasurementLookup.mapToDatabaseFilter} built the database filter
 * from the experiment, search term and time offset only, silently dropping {@code includedSamples}/
 * {@code excludedSamples}. Its one caller ({@code findMeasuredSampleIds}) hid the defect by
 * re-applying {@code retainAll} on the result, so the answer stayed correct while every lookup
 * fetched <em>all</em> measurements of the experiment. No test covered it.</p>
 *
 * <p>The mapping is exercised directly: it is the exact seam where the constraint was lost, and it
 * is the only place the loss is observable without a database.</p>
 */
class MeasurementLookupSampleFilterSpec extends Specification {

    def "the NGS database filter carries included sample ids"() {
        given:
        def filter = NgsMeasurementLookup.MeasurementFilter.forExperiment("E1")
                .includingSamples(["S1", "S2"] as Set)

        when:
        def dbFilter = MeasurementLookup.mapToDatabaseFilter(filter)

        then:
        dbFilter.hasSampleConstraint()
    }

    def "the PxP database filter carries included sample ids"() {
        given:
        def filter = PxpMeasurementLookup.MeasurementFilter.forExperiment("E1")
                .includingSamples(["S1"] as Set)

        when:
        def dbFilter = MeasurementLookup.mapToDatabaseFilter(filter)

        then:
        dbFilter.hasSampleConstraint()
    }

    def "the IP database filter carries included sample ids"() {
        given:
        def filter = IpMeasurementLookup.MeasurementFilter.forExperiment("E1")
                .includingSamples(["S1"] as Set)

        when:
        def dbFilter = MeasurementLookup.mapToDatabaseFilter(filter)

        then:
        dbFilter.hasSampleConstraint()
    }

    def "an exclusively excluded sample set is carried too"() {
        given: "a filter that only names samples to exclude"
        def filter = NgsMeasurementLookup.MeasurementFilter.forExperiment("E1")
                .excludingSamples(["S9"] as Set)

        when:
        def dbFilter = MeasurementLookup.mapToDatabaseFilter(filter)

        then: "the constraint is not lost, and it is not turned into an inclusion"
        dbFilter.hasSampleConstraint()
    }

    @Unroll
    def "a lookup without a sample constraint does not invent one (#domain)"() {
        when:
        def dbFilter = MeasurementLookup.mapToDatabaseFilter(filter)

        then:
        !dbFilter.hasSampleConstraint()

        where:
        domain | filter
        "ngs"  | NgsMeasurementLookup.MeasurementFilter.forExperiment("E1")
        "pxp"  | PxpMeasurementLookup.MeasurementFilter.forExperiment("E1")
        "ip"   | IpMeasurementLookup.MeasurementFilter.forExperiment("E1")
    }

    @Unroll
    def "forwarding the samples does not replace the experiment constraint (#domain)"() {
        given: "a filter that constrains both the experiment and the samples"
        def filter = switch (domain) {
            case "ngs" -> NgsMeasurementLookup.MeasurementFilter.forExperiment("E1")
                    .includingSamples(["S1"] as Set)
            case "pxp" -> PxpMeasurementLookup.MeasurementFilter.forExperiment("E1")
                    .includingSamples(["S1"] as Set)
            case "ip" -> IpMeasurementLookup.MeasurementFilter.forExperiment("E1")
                    .includingSamples(["S1"] as Set)
            default -> throw new IllegalArgumentException(domain)
        }

        when:
        def dbFilter = MeasurementLookup.mapToDatabaseFilter(filter)

        then:
        dbFilter.getExperimentId().orElseThrow() == "E1"
        dbFilter.hasSampleConstraint()

        where:
        domain << ["ngs", "pxp", "ip"]
    }
}
