package life.qbic.projectmanagement.application.measurement.validation

import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationIP
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificIP
import life.qbic.projectmanagement.application.ProjectInformationService
import life.qbic.projectmanagement.application.measurement.MeasurementService
import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.application.ontology.TerminologyService
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.Project
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.model.sample.Sample
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import life.qbic.projectmanagement.domain.model.sample.SampleId
import spock.lang.Specification

class MeasurementIPValidatorSpec extends Specification {

    final static List<String> validIPProperties = MeasurementIPValidator.IP_PROPERTY.values().collect { it.label() }

    final static OntologyClass validInstrument = new OntologyClass(
            "efo",
            "3.62.0",
            "http://www.ebi.ac.uk/efo/efo.owl#",
            "Q Exactive HF-X",
            "EFO:0008637",
            "A hybrid quadrupole-Orbitrap mass spectrometer.",
            "http://www.ebi.ac.uk/efo/EFO_0008637"
    )

    def "A complete property set must be valid no matter the letter casing style"() {
        when:
        def isIPMetadata = MeasurementIPValidator.isIP(chaosCasing)

        then:
        isIPMetadata

        where:
        chaosCasing << [
                validIPProperties.collect { it.toUpperCase() },
                validIPProperties.collect { it.toLowerCase() }
        ]
    }

    def "Given a valid IP measurement metadata property collection, pass the validation"() {
        when:
        def isIPMetadata = MeasurementIPValidator.isIP(validIPProperties)

        then:
        isIPMetadata
    }

    def "Missing properties for the IP metadata collection must result in an unsuccessful validation"() {
        given:
        def missingProperties = new ArrayList<>(validIPProperties)
        missingProperties.remove(0)

        when:
        def isIPMetadata = MeasurementIPValidator.isIP(missingProperties)

        then:
        !isIPMetadata
    }

    def "Providing no properties for the IP metadata collection must result in an unsuccessful validation"() {
        when:
        def isIPMetadata = MeasurementIPValidator.isIP([])

        then:
        !isIPMetadata
    }

    def "Given a valid IP registration, validateRegistration returns a successful result"() {
        given:
        def experimentId = ExperimentId.create()
        def sampleInformationService = Mock(SampleInformationService) {
            findSampleId(SampleCode.create("QTEST001AE")) >> Optional.of(
                    new SampleIdCodeEntry(SampleId.create(), SampleCode.create("QTEST001AE"))
            )
            retrieveSamplesByIds(_ as List<SampleId>) >> { args ->
                def sampleId = ((List<SampleId>) args[0])[0]
                def sample = Mock(Sample)
                sample.sampleId() >> sampleId
                sample.experimentId() >> experimentId
                [sample]
            }
        }
        def terminologyService = Mock(TerminologyService) {
            findByCurie("EFO:0008637") >> Optional.of(validInstrument)
        }
        def projectInformationService = Mock(ProjectInformationService) {
            find(_ as ProjectId) >> Optional.of(Mock(Project) {
                experiments() >> [experimentId]
            })
        }
        def measurementService = Mock(MeasurementService)

        def validator = new MeasurementIPValidator(
                sampleInformationService, terminologyService, projectInformationService, measurementService
        )

        def registration = createValidRegistration()

        when:
        def result = validator.validateRegistration(registration, experimentId.value(), ProjectId.create())

        then:
        result.allPassed()
        result.failures().isEmpty()
    }

    def "Given an IP registration with missing mandatory fields, validateRegistration returns failures"() {
        given:
        def experimentId = ExperimentId.create()
        def sampleInformationService = Mock(SampleInformationService) {
            findSampleId(SampleCode.create("QTEST001AE")) >> Optional.of(
                    new SampleIdCodeEntry(SampleId.create(), SampleCode.create("QTEST001AE"))
            )
            retrieveSamplesByIds(_ as List<SampleId>) >> { args ->
                def sampleId = ((List<SampleId>) args[0])[0]
                def sample = Mock(Sample)
                sample.sampleId() >> sampleId
                sample.experimentId() >> experimentId
                [sample]
            }
        }
        def terminologyService = Mock(TerminologyService) {
            findByCurie("EFO:0008637") >> Optional.of(validInstrument)
        }
        def projectInformationService = Mock(ProjectInformationService) {
            find(_ as ProjectId) >> Optional.of(Mock(Project) {
                experiments() >> [experimentId]
            })
        }
        def measurementService = Mock(MeasurementService)

        def validator = new MeasurementIPValidator(
                sampleInformationService, terminologyService, projectInformationService, measurementService
        )

        and: "registration with blank mandatory specific metadata fields"
        def blankSpecific = new MeasurementSpecificIP(
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""
        )
        def registration = new MeasurementRegistrationInformationIP(
                "https://ror.org/03a1kwz48",
                "EFO:0008637",
                "QBiC",
                "",
                Map.of("QTEST001AE", blankSpecific),
                "Test"
        )

        when:
        def result = validator.validateRegistration(registration, experimentId.value(), ProjectId.create())

        then:
        !result.allPassed()
        result.failures().size() == 10
        result.failures().contains("Sample Mass is mandatory")
        result.failures().contains("Sample Volume is mandatory")
        result.failures().contains("MHC Antibody is mandatory")
        result.failures().contains("Enrichment method is mandatory")
        result.failures().contains("LCMS Method is mandatory")
        result.failures().contains("LC Column is mandatory")
        result.failures().contains("Data Acquisition is mandatory")
        result.failures().contains("Mass range is mandatory")
        result.failures().contains("Retention time range is mandatory")
        result.failures().contains("Charge range is mandatory")
    }

    def "Given an IP registration with missing instrument, validateRegistration returns instrument failure"() {
        given:
        def experimentId = ExperimentId.create()
        def sampleInformationService = Mock(SampleInformationService) {
            findSampleId(SampleCode.create("QTEST001AE")) >> Optional.of(
                    new SampleIdCodeEntry(SampleId.create(), SampleCode.create("QTEST001AE"))
            )
            retrieveSamplesByIds(_ as List<SampleId>) >> { args ->
                def sampleId = ((List<SampleId>) args[0])[0]
                def sample = Mock(Sample)
                sample.sampleId() >> sampleId
                sample.experimentId() >> experimentId
                [sample]
            }
        }
        def terminologyService = Mock(TerminologyService)
        def projectInformationService = Mock(ProjectInformationService) {
            find(_ as ProjectId) >> Optional.of(Mock(Project) {
                experiments() >> [experimentId]
            })
        }
        def measurementService = Mock(MeasurementService)

        def validator = new MeasurementIPValidator(
                sampleInformationService, terminologyService, projectInformationService, measurementService
        )

        def registration = new MeasurementRegistrationInformationIP(
                "https://ror.org/03a1kwz48",
                "", // blank instrument
                "QBiC",
                "",
                Map.of("QTEST001AE", createValidSpecific()),
                "Test"
        )

        when:
        def result = validator.validateRegistration(registration, experimentId.value(), ProjectId.create())

        then:
        !result.allPassed()
        result.failures().contains("Instrument is mandatory")
    }

    def "Given an IP registration with unknown instrument CURIE, validateRegistration returns unknown instrument failure"() {
        given:
        def experimentId = ExperimentId.create()
        def sampleInformationService = Mock(SampleInformationService) {
            findSampleId(SampleCode.create("QTEST001AE")) >> Optional.of(
                    new SampleIdCodeEntry(SampleId.create(), SampleCode.create("QTEST001AE"))
            )
            retrieveSamplesByIds(_ as List<SampleId>) >> { args ->
                def sampleId = ((List<SampleId>) args[0])[0]
                def sample = Mock(Sample)
                sample.sampleId() >> sampleId
                sample.experimentId() >> experimentId
                [sample]
            }
        }
        def terminologyService = Mock(TerminologyService) {
            findByCurie("UNKNOWN:12345") >> Optional.empty()
        }
        def projectInformationService = Mock(ProjectInformationService) {
            find(_ as ProjectId) >> Optional.of(Mock(Project) {
                experiments() >> [experimentId]
            })
        }
        def measurementService = Mock(MeasurementService)

        def validator = new MeasurementIPValidator(
                sampleInformationService, terminologyService, projectInformationService, measurementService
        )

        def registration = new MeasurementRegistrationInformationIP(
                "https://ror.org/03a1kwz48",
                "UNKNOWN:12345",
                "QBiC",
                "",
                Map.of("QTEST001AE", createValidSpecific()),
                "Test"
        )

        when:
        def result = validator.validateRegistration(registration, experimentId.value(), ProjectId.create())

        then:
        !result.allPassed()
        result.failures().contains("Unknown instrument: UNKNOWN:12345")
    }

    def "Given an IP registration with missing organisation, validateRegistration returns organisation failure"() {
        given:
        def experimentId = ExperimentId.create()
        def sampleInformationService = Mock(SampleInformationService) {
            findSampleId(SampleCode.create("QTEST001AE")) >> Optional.of(
                    new SampleIdCodeEntry(SampleId.create(), SampleCode.create("QTEST001AE"))
            )
            retrieveSamplesByIds(_ as List<SampleId>) >> { args ->
                def sampleId = ((List<SampleId>) args[0])[0]
                def sample = Mock(Sample)
                sample.sampleId() >> sampleId
                sample.experimentId() >> experimentId
                [sample]
            }
        }
        def terminologyService = Mock(TerminologyService) {
            findByCurie("EFO:0008637") >> Optional.of(validInstrument)
        }
        def projectInformationService = Mock(ProjectInformationService) {
            find(_ as ProjectId) >> Optional.of(Mock(Project) {
                experiments() >> [experimentId]
            })
        }
        def measurementService = Mock(MeasurementService)

        def validator = new MeasurementIPValidator(
                sampleInformationService, terminologyService, projectInformationService, measurementService
        )

        def registration = new MeasurementRegistrationInformationIP(
                "", // blank organisation
                "EFO:0008637",
                "QBiC",
                "",
                Map.of("QTEST001AE", createValidSpecific()),
                "Test"
        )

        when:
        def result = validator.validateRegistration(registration, experimentId.value(), ProjectId.create())

        then:
        !result.allPassed()
        result.failures().contains("Organisation URL is mandatory")
    }

    def "Given an IP registration with invalid ROR organisation, validateRegistration returns ROR format failure"() {
        given:
        def experimentId = ExperimentId.create()
        def sampleInformationService = Mock(SampleInformationService) {
            findSampleId(SampleCode.create("QTEST001AE")) >> Optional.of(
                    new SampleIdCodeEntry(SampleId.create(), SampleCode.create("QTEST001AE"))
            )
            retrieveSamplesByIds(_ as List<SampleId>) >> { args ->
                def sampleId = ((List<SampleId>) args[0])[0]
                def sample = Mock(Sample)
                sample.sampleId() >> sampleId
                sample.experimentId() >> experimentId
                [sample]
            }
        }
        def terminologyService = Mock(TerminologyService) {
            findByCurie("EFO:0008637") >> Optional.of(validInstrument)
        }
        def projectInformationService = Mock(ProjectInformationService) {
            find(_ as ProjectId) >> Optional.of(Mock(Project) {
                experiments() >> [experimentId]
            })
        }
        def measurementService = Mock(MeasurementService)

        def validator = new MeasurementIPValidator(
                sampleInformationService, terminologyService, projectInformationService, measurementService
        )

        def registration = new MeasurementRegistrationInformationIP(
                "not-a-ror-id",
                "EFO:0008637",
                "QBiC",
                "",
                Map.of("QTEST001AE", createValidSpecific()),
                "Test"
        )

        when:
        def result = validator.validateRegistration(registration, experimentId.value(), ProjectId.create())

        then:
        !result.allPassed()
        result.failures().any { it.startsWith("The organisation ID does not seem to be a ROR ID") }
    }

    private static MeasurementRegistrationInformationIP createValidRegistration() {
        return new MeasurementRegistrationInformationIP(
                "https://ror.org/03a1kwz48",
                "EFO:0008637",
                "QBiC",
                "",
                Map.of("QTEST001AE", createValidSpecific()),
                "MS Run 1"
        )
    }

    private static MeasurementSpecificIP createValidSpecific() {
        return new MeasurementSpecificIP(
                "1.5",
                "100.5",
                "Fraction 1",
                "W6/32",
                "PCR-SSP",
                "Immune affinity",
                "2024-01-15",
                "2024-01-16",
                "DDA",
                "C18",
                "DDA",
                "300-1800",
                "120",
                "2-4",
                "0.6-1.6",
                "Test comment"
        )
    }
}
