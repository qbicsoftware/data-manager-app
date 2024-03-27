package life.qbic.projectmanagement.application.measurement;

import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;

/**
 * <b>NGS Measurement Metadata</b>
 * <p>
 * Indicating NGS measurement metadata registration request.
 *
 * @since 1.0.0
 */
public class NGSMeasurementMetadata implements MeasurementMetadata {


  public String instrumentCURIE(){
    return "";
  }

  @Override
  public MeasurementCode measurementCode() {
    return null;
  }
}
