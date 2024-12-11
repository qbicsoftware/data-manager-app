package life.qbic.projectmanagement.application.policy

import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponDeletionEvent
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.model.project.event.ProjectChanged
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */

class ProjectChangedPolicySpec extends Specification {

    def "Given a project changed event, the project update directive is executed"() {
        given:
        ProjectChanged projectChanged = ProjectChanged.create(ProjectId.create())

        and:
        UpdateProjectUponDeletionEvent modifyProject = Mock(UpdateProjectUponDeletionEvent.class)
        modifyProject.subscribedToEventType() >> ProjectChanged.class

        and:
        new ProjectChangedPolicy(modifyProject)

        when:
        DomainEventDispatcher.instance().dispatch(projectChanged)

        then:
        1 * modifyProject.handleEvent(projectChanged)
    }


}
