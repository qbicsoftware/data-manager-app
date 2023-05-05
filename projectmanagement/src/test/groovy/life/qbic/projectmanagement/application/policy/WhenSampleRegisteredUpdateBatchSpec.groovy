package life.qbic.projectmanagement.application.policy

import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.projectmanagement.domain.project.repository.BatchRepository
import life.qbic.projectmanagement.domain.project.sample.BatchId
import life.qbic.projectmanagement.domain.project.sample.SampleId
import life.qbic.projectmanagement.domain.project.sample.event.SampleRegistered
import org.hibernate.engine.jdbc.batch.spi.Batch
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */

class WhenSampleRegisteredUpdateBatchSpec extends Specification {

    def "Given a sample registered event, the policy is executed"() {
        given:
        SampleRegistered sampleRegistered = SampleRegistered.create(BatchId.create(), SampleId.create())

        and:
        BatchRepository repo = Mock(BatchRepository.class)
        repo.find(_ as BatchId) >> Optional.ofNullable(Stub(Batch.class))

        and:
        new WhenSampleRegisteredUpdateBatch(repo)

        when:
        DomainEventDispatcher.instance().dispatch(sampleRegistered)

        then:
        1 * repo.find(_)
    }


}
