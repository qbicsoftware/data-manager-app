package life.qbic.projectmanagement.application.policy

import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.projectmanagement.application.policy.directive.DeleteSampleFromBatch
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectLastModified
import life.qbic.projectmanagement.domain.model.batch.BatchId
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.model.project.event.ProjectChanged
import life.qbic.projectmanagement.domain.model.sample.SampleId
import life.qbic.projectmanagement.domain.model.sample.event.SampleDeleted
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */

class SampleDeletedPolicySpec extends Specification {

    def "Given a sample deletion event, the respective directive is executed"() {
        given:
        SampleDeleted sampleDeleted = SampleDeleted.create(BatchId.create(), SampleId.create())

        and:
        DeleteSampleFromBatch deleteSampleFromBatch = Mock(DeleteSampleFromBatch.class)
        deleteSampleFromBatch.subscribedToEventType() >> SampleDeleted.class
        UpdateProjectLastModified modifyProject = Mock(UpdateProjectLastModified.class)
        modifyProject.subscribedToEventType() >> ProjectChanged.class

        and:
        SampleDeletedPolicy sampleDeletedPolicy = new SampleDeletedPolicy(deleteSampleFromBatch)

        when:
        DomainEventDispatcher.instance().dispatch(sampleDeleted)

        then:
        1 * deleteSampleFromBatch.handleEvent(sampleDeleted)
    }


}
