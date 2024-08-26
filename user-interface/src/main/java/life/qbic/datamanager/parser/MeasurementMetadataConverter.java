package life.qbic.datamanager.parser;

import java.util.List;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface MeasurementMetadataConverter {

  /**
   * Takes an instance of {@link ParsingResult} and tries to convert it to known implementations of
   * the {@link MeasurementMetadata} interface.
   * <p>
   * Currently supported implementations are:
   *
   * <ul>
   *   <li>NGS Measurement Metadata {@link life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata}</li>
   *   <li>Proteomics Measurement Metadata {@link life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata}</li>
   * </ul>
   *
   * @param parsingResult       the parsing result to take as input for the conversion.
   * @param ignoreMeasurementId weather to ignore the measurement identifier or not
   * @return a list of converted implementations of {@link MeasurementMetadata}.
   * @throws UnknownMetadataTypeException if no matching implementation of
   *                                      {@link MeasurementMetadata} can be associated from the
   *                                      provided {@link ParsingResult#keys()}.
   * @since 1.4.0
   */
  List<? extends MeasurementMetadata> convert(ParsingResult parsingResult,
      boolean ignoreMeasurementId)
      throws UnknownMetadataTypeException;

  class UnknownMetadataTypeException extends RuntimeException {

    public UnknownMetadataTypeException(String message) {
      super(message);
    }
  }

}
