package life.qbic.controlling.domain.service

import life.qbic.application.commons.Result
import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.controlling.domain.model.project.Contact
import life.qbic.controlling.domain.model.experiment.ExperimentId
import life.qbic.controlling.domain.model.experiment.BiologicalReplicateId
import life.qbic.controlling.domain.model.experiment.vocabulary.Analyte
import life.qbic.controlling.domain.model.experiment.vocabulary.Specimen
import life.qbic.controlling.domain.model.project.Project
import life.qbic.controlling.domain.model.project.ProjectCode
import life.qbic.controlling.domain.model.project.ProjectIntent
import life.qbic.controlling.domain.model.project.ProjectObjective
import life.qbic.controlling.domain.model.project.ProjectTitle
import life.qbic.controlling.domain.repository.SampleRepository
import life.qbic.controlling.domain.model.batch.BatchId
import life.qbic.controlling.domain.model.sample.SampleOrigin
import life.qbic.controlling.domain.model.sample.event.SampleRegistered
import life.qbic.controlling.domain.model.sample.AnalysisMethod
import life.qbic.controlling.domain.model.sample.Sample
import life.qbic.controlling.domain.model.sample.SampleCode
import life.qbic.controlling.domain.model.sample.SampleId
import life.qbic.controlling.domain.model.sample.SampleRegistrationRequest
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
        Sample testSample = Sample.create(SampleCode.create("test"), new SampleRegistrationRequest("test sample", BatchId.create(), ExperimentId.create(), 1L, BiologicalReplicateId.create(), new SampleOrigin(new life.qbic.controlling.domain.model.experiment.vocabulary.Species("test"), new Specimen("test"), new Analyte("test")), AnalysisMethod.WES, ""))
        Contact who = new Contact()
        Project project = Project.create(new ProjectIntent(new ProjectTitle("a title"), new ProjectObjective("an objective")), new ProjectCode("QABCD"), who, who, who)
        Map<SampleCode, SampleRegistrationRequest> sampleCodesToRegistrationRequests = new HashMap<>()
        SampleRegistrationRequest sampleRegistrationRequest = new SampleRegistrationRequest("test sample", BatchId.create(), ExperimentId.create(), 1L, BiologicalReplicateId.create(), new SampleOrigin(new life.qbic.controlling.domain.model.experiment.vocabulary.Species("test"), new Specimen("test"), new Analyte("test")), AnalysisMethod.WES, "")
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
        Result<Collection<Sample>, SampleDomainService.ResponseCode> result = sampleDomainService.registerSamples(project, sampleCodesToRegistrationRequests)

        then:
        sampleRegistered.batchIdOfEvent.equals(result.getValue()[0].assignedBatch())
        sampleRegistered.sampleIdOfEvent.equals(result.getValue()[0].sampleId())
    }
}
