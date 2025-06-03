package life.qbic.datamanager.files.parsing;

import java.util.List;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;

/**
 * <b>Measurement Metadata Converter</b>
 * <p>
 * Measurement metadata converter enable the client to process a {@link ParsingResult} object and
 * convert them into known implementations of the {@link MeasurementMetadata} interface.
 *
 * @since 1.4.0
 * @deprecated since 1.10.0, use
 * {@link life.qbic.datamanager.files.parsing.converters.MetadataConverterV2} and
 * {@link life.qbic.datamanager.files.parsing.converters.ConverterFactory} instead.
 */
@Deprecated(since = "1.10.0", forRemoval = true)
public interface MeasurementMetadataConverter {

  /**
   * Takes an instance of {@link ParsingResult} and tries to convert it to known implementations of
   * the {@link MeasurementMetadata} interface.
   * <p>
   * Currently supported implementations are:
   *
   * <ul>
   *   <li>NGS Measurement Metadata {@link NGSMeasurementMetadata}</li>
   *   <li>Proteomics Measurement Metadata {@link ProteomicsMeasurementMetadata}</li>
   * </ul>
   *
   * @param parsingResult the parsing result to take as input for the conversion.
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
