package life.qbic.projectmanagement.application.measurement.foobar.proteomics;

import life.qbic.projectmanagement.application.measurement.foobar.SampleSpecificMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public record ProteomicsSampleSpecificMetadata(SampleId sampleId, SampleCode sampleCode) implements
    SampleSpecificMeasurementMetadata {

}
