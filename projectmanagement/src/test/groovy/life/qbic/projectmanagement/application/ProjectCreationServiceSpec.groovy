package life.qbic.projectmanagement.application

import life.qbic.application.commons.ApplicationException
import life.qbic.application.commons.Result
import life.qbic.projectmanagement.domain.project.Contact
import life.qbic.projectmanagement.domain.project.Project
import life.qbic.projectmanagement.domain.project.ProjectId
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository
import life.qbic.projectmanagement.domain.project.service.ProjectDomainService
import spock.lang.Specification

import static java.util.Objects.nonNull

class ProjectCreationServiceSpec extends Specification {

  ProjectRepository projectRepositoryStub = Stub()
  AddExperimentToProjectService addExperimentToProjectServiceStub = Stub()
  ProjectDomainService projectDomainServiceStub = new ProjectDomainService(projectRepositoryStub)
  ProjectCreationService projectCreationServiceWithStubs = new ProjectCreationService(projectRepositoryStub, projectDomainServiceStub)

  def "invalid project title leads to INVALID_PROJECT_TITLE code"() {
    given:
    projectRepositoryStub.add(_ as Project) >> {}
    addExperimentToProjectServiceStub.addExperimentToProject(_ as ProjectId, _ as String as String, _ as List<Species> as List<Species>, _ as List<Specimen> as List<Specimen>, _ as List<Analyte>) >> {}
    def personReference = new Contact("Mustermann", "some@notavailable.zxü")
    when: "null input is provided"


    Result<Project, ApplicationException> resultWithExperimentalDesign = projectCreationServiceWithStubs.createProject(
            "source offer", "QABCD",
            null,
            "objective",
            personReference,
            personReference,
            personReference)
    then: "an exception is thrown"
    resultWithExperimentalDesign.isError()
    resultWithExperimentalDesign.getError().errorCode() == ApplicationException.ErrorCode.INVALID_PROJECT_TITLE
  }

  def "when create is called without a project manager then an exception is thrown"() {
    given:
    projectRepositoryStub.add(_ as Project) >> {}
    addExperimentToProjectServiceStub.addExperimentToProject(_ as ProjectId, _ as String as String, _ as List<Species> as List<Species>, _ as List<Specimen> as List<Specimen>, _ as List<Analyte>) >> {}
    def contact = new Contact("Mustermann", "some@notavailable.zxü")

    when: "create is called without a project manager"
    Result<Project, ApplicationException> result = projectCreationServiceWithStubs.createProject("source offer", "QABCD",
            "my title",
            "objective",
            contact,
            contact,
            null)

        then: "an exception is thrown"
        result.isError()
        result.getError().errorCode() == ApplicationException.ErrorCode.GENERAL
    }

  def "when create is called without a principal investigator (PI) then an exception is thrown"() {
    given:
    projectRepositoryStub.add(_ as Project) >> {}
    addExperimentToProjectServiceStub.addExperimentToProject(_ as ProjectId, _ as String as String, _ as List<Species> as List<Species>, _ as List<Specimen> as List<Specimen>, _ as List<Analyte>) >> {}
    def contact = new Contact("Mustermann", "some@notavailable.zxü")

    when: "create is called without a principal investigator"
    Result<Project, ApplicationException> result = projectCreationServiceWithStubs.createProject("source offer", "QABCD",
            "my title",
            "objective",
            null,
            contact,
            contact)

        then: "an exception is thrown"
        result.isError()
        result.getError().errorCode() == ApplicationException.ErrorCode.GENERAL
    }

  def "when create is called without a responsible person then the project does not contain a responsible person"() {
    given:
    projectRepositoryStub.add(_ as Project) >> {}
    addExperimentToProjectServiceStub.addExperimentToProject(_ as ProjectId, _ as String, _ as List<Species>, _ as List<Specimen>, _ as List<Analyte>) >> Result.fromValue(ExperimentId.create())
    def contact = new Contact("Mustermann", "some@notavailable.zxü")

    when: "create is called without a project manager"
    Result<Project, ApplicationException> result = projectCreationServiceWithStubs.createProject("source offer", "QABCD",
            "my title",
            "objective",
            contact,
            null,
            contact)

    then: "a project is returned"
    result.isValue()
    result.getValue().getResponsiblePerson().isEmpty()
  }

  def "expect project creation returns the created project for a non-empty title"() {
    given:
    projectRepositoryStub.add(_ as Project) >> {}
    addExperimentToProjectServiceStub.addExperimentToProject(_ as ProjectId, _ as String, _ as List<Species>, _ as List<Specimen>, _ as List<Analyte>) >> Result.fromValue(ExperimentId.create())

    def contact = new Contact("Mustermann", "some@notavailable.zxü")

    when: "a project is created with a non-empty title"
    Result<Project, ApplicationException> result = projectCreationServiceWithStubs.createProject("source offer", "QABCD",
            "my title",
            "objective",
            contact,
            contact,
            contact)

        then: "the created project is returned"
        result.isValue()
        nonNull(result.getValue())
    }

  def "expect unsuccessful save of a new project returns GENERAL error code"() {
    given:
    projectRepositoryStub.add(_ as Project) >> { throw new RuntimeException("expected exception") }
    def contact = new Contact("Mustermann", "some@notavailable.zxü")

    when:
    Result<Project, ApplicationException> result = projectCreationServiceWithStubs.createProject("source offer", "QABCD",
            "my title",
            "objective",
            contact,
            contact,
            contact)


        then:
        result.isError()
        result.getError().errorCode() == ApplicationException.ErrorCode.GENERAL
    }
}
