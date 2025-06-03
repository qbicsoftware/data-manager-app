package life.qbic.datamanager.files.parsing.converters;

import java.util.List;
import life.qbic.datamanager.files.parsing.ParsingResult;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface MetadataConverterV2<T> {

  List<T> convert(ParsingResult result);

}
