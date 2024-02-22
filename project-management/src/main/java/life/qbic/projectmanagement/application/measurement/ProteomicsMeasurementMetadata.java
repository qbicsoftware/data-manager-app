package life.qbic.projectmanagement.application.measurement;

import java.util.Collection;
import java.util.List;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * <b>Proteomics Measurement Metadata</b>
 * <p>
 * Indicating proteomics measurement metadata registration request.
 *
 * @since 1.0.0
 */
public record ProteomicsMeasurementMetadata(Collection<SampleCode> sampleCodes, String organisationId) implements MeasurementMetadata {

}
