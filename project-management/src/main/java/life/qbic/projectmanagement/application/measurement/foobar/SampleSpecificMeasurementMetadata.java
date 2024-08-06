package life.qbic.projectmanagement.application.measurement.foobar;

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
public interface SampleSpecificMeasurementMetadata {

  SampleCode sampleCode();

  SampleId sampleId();
}
