package life.qbic.projectmanagement.application.measurement.foobar;

import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public non-sealed interface SingleSampleMeasurement extends Measurement {

  SampleId measuredSample();
}
