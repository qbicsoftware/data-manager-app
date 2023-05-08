package life.qbic.projectmanagement.domain.project.service

import life.qbic.application.commons.Result
import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.projectmanagement.domain.project.repository.BatchRepository
import life.qbic.projectmanagement.domain.project.sample.Batch
import life.qbic.projectmanagement.domain.project.sample.BatchId
import life.qbic.projectmanagement.domain.project.sample.event.BatchRegistered
import spock.lang.Specification

/**
 * Tests for batch domain service behaviour.
 *
 * @since 1.0.0
 */
class BatchDomainServiceSpec extends Specification {

    def "When a batch has been successfully registered, a batch registered event is dispatched"() {
        given:
        Batch testBatch = Batch.create("New Batch!")

        and:
        BatchRepository testRepo = Mock(BatchRepository)
        testRepo.add(_ as Batch) >> Result.fromValue(testBatch)
        BatchDomainService batchDomainService = new BatchDomainService(testRepo)

        and:
        DomainEventSubscriber<BatchRegistered> batchRegistered = new DomainEventSubscriber<BatchRegistered>() {

            BatchId batchIdOfEvent

            @Override
            Class<? extends DomainEvent> subscribedToEventType() {
                BatchRegistered.class
            }

            @Override
            void handleEvent(BatchRegistered event) {
                this.batchIdOfEvent = event.batchId()
            }
        }
        DomainEventDispatcher.instance().subscribe(batchRegistered)

        when:
        Result<Batch, BatchDomainService.ResponseCode> result = batchDomainService.register("test", false)

        then:
        batchRegistered.batchIdOfEvent.equals(result.getValue().batchId())
    }
}
