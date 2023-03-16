package life.qbic.projectmanagement.application


import life.qbic.projectmanagement.domain.project.*
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository
import spock.lang.Specification

class AddExperimentToProjectServiceSpec extends Specification {


    ProjectRepository projectRepository = Mock()
    AddExperimentToProjectService service = new AddExperimentToProjectService(projectRepository)


    def project = setupProject()

    def "when an experiment is added to a project then the project is modified correctly"() {
        given:
        projectRepository.find(project.id) >> Optional.of(project)
        projectRepository.find((ProjectId) _) >> Optional.empty()

        when: "an experiment is added to a project"
        def result = service.addExperimentToProject(project.id, "bla", [Analyte.create("a1")], [Species.create("species1")], [Specimen.create("specimen1")])
        ExperimentId experimentId = result.value()

        then: "the project holds a reference to the created experiment"
        project.experiments().contains(experimentId)

        and: "the active experiment of the project is the added experiment"
        project.activeExperiment().map { it == experimentId }.orElse(false)

        and: "the project is updated"
        1 * projectRepository.update(project)
    }


    private static Project setupProject() {
        ProjectId projectId = ProjectId.parse("0270ce7f-4092-40e3-9c4c-ce7adb688bf5")
        ProjectIntent projectIntent = ProjectIntent.of(ProjectTitle.of("title"), ProjectObjective.create("objective"))
        ProjectCode projectCode = ProjectCode.random()
        PersonReference personReference = new PersonReference("abcd", "john doe", "john@doe.abcdefg")
        return Project.of(projectId, projectIntent, projectCode, personReference, personReference, personReference)
    }
}
