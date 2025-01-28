package life.qbic.datamanager.files.structure;

import java.util.Optional;
import life.qbic.datamanager.importing.parser.ExampleProvider.Helper;

/**
 * A column in a file (for example xlsx files or TSV files)
 *
 * @since 1.8.0
 */
public interface Column {

  /**
   * The index of the column 0-based
   *
   * @return the index of this column
   */
  int index();

  /**
   * The name of the column as it can be used in a header cell for the column
   *
   * @return the name of the column
   */
  String headerName();

  /**
   * @return true if filling out this column is mandatory; false otherwise
   */
  boolean isMandatory();

  /**
   * @return true if the content of this column is only provided and not considered relevant for
   * parsing; false otherwise
   */
  boolean isReadOnly();

  /**
   * Information on how to fill this column
   *
   * @return a helper with information on how to fill this column. Can be {@link Optional#empty()}
   * if no help is provided.
   */
  Optional<Helper> getFillHelp();

}
