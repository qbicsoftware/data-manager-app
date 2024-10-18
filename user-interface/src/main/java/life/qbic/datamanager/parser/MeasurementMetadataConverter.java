package life.qbic.datamanager.parser;

import java.util.List;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;

/**
 * <b>Measurement Metadata Converter</b>
 * <p>
 * Measurement metadata converter enable the client to process a {@link ParsingResult} object and
 * convert them into known implementations of the {@link MeasurementMetadata} interface.
 *
 * @since 1.4.0
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
   * @return a list of converted implementations of {@link MeasurementMetadata}.
   * @throws UnknownMetadataTypeException if no matching implementation of
   *                                      {@link MeasurementMetadata} can be associated from the
   *                                      provided {@link ParsingResult#columnMap()}.
   * @since 1.4.0
   */
  List<? extends MeasurementMetadata> convertRegister(ParsingResult parsingResult)
      throws UnknownMetadataTypeException;

  List<MeasurementMetadata> convertEdit(ParsingResult parsingResult)
      throws UnknownMetadataTypeException, MissingSampleIdException;

  class UnknownMetadataTypeException extends RuntimeException {

    public UnknownMetadataTypeException(String message) {
      super(message);
    }
  }

  class MissingSampleIdException extends RuntimeException {
    public MissingSampleIdException(String message) {
      super(message);
    }
  }

}
