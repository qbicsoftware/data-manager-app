package life.qbic.projectmanagement.domain.project.service

import life.qbic.application.commons.Result
import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.domain.concepts.communication.CommunicationService
import life.qbic.projectmanagement.application.ProjectInformationService
import life.qbic.projectmanagement.application.batch.BatchRegistrationService
import life.qbic.projectmanagement.application.sample.SampleRegistrationService
import life.qbic.projectmanagement.domain.project.Contact
import life.qbic.projectmanagement.domain.project.Project
import life.qbic.projectmanagement.domain.project.ProjectCode
import life.qbic.projectmanagement.domain.project.ProjectId
import life.qbic.projectmanagement.domain.project.ProjectIntent
import life.qbic.projectmanagement.domain.project.ProjectObjective
import life.qbic.projectmanagement.domain.project.ProjectTitle
import life.qbic.projectmanagement.domain.project.repository.BatchRepository
import life.qbic.projectmanagement.domain.project.sample.Batch
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest
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
