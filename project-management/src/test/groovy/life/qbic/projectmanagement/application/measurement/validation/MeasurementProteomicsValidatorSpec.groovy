package life.qbic.projectmanagement.application.measurement.validation


import life.qbic.projectmanagement.application.measurement.Labeling
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata
import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import spock.lang.Specification

import java.util.stream.Collectors

class MeasurementMeasurementProteomicsValidatorSpec extends Specification {

    final static ProteomicsMeasurementMetadata validMetadata = new ProteomicsMeasurementMetadata("", "", SampleCode.create("QTEST001AE"),
            "",
            "https://ror.org/03a1kwz48", //Universität Tübingen,
            "EFO:0004205", //Illumina MiSeq
            "1",
            "The geniuses of ITSS",
            "4 Nations lived in harmony",
            "CASPASE6",
            "in solition",
            "Enrichment Index",
            "1337",
            "12",
            "LCMS Method 1",
            new Labeling("isotope", "N15"),
            "Don't tell anyone this is a test"
    )
    final static OntologyClass illuminaMiSeq = new OntologyClass(
            "efo",
            "3.62.0",
            "http://www.ebi.ac.uk/efo/efo.owl#",
            "Illumina MiSeq",
            "EFO:0004205",
            "The Illumina MiSeq is a high-throughput sequencing machine developed by Illumina. Its primary applications include small whole-genome sequencing, targeted sequencing of a set of genes or gene regions and 16S metagenomic sequencing.",
            "http://www.ebi.ac.uk/efo/EFO_0004205"
    )

    final static List<String> validPXPProperties = Collections.unmodifiableList(["qbic sample id", "sample name", "technical replicate", "organisation id", "facility", "ms device",
                                                                                 "sample pool group", "cycle/fraction name", "digestion method", "digestion enzyme",
                                                                                 "enrichment method", "injection volume (uL)", "lc column",
                                                                                 "lcms method", "labeling type", "label", "comment"])

    def "A complete property set must be valid no matter the letter casing style"() {

        when:
        def isPxPmetadata = MeasurementProteomicsValidator.isProteomics(chaosCasing)

        then:
        isPxPmetadata
        where:
        chaosCasing << [
                validPXPProperties.collect { it.toUpperCase() },
                validPXPProperties.collect { it.toLowerCase() }]
    }


    def "Given a valid proteomics measurement metadata property collection, pass the validation"() {
        given:

        when:
        def isPXPmetadata = MeasurementProteomicsValidator.isProteomics(validPXPProperties)

        then:
        isPXPmetadata

    }

    def "Missing properties for the proteomics metadata collection must result in an unsuccessful validation"() {
        given:
        // we just take the NGS property list
        def missingProperties = validPXPProperties.stream().collect(Collectors.toList())
        missingProperties.remove(0)

        when:
        def isPXPmetadata = MeasurementProteomicsValidator.isProteomics(missingProperties)

        then:
        !isPXPmetadata
    }

    def "Providing no properties for the NGS metadata collection must result in an unsuccessful validation"() {
        given:
        def missingProperties = []

        when:
        def isPxPmetadata = MeasurementProteomicsValidator.isProteomics(missingProperties)

        then:
        !isPxPmetadata
    }

}
