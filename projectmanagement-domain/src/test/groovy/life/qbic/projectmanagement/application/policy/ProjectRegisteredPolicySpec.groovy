package life.qbic.projectmanagement.application.policy

import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.projectmanagement.application.policy.directive.CreateNewSampleStatisticsEntry
import life.qbic.projectmanagement.domain.project.ProjectId
import life.qbic.projectmanagement.domain.project.event.ProjectRegisteredEvent
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ProjectRegisteredPolicySpec extends Specification {

    def "Given a project registered event, ensure that the directive to add a sample statistics entry is executed"() {
        given:
        ProjectRegisteredEvent projectRegisteredEvent = ProjectRegisteredEvent.create(ProjectId.create())

        and:
        CreateNewSampleStatisticsEntry createNewSampleStatisticsEntry = Mock(CreateNewSampleStatisticsEntry)
        createNewSampleStatisticsEntry.subscribedToEventType() >> ProjectRegisteredEvent.class

        and:
        new ProjectRegisteredPolicy(createNewSampleStatisticsEntry)

        when:
        DomainEventDispatcher.instance().dispatch(projectRegisteredEvent)

        then:
        1 * createNewSampleStatisticsEntry.handleEvent(projectRegisteredEvent)
    }

}
