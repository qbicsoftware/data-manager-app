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

  List<MeasurementMetadata> convert(ParsingResult parsingResult);

}
