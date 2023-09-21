package life.qbic.projectmanagement.application

import life.qbic.application.commons.Result
import life.qbic.projectmanagement.application.api.SampleCodeService
import life.qbic.projectmanagement.application.sample.SampleRegistrationService
import life.qbic.projectmanagement.domain.project.Contact
import life.qbic.projectmanagement.domain.project.Project
import life.qbic.projectmanagement.domain.project.ProjectCode
import life.qbic.projectmanagement.domain.project.ProjectId
import life.qbic.projectmanagement.domain.project.ProjectIntent
import life.qbic.projectmanagement.domain.project.ProjectObjective
import life.qbic.projectmanagement.domain.project.ProjectTitle
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicateId
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen
import life.qbic.projectmanagement.domain.project.sample.*
import life.qbic.projectmanagement.domain.project.service.SampleDomainService
import spock.lang.Specification

class SampleRegistrationServiceSpec extends Specification {

    SampleCodeService sampleCodeService = Mock()
    SampleDomainService sampleDomainService = Mock()
    ProjectInformationService projectInformationService = Mock()
    SampleRegistrationService sampleRegistrationService = new SampleRegistrationService(sampleCodeService, sampleDomainService, projectInformationService)
    ProjectId projectId = ProjectId.create()
    Contact who = new Contact()
    Project project = Project.create(new ProjectIntent(new ProjectTitle("a title"), new ProjectObjective("an objective")), new ProjectCode("QABCD"), who, who, who)

    def "No SampleRegistrationRequests returns a Result containing a NO_SAMPLES_DEFINED response code"() {
        given:
        List<SampleRegistrationRequest> sampleRegistrationRequests = new ArrayList<>()
        projectInformationService.find(projectId) >> Optional.of(project)

        when: "A List with no SampleRegistrationRequests is provided"
        Result<Collection<Sample>, SampleRegistrationService.ResponseCode> resultWithSamples = sampleRegistrationService.registerSamples(sampleRegistrationRequests, projectId)

        then: "an exception is thrown"
        resultWithSamples.isError()
        resultWithSamples.getError() == SampleRegistrationService.ResponseCode.NO_SAMPLES_DEFINED
    }

    def "Invalid SampleRegistrationRequests returns a Result containing a SAMPLE_REGISTRATION_FAILED response code"() {
        given:
        SampleOrigin sampleOrigin = SampleOrigin.create(new Species("species"), new Specimen("specimen"), new Analyte("analyte"))
        SampleRegistrationRequest sampleRegistrationRequest = new SampleRegistrationRequest("my_label", BatchId.create(), ExperimentId.create(), 5, BiologicalReplicateId.create(), sampleOrigin, "mytype", AnalysisMethod.ATAC_SEQ, "no comment")
        SampleCode sampleCode = SampleCode.create("QABCDE")
        sampleCodeService.generateFor(projectId) >> Result.fromValue(sampleCode)
        Map<SampleCode, SampleRegistrationRequest> sampleCodesToRegistrationRequests = new HashMap<>()
        sampleCodesToRegistrationRequests.put(sampleCode, sampleRegistrationRequest)
        sampleDomainService.registerSamples(project, sampleCodesToRegistrationRequests) >> Result.fromError(SampleDomainService.ResponseCode.REGISTRATION_FAILED)
        projectInformationService.find(projectId) >> Optional.of(project)

        when: "A list with an invalid SampleRegistrationRequest is provided"
        List<SampleRegistrationRequest> sampleRegistrationRequests = new ArrayList<>()
        sampleRegistrationRequests.add(sampleRegistrationRequest)
        var result = sampleRegistrationService.registerSamples(sampleRegistrationRequests, projectId)

        then: "The result contains the sample_registration_failed response code"
        result.isError()
        result.getError() == SampleRegistrationService.ResponseCode.SAMPLE_REGISTRATION_FAILED
    }

    def "Valid SampleRegistrationRequests returns a Result with the list of registered Samples"() {
        given:
        SampleOrigin sampleOrigin = SampleOrigin.create(new Species("species"), new Specimen("specimen"), new Analyte("analyte"))
        SampleRegistrationRequest sampleRegistrationRequest = new SampleRegistrationRequest("my_label", BatchId.create(), ExperimentId.create(), 4, BiologicalReplicateId.create(), sampleOrigin, "this analysis type", AnalysisMethod.ATAC_SEQ, "a comment")
        SampleCode sampleCode = SampleCode.create("QABCDE")
        Sample sample = Sample.create(sampleCode, sampleRegistrationRequest)
        sampleCodeService.generateFor(projectId) >> Result.fromValue(sampleCode)
        Map<SampleCode, SampleRegistrationRequest> sampleCodesToRegistrationRequests = new HashMap<>()
        sampleCodesToRegistrationRequests.put(sampleCode, sampleRegistrationRequest)
        sampleDomainService.registerSamples(project, sampleCodesToRegistrationRequests) >> Result.fromValue(List.of(sample))
        List<SampleRegistrationRequest> sampleRegistrationRequests = new ArrayList<>()
        sampleRegistrationRequests.add(sampleRegistrationRequest)
        projectInformationService.find(projectId) >> Optional.of(project)

        when: "A List with a valid SampleRegistrationRequest is provided"
        var result = sampleRegistrationService.registerSamples(sampleRegistrationRequests, projectId)

        then: "The result contains the information of the sample registration request"
        result.isValue()
        result.getValue().get(0) == sample
        result.getValue().get(0).sampleCode() == sampleCode
    }

    def "If project cannot be found, valid SampleRegistrationRequests returns a Result containing a SAMPLE_REGISTRATION_FAILED response code"() {
        given:
        SampleOrigin sampleOrigin = SampleOrigin.create(new Species("species"), new Specimen("specimen"), new Analyte("analyte"))
        SampleRegistrationRequest sampleRegistrationRequest = new SampleRegistrationRequest("my_label", BatchId.create(), ExperimentId.create(), 4, BiologicalReplicateId.create(), sampleOrigin, "this analysis type", AnalysisMethod.ATAC_SEQ, "a comment")
        SampleCode sampleCode = SampleCode.create("QABCDE")
        Sample sample = Sample.create(sampleCode, sampleRegistrationRequest)
        sampleCodeService.generateFor(projectId) >> Result.fromValue(sampleCode)
        Map<SampleCode, SampleRegistrationRequest> sampleCodesToRegistrationRequests = new HashMap<>()
        sampleCodesToRegistrationRequests.put(sampleCode, sampleRegistrationRequest)
        sampleDomainService.registerSamples(project, sampleCodesToRegistrationRequests) >> Result.fromValue(List.of(sample))
        List<SampleRegistrationRequest> sampleRegistrationRequests = new ArrayList<>()
        sampleRegistrationRequests.add(sampleRegistrationRequest)
        projectInformationService.find(projectId) >> Optional.empty()

        when: "A List with a valid SampleRegistrationRequest is provided"
        var result = sampleRegistrationService.registerSamples(sampleRegistrationRequests, projectId)

        then: "The result contains the sample_registration_failed response code"
        result.isError()
        result.getError() == SampleRegistrationService.ResponseCode.SAMPLE_REGISTRATION_FAILED
    }
}
