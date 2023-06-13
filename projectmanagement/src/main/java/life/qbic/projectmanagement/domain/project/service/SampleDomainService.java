package life.qbic.projectmanagement.domain.project.service;

import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.domain.project.repository.SampleRepository;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleCode;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;
import life.qbic.projectmanagement.domain.project.sample.event.SampleRegistered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

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

    /**
     * Registers a new sample for measurement.
     *
     * @param sampleCode                A unique {@link SampleCode} which defines the sample to be registered
     * @param sampleRegistrationRequest The {@link SampleRegistrationRequest} containing the information to be stored within the sample
     * @return A {@link Result} object with the created {@link Sample} or an error with
     * {@link ResponseCode}
     * @since 1.0.0
     */
    public Result<Sample, ResponseCode> registerSample(SampleCode sampleCode,
                                                       SampleRegistrationRequest sampleRegistrationRequest) {
        Objects.requireNonNull(sampleCode);
        Objects.requireNonNull(sampleRegistrationRequest);
        var sample = Sample.create(sampleCode, sampleRegistrationRequest);
        Result<Sample, ResponseCode> result = this.sampleRepository.add(sample);
        // For successful registration transactions we dispatch the event
        result.onValue(this::dispatchSuccessfulSampleRegistration);
        return result;
    }

    public Result<Collection<Sample>, ResponseCode> registerSamples(Map<SampleCode, SampleRegistrationRequest> sampleCodesToRegistrationRequests) {
        Objects.requireNonNull(sampleCodesToRegistrationRequests);
        Collection<Sample> samplesToRegister = new ArrayList<>();
        sampleCodesToRegistrationRequests.forEach((sampleCode, sampleRegistrationRequest) -> {
            var sample = Sample.create(sampleCode, sampleRegistrationRequest);
            samplesToRegister.add(sample);
        });
        Result<Collection<Sample>, ResponseCode> result = this.sampleRepository.addAll(samplesToRegister);
        result.onValue(createdSamples ->
                createdSamples.forEach(this::dispatchSuccessfulSampleRegistration)).onError(Result::fromError);
        return result;
    }

    private void dispatchSuccessfulSampleRegistration(Sample sample) {
        SampleRegistered sampleRegistered = SampleRegistered.create(sample.assignedBatch(), sample.sampleId());
        DomainEventDispatcher.instance().dispatch(sampleRegistered);
    }

    /**
     * Response error codes for the sample registration
     *
     * @since 1.0.0
     */
    public enum ResponseCode {
        REGISTRATION_FAILED
    }

}
