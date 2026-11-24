package life.qbic.projectmanagement.infrastructure.sample

import life.qbic.application.commons.ApplicationException
import life.qbic.projectmanagement.domain.model.OntologyTerm
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.Contact
import life.qbic.projectmanagement.domain.model.project.Project
import life.qbic.projectmanagement.domain.model.project.ProjectCode
import life.qbic.projectmanagement.domain.model.project.ProjectIntent
import life.qbic.projectmanagement.domain.model.project.ProjectObjective
import life.qbic.projectmanagement.domain.model.project.ProjectTitle
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod
import life.qbic.projectmanagement.domain.model.sample.Sample
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import life.qbic.projectmanagement.domain.model.sample.SampleOrigin
import life.qbic.projectmanagement.domain.model.sample.SampleRegistrationRequest
import life.qbic.projectmanagement.domain.repository.ProjectRepository
import life.qbic.projectmanagement.domain.repository.SampleRepository
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

/**
 * Pins the ordering between the database write and the openBIS write in
 * {@link SampleRepositoryImpl#addAll}.
 *
 * <p>The database must validate the write before openBIS is mutated. Because {@link Sample} has an
 * assigned {@code @EmbeddedId}, {@code saveAll} only queues the INSERT; without an explicit flush
 * a constraint violation surfaces at commit, i.e. after openBIS already holds the samples. See
 * issue #1696.
 */
class SampleRepositoryImplSpec extends Specification {

  SampleJpaRepository sampleJpaRepository = Mock()
  SampleDataRepository sampleDataRepository = Mock()
  ProjectRepository projectRepository = Mock()
  SampleRepository selfProxy = Mock()

  SampleRepositoryImpl repository = new SampleRepositoryImpl(
      sampleJpaRepository,
      sampleDataRepository,
      projectRepository,
      selfProxy)

  Project project = Project.create(
      ProjectIntent.of(new ProjectTitle("a title"), new ProjectObjective("an objective")),
      ProjectCode.random(),
      new Contact("a manager", "manager@example.org", null, null),
      new Contact("a pi", "pi@example.org", null, null),
      new Contact("a responsible", "responsible@example.org", null, null))

  def "the database write is flushed before openBIS is called"() {
    given: "a record of the order in which the two stores are written to"
    def calls = []

    and: "samples the database accepts"
    def samples = [sample("Q2001"), sample("Q2002")]
    sampleJpaRepository.saveAllAndFlush(_) >> { calls << "database"; samples }
    sampleDataRepository.addSamplesToProject(_, _) >> { calls << "openbis" }

    when: "the samples are added"
    def result = repository.addAll(project, samples)

    then: "the database is written to exactly once, and before openBIS"
    calls == ["database", "openbis"]

    and: "the saved samples are returned"
    result == samples
  }

  def "a database failure is raised before openBIS is written to"() {
    given: "samples that the database rejects, surfaced by the flush"
    def samples = [sample("Q2001")]
    def databaseFailure = new DataIntegrityViolationException(
        "Data truncation: Data too long for column 'comment' at row 1")
    sampleJpaRepository.saveAllAndFlush(_) >> { throw databaseFailure }

    when: "the samples are added"
    repository.addAll(project, samples)

    then: "the database failure propagates unchanged"
    def exception = thrown(DataIntegrityViolationException)
    exception.is(databaseFailure)

    then: "openBIS was never called, so no orphaned samples can be left behind"
    0 * sampleDataRepository.addSamplesToProject(_, _)
  }

  def "a failure in openBIS is wrapped and keeps its cause"() {
    given: "samples the database accepts but openBIS rejects"
    def samples = [sample("Q2001")]
    def openbisFailure = new RuntimeException("sample with the same code already exists")
    sampleJpaRepository.saveAllAndFlush(_) >> samples
    sampleDataRepository.addSamplesToProject(_, _) >> { throw openbisFailure }

    when: "the samples are added"
    repository.addAll(project, samples)

    then: "the failure is reported with its original cause attached"
    def exception = thrown(ApplicationException)
    exception.cause.is(openbisFailure)
  }

  def "an update is flushed before openBIS is written to"() {
    given: "an updated sample whose new comment the database rejects"
    def samples = [sample("Q2001")]
    sampleJpaRepository.saveAllAndFlush(_) >> {
      throw new DataIntegrityViolationException("Data too long for column 'comment'")
    }

    when: "the samples are updated"
    repository.updateAll(project, samples)

    then: "the database failure propagates"
    thrown(DataIntegrityViolationException)

    then: "openBIS is not touched"
    0 * sampleDataRepository.updateAll(_, _)
  }

  private Sample sample(String code) {
    def request = new SampleRegistrationRequest(
        "sample " + code,
        "patient 1",
        "batch 1",
        project.getId(),
        ExperimentId.create(),
        1L,
        SampleOrigin.create(new OntologyTerm(), new OntologyTerm(), new OntologyTerm()),
        AnalysisMethod.WGS,
        "")
    return Sample.create(SampleCode.create(code), request)
  }
}
