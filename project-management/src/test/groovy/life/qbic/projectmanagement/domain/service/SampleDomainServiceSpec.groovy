package life.qbic.projectmanagement.domain.service

import life.qbic.application.commons.Result
import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.projectmanagement.domain.model.OntologyTerm
import life.qbic.projectmanagement.domain.model.batch.BatchId
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.*
import life.qbic.projectmanagement.domain.model.sample.*
import life.qbic.projectmanagement.domain.model.sample.event.SampleRegistered
import life.qbic.projectmanagement.domain.repository.SampleRepository
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class SampleDomainServiceSpec extends Specification {

    def "When a sample has been successfully registered, a sample registered event is dispatched"() {
        given:
        Sample testSample = Sample.create(SampleCode.create("test"), new SampleRegistrationRequest("test sample", "patient 1", BatchId.create(), ExperimentId.create(), 1L, new SampleOrigin(new OntologyTerm(), new OntologyTerm(), new OntologyTerm()), AnalysisMethod.WES, ""))
        Contact who = new Contact()
        Project project = Project.create(new ProjectIntent(new ProjectTitle("a title"), new ProjectObjective("an objective")), new ProjectCode("QABCD"), who, who, who)
        Map<SampleCode, SampleRegistrationRequest> sampleCodesToRegistrationRequests = new HashMap<>()
        SampleRegistrationRequest sampleRegistrationRequest = new SampleRegistrationRequest("test sample", "patient 1", BatchId.create(), ExperimentId.create(), 1L, new SampleOrigin(new OntologyTerm(), new OntologyTerm(), new OntologyTerm()), AnalysisMethod.WES, "")
        sampleCodesToRegistrationRequests.put(SampleCode.create("test"), sampleRegistrationRequest)

        and:
        SampleRepository testRepo = Mock(SampleRepository)
        testRepo.addAll(_ as Project, _ as Collection<Sample>) >> Result.fromValue(Arrays.asList(testSample))
        SampleDomainService sampleDomainService = new SampleDomainService(testRepo)

        and:
        DomainEventSubscriber<SampleRegistered> sampleRegistered = new DomainEventSubscriber<SampleRegistered>() {

            SampleId sampleIdOfEvent

            BatchId batchIdOfEvent

            @Override
            Class<? extends DomainEvent> subscribedToEventType() {
                SampleRegistered.class
            }

            @Override
            void handleEvent(SampleRegistered event) {
                this.sampleIdOfEvent = event.registeredSample()
                this.batchIdOfEvent = event.assignedBatch()
            }
        }
        DomainEventDispatcher.instance().subscribe(sampleRegistered)

        when:
        sampleDomainService.registerSamples(project, sampleCodesToRegistrationRequests)

        then:

        sampleRegistered.batchIdOfEvent.equals(sampleRegistrationRequest.assignedBatch())
        SampleId.parse(sampleRegistered.sampleIdOfEvent.value())
        notThrown(IllegalArgumentException)
    }
}
