package life.qbic.projectmanagement.application

import life.qbic.projectmanagement.domain.model.OntologyTerm
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.*
import life.qbic.projectmanagement.domain.repository.ProjectRepository
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
        def result = service.addExperimentToProject(project.id,
                "Pilot",
                [new OntologyTerm()],
                [new OntologyTerm()],
                [new OntologyTerm()],
                "default",
                "default")
        ExperimentId experimentId = result.getValue()

        then: "the project holds a reference to the created experiment"
        project.experiments().contains(experimentId)


        and: "the project is updated"
        1 * projectRepository.update(project)
    }


    private static Project setupProject() {
        ProjectId projectId = ProjectId.parse("0270ce7f-4092-40e3-9c4c-ce7adb688bf5")
        ProjectIntent projectIntent = ProjectIntent.of(
                ProjectTitle.of("Oral microbiome study"),
                ProjectObjective.create("Analysis if tooth paste has an impact oral health and the mouth microbiome"))
        ProjectCode projectCode = ProjectCode.random()
        Contact personReference = new Contact("John Doe", "john@doe.abcdefg")
        return Project.of(projectId, projectIntent, projectCode, personReference, personReference, personReference)
    }
}
