package life.qbic.datamanager.files.parsing.converters;

import java.util.List;
import life.qbic.datamanager.files.parsing.MetadataConverter;
import life.qbic.datamanager.files.parsing.ParsingResult;

/**
 * Metadata Converter V2
 * <p>
 * Successor of the {@link MetadataConverter} interface.
 * <p>
 * The interface is used to provide a generic way of converting a {@link ParsingResult} into a list
 * of different metadata types.
 *
 * @since 1.10.0
 */
public interface MetadataConverterV2<T> {

  /**
   * Converts a {@link ParsingResult} into a list of metadata objects of type {@link T}.
   * <p>
   * The implementation must guarantee the order of the resulting list to be the same as the order
   * of rows in the {@link ParsingResult}.
   *
   * @param result the {@link ParsingResult} to be converted into metadata objects of a type
   *               {@link T}
   * @return a list of metadata objects of type {@link T}
   * @since 1.10.0
   */
  List<T> convert(ParsingResult result);

}
