package life.qbic.projectmanagement.application.sample

import life.qbic.application.commons.Result
import life.qbic.projectmanagement.application.DeletionService
import life.qbic.projectmanagement.application.api.SampleCodeService
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference
import life.qbic.projectmanagement.domain.model.OntologyTerm
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import life.qbic.projectmanagement.domain.model.sample.SampleId
import life.qbic.projectmanagement.domain.repository.SampleRepository
import spock.lang.Specification

class SampleRegistrationServiceV2Spec extends Specification {

  SampleRepository sampleRepository = Mock()
  SampleCodeService sampleCodeService = Stub()
  DeletionService deletionService = Mock()
  ConfoundingVariableService confoundingVariableService = Mock()
  SampleValidationService validationService = Stub()
  SampleRegistrationNotificationService notificationService = Stub()

  SampleRegistrationServiceV2 service = new SampleRegistrationServiceV2(
      sampleRepository,
      sampleCodeService,
      deletionService,
      confoundingVariableService,
      validationService,
      notificationService)

  def "When a step after registration fails, the compensation deletes the registered (non-null) sample ids"() {
    given: "a project and metadata for one new sample (sample id still null at this point)"
    def projectId = ProjectId.create()
    def experimentId = ExperimentId.create().value()
    def metadata = [
        SampleMetadata.createNew(
            "sample-1", AnalysisMethod.WGS, "1", 1L,
            new OntologyTerm(), new OntologyTerm(), new OntologyTerm(),
            "comment", [:], experimentId, "batch-1")
    ]

    and: "a sample code can be generated"
    sampleCodeService.generateFor(_) >> Result.fromValue(SampleCode.create("Q2001"))

    and: "addAll returns the samples it was given, so they carry their real sample ids"
    sampleRepository.addAll(projectId, _) >> { pid, samples -> samples }

    and: "a later step (confounding variables) fails after the samples were registered"
    confoundingVariableService.setVariableLevelsForSample(_, _, _, _) >> {
      throw new RuntimeException("confounding variables failed")
    }

    when: "registration is attempted"
    service.registerSamplesFromMetadata(metadata, projectId, new ExperimentReference(experimentId))

    then: "the failure propagates"
    thrown(RuntimeException)

    and: "the compensation deletes the real, non-null registered sample ids (not null placeholders)"
    1 * deletionService.deleteSamples(projectId, { Collection<SampleId> sampleIds ->
      !sampleIds.isEmpty() && sampleIds.every { it != null }
    })
  }
}