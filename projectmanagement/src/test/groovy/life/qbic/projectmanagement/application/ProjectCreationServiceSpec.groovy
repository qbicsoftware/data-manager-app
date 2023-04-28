package life.qbic.projectmanagement.application

import life.qbic.application.commons.ApplicationException
import life.qbic.application.commons.Result
import life.qbic.projectmanagement.domain.project.PersonReference
import life.qbic.projectmanagement.domain.project.Project
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository
import spock.lang.Specification

import static java.util.Objects.nonNull

class ProjectCreationServiceSpec extends Specification {

    ProjectRepository projectRepositoryStub = Stub()
    AddExperimentToProjectService addExperimentToProjectServiceStub = Stub()
    ProjectCreationService projectCreationServiceWithStubs = new ProjectCreationService(projectRepositoryStub, addExperimentToProjectServiceStub)

    def "invalid project title leads to INVALID_PROJECT_TITLE code"() {
        given:
        projectRepositoryStub.add(_) >> {}
        addExperimentToProjectServiceStub.addExperimentToProject(_, _, _, _, _) >> {}
        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")
        when: "null input is provided"


        Result<Project, ApplicationException> resultWithExperimentalDesign = projectCreationServiceWithStubs.createProject("QABCD",
                null,
                "objective",
                "design description",
                "source offer",
                personReference,
                personReference,
                personReference,
                [], [], [])
        then: "an exception is thrown"
        resultWithExperimentalDesign.isFailure()
        resultWithExperimentalDesign.exception().errorCode() == ApplicationException.ErrorCode.INVALID_PROJECT_TITLE
    }

    def "invalid objective leads to INVALID_PROJECT_OBJECTIVE code"() {
        given:
        projectRepositoryStub.add(_) >> {}
        addExperimentToProjectServiceStub.addExperimentToProject(_, _, _, _, _) >> {}
        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")
        when: "null input is provided"
        Result<Project, ApplicationException> resultWithExperimentalDesign = projectCreationServiceWithStubs.createProject("QABCD",
                "my title",
                null,
                "design description",
                "source offer",
                personReference,
                personReference,
                personReference,
                [], [], [])

        then: "an exception is thrown"
        resultWithExperimentalDesign.isFailure()
        resultWithExperimentalDesign.exception().errorCode() == ApplicationException.ErrorCode.INVALID_PROJECT_OBJECTIVE
    }

    def "invalid experimental design description leads to INVALID_EXPERIMENTAL_DESIGN code"() {
        given:
        projectRepositoryStub.add(_) >> {}
        addExperimentToProjectServiceStub.addExperimentToProject(_, _, _, _, _) >> {}
        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")

        and:
        String descriptionWithToManyCharacters = "test" * 1000

        when: "null input is provided"
        Result<Project, ApplicationException> resultWithExperimentalDesign = projectCreationServiceWithStubs.createProject("QABCD",
                "my title",
                "objective",
                descriptionWithToManyCharacters,
                "source offer",
                personReference,
                personReference,
                personReference,
                [], [], [])

        then: "an exception is thrown"
        resultWithExperimentalDesign.isFailure()
        resultWithExperimentalDesign.exception().errorCode() == ApplicationException.ErrorCode.INVALID_EXPERIMENTAL_DESIGN
    }

    def "when create is called without a project manager then an exception is thrown"() {
        given:
        projectRepositoryStub.add(_) >> {}
        addExperimentToProjectServiceStub.addExperimentToProject(_, _, _, _, _) >> {}
        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")

        when: "create is called without a project manager"
        Result<Project, ApplicationException> result = projectCreationServiceWithStubs.createProject("QABCD",
                "my title",
                "objective",
                "some description",
                "source offer",
                null,
                personReference,
                personReference,
                [], [], [])

        then: "an exception is thrown"
        result.isFailure()
        result.exception().errorCode() == ApplicationException.ErrorCode.GENERAL
    }

    def "when create is called without a principal investigator (PI) then an exception is thrown"() {
        given:
        projectRepositoryStub.add(_) >> {}
        addExperimentToProjectServiceStub.addExperimentToProject(_, _, _, _, _) >> {}
        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")

        when: "create is called without a project manager"
        Result<Project, ApplicationException> result = projectCreationServiceWithStubs.createProject("QABCD",
                "my title",
                "objective",
                "some description",
                "source offer",
                personReference,
                null,
                personReference,
                [], [], [])

        then: "an exception is thrown"
        result.isFailure()
        result.exception().errorCode() == ApplicationException.ErrorCode.GENERAL
    }

    def "when create is called without a responsible person then the project does not contain a responsible person"() {
        given:
        projectRepositoryStub.add(_) >> {}
        addExperimentToProjectServiceStub.addExperimentToProject(_, _, _, _, _) >> Result.success(ExperimentId.create())
        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")

        when: "create is called without a project manager"
        Result<Project, ApplicationException> result = projectCreationServiceWithStubs.createProject("QABCD",
                "my title",
                "objective",
                "some description",
                "source offer",
                personReference,
                personReference,
                null,
                [], [], [])

        then: "a project is returned"
        result.isSuccess()
        result.value().getResponsiblePerson().isEmpty()
    }

    def "when analytes are provided at creation then an experiment is created with those analytes"() {
        given:
        projectRepositoryStub.add(_) >> {}
        projectRepositoryStub.update(_) >> {}
        AddExperimentToProjectService addExperimentToProjectService = Mock()
        ProjectCreationService projectCreationService = new ProjectCreationService(projectRepositoryStub, addExperimentToProjectService)
        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")
        def analytes = List.of(Analyte.create("my analyte"))

        when: "analytes are provided at creation"
        Result<Project, ApplicationException> result = projectCreationService.createProject("QABCD",
                "my title",
                "objective",
                "some description",
                "source offer",
                personReference,
                personReference,
                personReference,
                [], analytes, [])

        then: "the analytes can be retrieved"
        1 * addExperimentToProjectService.addExperimentToProject(_, _, analytes, _, _) >> Result.success(ExperimentId.create());
        result.isSuccess()
    }

    def "when species are provided at creation then an experiment is created with those species"() {
        given:
        projectRepositoryStub.add(_) >> {}
        AddExperimentToProjectService addExperimentToProjectService = Mock()
        addExperimentToProjectService.addExperimentToProject(_, _, _, _, _) >> Result.success(ExperimentId.create())
        ProjectCreationService projectCreationService = new ProjectCreationService(projectRepositoryStub, addExperimentToProjectService)
        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")
        def species = List.of(Species.create("my analyte"))

        when: "species are provided at creation"
        Result<Project, ApplicationException> result = projectCreationService.createProject("QABCD",
                "my title",
                "objective",
                "some description",
                "source offer",
                personReference,
                personReference,
                personReference,
                species, [], [])

        then: "the analytes can be retrieved"
        1 * addExperimentToProjectService.addExperimentToProject(_, _, _, species, _) >> Result.success(ExperimentId.create())
        result.isSuccess()
    }

    def "when specimens are provided at creation then an experiment is created with those specimens"() {
        given:
        projectRepositoryStub.add(_) >> {}
        AddExperimentToProjectService addExperimentToProjectService = Mock()
        addExperimentToProjectService.addExperimentToProject(_, _, _, _, _) >> Result.success(ExperimentId.create())
        ProjectCreationService projectCreationService = new ProjectCreationService(projectRepositoryStub, addExperimentToProjectService)
        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")
        def specimens = List.of(Specimen.create("my analyte"))

        when: "specimens are provided at creation"
        Result<Project, ApplicationException> result = projectCreationService.createProject("QABCD",
                "my title",
                "objective",
                "some description",
                "source offer",
                personReference,
                personReference,
                personReference,
                [], [], specimens)

        then: "the analytes can be retrieved"
        1 * addExperimentToProjectService.addExperimentToProject(_, _, _, _, specimens) >> Result.success(ExperimentId.create())
        result.isSuccess()
    }

    def "expect project creation returns the created project for a non-empty title"() {
        given:
        projectRepositoryStub.add(_) >> {}
        addExperimentToProjectServiceStub.addExperimentToProject(_, _, _, _, _) >> Result.success(ExperimentId.create())

        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")

        when: "a project is created with a non-empty title"
        Result<Project, ApplicationException> result = projectCreationServiceWithStubs.createProject("QABCD",
                "my title",
                "objective",
                "design description",
                "source offer",
                personReference,
                personReference,
                personReference,
                [], [], [])

        then: "the created project is returned"
        result.isSuccess()
        nonNull(result.value())
    }

    def "expect unsuccessful save of a new project returns GENERAL error code"() {
        given:
        projectRepositoryStub.add(_) >> { throw new RuntimeException("expected exception") }
        addExperimentToProjectServiceStub.addExperimentToProject(_, _, _, _, _) >> {}
        def personReference = new PersonReference("Max", "Mustermann", "some@notavailable.zxü")

        when:
        Result<Project, ApplicationException> resultWithExperimentalDesign = projectCreationServiceWithStubs.createProject("QABCD",
                "my title",
                "objective",
                "design description",
                "source offer",
                personReference,
                personReference,
                personReference,
                [], [], [])


        then:
        resultWithExperimentalDesign.isFailure()
        resultWithExperimentalDesign.exception().errorCode() == ApplicationException.ErrorCode.GENERAL
    }
}
