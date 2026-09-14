package life.qbic.projectmanagement.application

import life.qbic.identity.api.AuthenticationToUserIdTranslator
import life.qbic.projectmanagement.application.api.ProjectOverviewLookup
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService
import life.qbic.projectmanagement.domain.model.project.*
import life.qbic.projectmanagement.domain.repository.ProjectRepository
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

class ProjectInformationServiceSpec extends Specification {

    ProjectRepository projectRepository = Mock()
    ProjectOverviewLookup projectPreviewLookup = Mock()
    ProjectAccessService projectAccessService = Mock()
    AuthenticationToUserIdTranslator authenticationToUserIdTranslator = Mock()
    ProjectInformationService projectInformationService = new ProjectInformationService(projectPreviewLookup, projectRepository, projectAccessService, authenticationToUserIdTranslator)

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

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
        Contact personReference = new Contact("Newly_Improved Jane_Doe", "TheJaneDoe@New.Improved", "", "")
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
        Contact personReference = new Contact("Newly_Improved Jane_Doe", "TheJaneDoe@New.Improved", "", "")
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
        Contact personReference = new Contact("Newly_Improved Jane_Doe", "TheJaneDoe@New.Improved", "", "")
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
        projectInformationService.updateObjective(project.getId(), projectObjective)

        then: "the project intent contains the new project objective"
        project.projectIntent.objective().objective() == projectObjective

        and: "the project is updated"
        1 * projectRepository.update(project)
    }


    def "Counting accessible project overviews delegates to the lookup with combined user and role project ids"() {
        given: "an authenticated user with a user id and one authority"
        def userId = "user-1"
        def projectIdA = ProjectId.parse("0270ce7f-4092-40e3-9c4c-ce7adb688bf5")
        def projectIdB = ProjectId.parse("1270ce7f-4092-40e3-9c4c-ce7adb688bf6")
        def roleProjectId = ProjectId.parse("2270ce7f-4092-40e3-9c4c-ce7adb688bf7")
        projectAccessService.getAccessibleProjectsForSid(userId) >> [projectIdA, projectIdB]
        projectAccessService.getAccessibleProjectsForSid("ROLE_EXAMPLE") >> [roleProjectId, projectIdA]
        authenticationToUserIdTranslator.translateToUserId(_ as Authentication) >> Optional.of(userId)
        Authentication authentication = Mock()
        authentication.authorities >> [new SimpleGrantedAuthority("ROLE_EXAMPLE")]
        SecurityContextHolder.getContext().setAuthentication(authentication)

        when:
        long result = projectInformationService.countOverview("cancer")

        then: "the lookup is called with the filter and the deduplicated accessible project ids"
        1 * projectPreviewLookup.count("cancer", [projectIdA, projectIdB, roleProjectId]) >> 17
        and:
        result == 17
    }

    def "Counting project overviews returns 0 when no authenticated user is present"() {
        given: "no user id can be translated from the authentication"
        authenticationToUserIdTranslator.translateToUserId(_ as Authentication) >> Optional.empty()
        SecurityContextHolder.getContext().setAuthentication(Mock(Authentication))

        when:
        long result = projectInformationService.countOverview("cancer")

        then: "the lookup is never queried and zero is returned"
        0 * projectPreviewLookup.count(_ as String, _ as Collection)
        result == 0
    }

    private static Project setupProject() {
        ProjectId projectId = ProjectId.parse("0270ce7f-4092-40e3-9c4c-ce7adb688bf5")
        ProjectIntent projectIntent = ProjectIntent.of(ProjectTitle.of("Oral microbiome study"),
                ProjectObjective.create("Analysis if tooth paste has an impact oral health and the mouth microbiome"))
        ProjectCode projectCode = ProjectCode.random()
        Contact personReference = new Contact("John Doe", "john@doe.abcdefg", "", "")
        return Project.of(projectId, projectIntent, projectCode, personReference, personReference, personReference)
    }
}
