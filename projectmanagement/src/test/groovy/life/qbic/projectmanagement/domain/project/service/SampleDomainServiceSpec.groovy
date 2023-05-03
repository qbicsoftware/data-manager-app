package life.qbic.projectmanagement.domain.project.service

import life.qbic.application.commons.Result
import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicateId
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen
import life.qbic.projectmanagement.domain.project.repository.SampleRepository
import life.qbic.projectmanagement.domain.project.sample.BatchId
import life.qbic.projectmanagement.domain.project.sample.Sample
import life.qbic.projectmanagement.domain.project.sample.SampleId
import life.qbic.projectmanagement.domain.project.sample.SampleOrigin
import life.qbic.projectmanagement.domain.project.sample.event.SampleRegistered
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
        Sample testSample = Sample.create("test sample", BatchId.create(), ExperimentId.create(), 1L, BiologicalReplicateId.create(), new SampleOrigin(new Species("test"), new Specimen("test"), new Analyte("test")))

        and:
        SampleRepository testRepo = Mock(SampleRepository)
        testRepo.add(_ as Sample) >> Result.fromValue(testSample)
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
        Result<Sample, SampleDomainService.ResponseCode> result = sampleDomainService.registerSample("test sample", BatchId.create(), ExperimentId.create(), 1L, BiologicalReplicateId.create(), new SampleOrigin(new Species("test"), new Specimen("test"), new Analyte("test")))

        then:
        sampleRegistered.batchIdOfEvent.equals(result.getValue().assignedBatch())
        sampleRegistered.sampleIdOfEvent.equals(result.getValue().sampleId())
    }
}
