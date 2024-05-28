package life.qbic.projectmanagement.application.policy

import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.projectmanagement.application.policy.directive.AddSampleToBatch
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponSampleCreation
import life.qbic.projectmanagement.domain.model.batch.BatchId
import life.qbic.projectmanagement.domain.model.sample.SampleId
import life.qbic.projectmanagement.domain.model.sample.event.SampleRegistered
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */

class SampleRegisteredPolicySpec extends Specification {

    def "Given a sample registered event, the respective directive is executed"() {
        given:
        SampleRegistered sampleRegistered = SampleRegistered.create(BatchId.create(), SampleId.create())

        and:
        AddSampleToBatch addSampleToBatch = Mock(AddSampleToBatch.class)
        addSampleToBatch.subscribedToEventType() >> SampleRegistered.class
        UpdateProjectUponSampleCreation updateProject = Mock(UpdateProjectUponSampleCreation.class)
        updateProject.subscribedToEventType() >> SampleRegistered.class

        and:
        SampleRegisteredPolicy sampleRegisteredPolicy = new SampleRegisteredPolicy(addSampleToBatch, updateProject)

        when:
        DomainEventDispatcher.instance().dispatch(sampleRegistered)

        then:
        1 * addSampleToBatch.handleEvent(sampleRegistered)
        1 * updateProject.handleEvent(sampleRegistered)
    }


}
