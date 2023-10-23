package life.qbic.controlling.application

import life.qbic.controlling.application.api.ProjectPreviewLookup
import life.qbic.controlling.domain.model.project.Contact
import life.qbic.controlling.domain.model.project.Project
import life.qbic.controlling.domain.model.project.ProjectCode
import life.qbic.controlling.domain.model.project.ProjectId
import life.qbic.controlling.domain.model.project.ProjectIntent
import life.qbic.controlling.domain.model.project.ProjectObjective
import life.qbic.controlling.domain.model.project.ProjectTitle
import life.qbic.controlling.domain.repository.ProjectRepository
import spock.lang.Specification

class ProjectInformationServiceSpec extends Specification {

    ProjectRepository projectRepository = Mock()
    ProjectPreviewLookup projectPreviewLookup = Mock()
    ProjectInformationService projectInformationService = new ProjectInformationService(projectPreviewLookup, projectRepository)

    def project = setupProject()

    def "Updating the project Title via the ProjectInformationService sets the new title within the projectIntent of the project"() {
        given:
        projectRepository.find(project.getId()) >> Optional.of(project)
        projectRepository.find((ProjectId) _) >> Optional.empty()

        when: "the project title is updated for a project"
        ProjectTitle projectTitle = ProjectTitle.of("My Awesome Title")
        projectInformationService.updateTitle(project.getId(), projectTitle.title())

        then: "the project intent contains the new project title"
        project.projectIntent.projectTitle().title() == projectTitle.title()

        and: "the project is updated"
        1 * projectRepository.update(project)
    }

    def "Updating the Project Manager via the ProjectInformationService sets the provided PersonReference as the Project Manager of the project"() {
        given:
        projectRepository.find(project.getId()) >> Optional.of(project)
        projectRepository.find((ProjectId) _) >> Optional.empty()

        when: "the project manager is updated for a project"
        Contact personReference = new Contact("Newly_Improved Jane_Doe", "TheJaneDoe@New.Improved")
        projectInformationService.manageProject(project.getId(), personReference)

        then: "the project contains the new person reference as project manager"
        project.projectManager.fullName() == personReference.fullName()
        project.projectManager.emailAddress() == personReference.emailAddress()

        and: "the project is updated"
        1 * projectRepository.update(project)
    }

    def "Updating the Principal investigator via the ProjectInformationService sets the provided PersonReference as the principal investigator of the project"() {
        given:
        projectRepository.find(project.getId()) >> Optional.of(project)
        projectRepository.find((ProjectId) _) >> Optional.empty()

        when: "the principal investigator is updated for a project"
        Contact personReference = new Contact("Newly_Improved Jane_Doe", "TheJaneDoe@New.Improved")
        projectInformationService.investigateProject(project.getId(), personReference)

        then: "the project contains the new person reference as principal investigator"
        project.principalInvestigator.fullName() == personReference.fullName()
        project.principalInvestigator.emailAddress() == personReference.emailAddress()

        and: "the project is updated"
        1 * projectRepository.update(project)
    }

    def "Updating the responsible Person via the ProjectInformationService sets the provided PersonReference as the responsible Person of the project"() {
        given:
        projectRepository.find(project.getId()) >> Optional.of(project)
        projectRepository.find((ProjectId) _) >> Optional.empty()

        when: "the responsible Person is updated for a project"
        Contact personReference = new Contact("Newly_Improved Jane_Doe", "TheJaneDoe@New.Improved")
        projectInformationService.setResponsibility(project.getId(), personReference)

        then: "the project contains the new person reference as responsible person"
        project.responsiblePerson.get().fullName() == personReference.fullName()
        project.responsiblePerson.get().emailAddress() == personReference.emailAddress()

        and: "the project is updated"
        1 * projectRepository.update(project)
    }

    def "Updating the project objective via the ProjectInformationService sets the new objective within the projectIntent of the project"() {
        given:
        projectRepository.find(project.getId()) >> Optional.of(project)
        projectRepository.find((ProjectId) _) >> Optional.empty()

        when: "the project objective is updated for a project"
        String projectObjective = "All your objectives are belong to us"
        projectInformationService.stateObjective(project.getId(), projectObjective)

        then: "the project intent contains the new project objective"
        project.projectIntent.objective().objective() == projectObjective

        and: "the project is updated"
        1 * projectRepository.update(project)
    }


    private static Project setupProject() {
        ProjectId projectId = ProjectId.parse("0270ce7f-4092-40e3-9c4c-ce7adb688bf5")
        ProjectIntent projectIntent = ProjectIntent.of(ProjectTitle.of("Oral microbiome study"),
                ProjectObjective.create("Analysis if tooth paste has an impact oral health and the mouth microbiome"))
        ProjectCode projectCode = ProjectCode.random()
        Contact personReference = new Contact("John Doe", "john@doe.abcdefg")
        return Project.of(projectId, projectIntent, projectCode, personReference, personReference, personReference)
    }
}
