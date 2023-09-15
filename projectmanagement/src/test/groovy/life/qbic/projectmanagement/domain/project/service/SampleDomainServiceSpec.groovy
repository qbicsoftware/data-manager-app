package life.qbic.projectmanagement.domain.project.service

import life.qbic.application.commons.Result
import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.projectmanagement.domain.project.PersonReference
import life.qbic.projectmanagement.domain.project.Project
import life.qbic.projectmanagement.domain.project.ProjectCode
import life.qbic.projectmanagement.domain.project.ProjectIntent
import life.qbic.projectmanagement.domain.project.ProjectObjective
import life.qbic.projectmanagement.domain.project.ProjectTitle
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicateId
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen
import life.qbic.projectmanagement.domain.project.repository.SampleRepository
import life.qbic.projectmanagement.domain.project.sample.*
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
        Sample testSample = Sample.create(SampleCode.create("test"), new SampleRegistrationRequest("test sample", BatchId.create(), ExperimentId.create(), 1L, BiologicalReplicateId.create(), new SampleOrigin(new Species("test"), new Specimen("test"), new Analyte("test")), "DNA analysis", ""))
        PersonReference who = new PersonReference()
        Project project = Project.create(new ProjectIntent(new ProjectTitle("a title"), new ProjectObjective("an objective")), new ProjectCode("QABCD"), who, who, who)
        Map<SampleCode, SampleRegistrationRequest> sampleCodesToRegistrationRequests = new HashMap<>()
        SampleRegistrationRequest sampleRegistrationRequest = new SampleRegistrationRequest("test sample", BatchId.create(), ExperimentId.create(), 1L, BiologicalReplicateId.create(), new SampleOrigin(new Species("test"), new Specimen("test"), new Analyte("test")), "DNA analysis", "")
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
