package life.qbic.controlling.domain.service

import life.qbic.application.commons.Result
import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.controlling.domain.model.project.Contact
import life.qbic.controlling.domain.model.project.Project
import life.qbic.controlling.domain.model.project.ProjectCode
import life.qbic.controlling.domain.model.project.ProjectIntent
import life.qbic.controlling.domain.model.project.ProjectObjective
import life.qbic.controlling.domain.model.project.ProjectTitle
import life.qbic.controlling.domain.repository.BatchRepository
import life.qbic.controlling.domain.model.sample.event.BatchRegistered
import life.qbic.controlling.domain.model.batch.Batch
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
        domainService.register("test", false, project.projectIntent.projectTitle().title(), project.getId())

        then:
        batchRegistered.eventReceived
    }
}
