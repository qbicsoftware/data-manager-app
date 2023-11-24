package life.qbic.projectmanagement.domain.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleRegistrationRequest;
import life.qbic.projectmanagement.domain.model.sample.event.SampleDeleted;
import life.qbic.projectmanagement.domain.model.sample.event.SampleRegistered;
import life.qbic.projectmanagement.domain.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Sample Domain Service</b>
 * <p>
 * Service that handles {@link Sample} creation and deletion events, that need to dispatch domain
 * events.
 *
 * @since 1.0.0
 */
@Service
public class SampleDomainService {

    private final SampleRepository sampleRepository;

    @Autowired
    public SampleDomainService(SampleRepository sampleRepository) {
        this.sampleRepository = Objects.requireNonNull(sampleRepository);
    }

    public Result<Collection<Sample>, ResponseCode> registerSamples(Project project, Map<SampleCode,
        SampleRegistrationRequest> sampleCodesToRegistrationRequests) {
        Objects.requireNonNull(sampleCodesToRegistrationRequests);
        Collection<Sample> samplesToRegister = new ArrayList<>();
        sampleCodesToRegistrationRequests.forEach((sampleCode, sampleRegistrationRequest) -> {
            var sample = Sample.create(sampleCode, sampleRegistrationRequest);
            samplesToRegister.add(sample);
        });
        Result<Collection<Sample>, ResponseCode> result = this.sampleRepository.addAll(project,
            samplesToRegister);
        result.onValue(createdSamples ->
                createdSamples.forEach(this::dispatchSuccessfulSampleRegistration))
            .onError(Result::fromError);
        return result;
    }

    public Result<Collection<Sample>, ResponseCode> updateSamples(
        Collection<Sample> updatedSamples) {
        Objects.requireNonNull(updatedSamples);
        Result<Collection<Sample>, ResponseCode> result = this.sampleRepository.updateAll(
            updatedSamples);
        result.onValue(createdSamples ->
                createdSamples.forEach(this::dispatchSuccessfulSampleRegistration))
            .onError(Result::fromError);
        return result;
    }

    public Result<Collection<Sample>, ResponseCode> deleteSamples(List<Sample> samples) {
        Objects.requireNonNull(samples);
        var result = this.sampleRepository.deleteAll(samples);
        result.onValue(deletedSamples ->
                deletedSamples.forEach(this::dispatchSuccessfulSampleDeletion))
            .onError(Result::fromError);
        return result;
    }

    private void dispatchSuccessfulSampleRegistration(Sample sample) {
        SampleRegistered sampleRegistered = SampleRegistered.create(sample.assignedBatch(), sample.sampleId());
        DomainEventDispatcher.instance().dispatch(sampleRegistered);
    }

    private void dispatchSuccessfulSampleDeletion(Sample sample) {
        SampleDeleted sampleDeleted = SampleDeleted.create(sample.assignedBatch(),
            sample.sampleId());
        DomainEventDispatcher.instance().dispatch(sampleDeleted);
    }

    /**
     * Response error codes for the sample registration
     *
     * @since 1.0.0
     */
    public enum ResponseCode {
        REGISTRATION_FAILED, DELETION_FAILED, DATA_ATTACHED_TO_SAMPLES, UPDATE_FAILED
    }

}
