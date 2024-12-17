package life.qbic.projectmanagement.domain.service

import life.qbic.application.commons.Result
import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.projectmanagement.domain.model.batch.Batch
import life.qbic.projectmanagement.domain.model.batch.BatchId
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.*
import life.qbic.projectmanagement.domain.model.sample.event.BatchDeleted
import life.qbic.projectmanagement.domain.model.sample.event.BatchRegistered
import life.qbic.projectmanagement.domain.repository.BatchRepository
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
        Contact who = new Contact()
        Project project = Project.create(new ProjectIntent(new ProjectTitle("a title"),
                new ProjectObjective("an objective")), new ProjectCode("QABCD"), who, who, who)

        and:
        BatchRepository testRepo = Mock(BatchRepository)
        BatchDomainService domainService = new BatchDomainService(testRepo)
        testRepo.add(_ as Batch) >> Result.fromValue(testBatch)

        and:
        DomainEventSubscriber<BatchRegistered> batchRegistered = new DomainEventSubscriber<BatchRegistered>() {

            boolean eventReceived = false

            @Override
            Class<? extends DomainEvent> subscribedToEventType() {
                BatchRegistered.class
            }

            @Override
            void handleEvent(BatchRegistered event) {
                this.eventReceived = true
            }
        }
        DomainEventDispatcher.instance().subscribe(batchRegistered)

        when:
        domainService.register("test", false, project.projectIntent.projectTitle().title(), project.getId(), ExperimentId.create())

        then:
        batchRegistered.eventReceived
    }


    def "When a batch has been successfully deleted, a batch deletion event is dispatched"() {
        given:
        Batch testBatch = Batch.create("Please set me free")

        and:
        BatchRepository testRepo = Mock(BatchRepository)
        testRepo.deleteById(_ as BatchId) >> Result.fromValue(testBatch.batchId())
        BatchDomainService batchDomainService = new BatchDomainService(testRepo)

        and:
        DomainEventSubscriber<BatchDeleted> batchDeleted = new DomainEventSubscriber<BatchDeleted>() {

            boolean eventReceived = false

            @Override
            Class<? extends DomainEvent> subscribedToEventType() {
                BatchDeleted.class
            }

            @Override
            void handleEvent(BatchDeleted event) {
                this.eventReceived = true
            }
        }
        DomainEventDispatcher.instance().subscribe(batchDeleted)

        when:
        batchDomainService.deleteBatch(testBatch.batchId(), ProjectId.create())

        then:
        batchDeleted.eventReceived
    }
}
