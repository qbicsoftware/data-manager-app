package life.qbic.projectmanagement.application.measurement.validation


import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata
import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.application.ontology.TerminologyService
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import spock.lang.Specification

import java.util.stream.Collectors

class MeasurementNGSValidatorSpec extends Specification {

    final static NGSMeasurementMetadata validMetadata = new NGSMeasurementMetadata("", "", [SampleCode.create("QTEST001AE")],
            "https://ror.org/03a1kwz48", //Universität Tübingen,
            "EFO:0004205", //Illumina MiSeq
            "My awesome facility",
            "Unknown sequencing read type",
            "Cool library kit",
            "Bodacious flow cell",
            "We do it by the book",
            "We need groups",
            "Index I7 never stops",
            "Index I5 always goes",
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
    final TerminologyService terminologyService = Mock(TerminologyService.class, {
        findByCurie(validMetadata.instrumentCURI()) >> Optional.of(illuminaMiSeq)
    })

    final static List<String> validNGSProperties = MeasurementNGSValidator.NGS_PROPERTY.values().collect { it.label() }


    def "A complete property set must be valid no matter the letter casing style"() {

        when:
        def isNGSMetadata = MeasurementNGSValidator.isNGS(chaosCasing)

        then:
        isNGSMetadata
        where:
        chaosCasing << [
                validNGSProperties.collect { it.toUpperCase() },
                validNGSProperties.collect { it.toLowerCase() }]
    }


    def "Given a valid ngs measurement metadata property collection, pass the validation"() {
        given:

        when:
        def isNGSMetadata = MeasurementNGSValidator.isNGS(validNGSProperties)

        then:
        isNGSMetadata

    }

    def "Missing properties for the ngs metadata collection must result in an unsuccessful validation"() {
        given:
        // we just take the NGS property list
        def missingProperties = validNGSProperties.stream().collect(Collectors.toList())
        missingProperties.remove(0)

        when:
        def isNGSMetadata = MeasurementNGSValidator.isNGS(missingProperties)

        then:
        !isNGSMetadata
    }

    def "Providing no properties for the NGS metadata collection must result in an unsuccessful validation"() {
        given:
        def missingProperties = []

        when:
        def isNGSMetadata = MeasurementNGSValidator.isNGS(missingProperties)

        then:
        !isNGSMetadata
    }

}
