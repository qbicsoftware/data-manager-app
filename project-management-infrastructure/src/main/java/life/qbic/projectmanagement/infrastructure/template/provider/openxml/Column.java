package life.qbic.projectmanagement.infrastructure.template.provider.openxml;

import java.util.Optional;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.ExampleProvider.Helper;

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
   * Provides contextual help for filling out this column.
   *
   * <p>
   * The returned {@link Helper} contains example values and explanatory text,
   * which may be shown as tooltips or spreadsheet guidance.
   * </p>
   * Not all columns provide contextual help. For columns without help information available, {@link Optional#empty()} is returned.
   *
   * @return an {@link Optional} containing fill-in help information
   */
  Optional<Helper> getFillHelp();

}
