package life.qbic.projectmanagement.application.policy

import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.projectmanagement.application.policy.directive.AddSampleToBatch
import life.qbic.projectmanagement.domain.project.sample.BatchId
import life.qbic.projectmanagement.domain.project.sample.SampleId
import life.qbic.projectmanagement.domain.project.sample.event.SampleRegistered
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */

class SampleRegisteredPolicySpec extends Specification {

    def "Given a sample registered event, the directive to add sample to batch is executed"() {
        given:
        SampleRegistered sampleRegistered = SampleRegistered.create(BatchId.create(), SampleId.create())

        and:
        AddSampleToBatch addSampleToBatch = Mock(AddSampleToBatch.class)
        addSampleToBatch.subscribedToEventType() >> SampleRegistered.class

        and:
        SampleRegisteredPolicy sampleRegisteredPolicy = new SampleRegisteredPolicy(addSampleToBatch)

        when:
        DomainEventDispatcher.instance().dispatch(sampleRegistered)

        then:
        1 * addSampleToBatch.handleEvent(sampleRegistered)
    }


}
