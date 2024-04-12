package life.qbic.datamanager.views.general.download;

import java.util.List;
import java.util.function.Function;

/**
 * The TextBuilder class is used for building data.
 * It primarily serves as a container for different types of data objects, which can be formatted
 * into rows within a text file via the provided function.
 * @param <T> the type of the objects this class operates on.
 */
public class TextFileBuilder<T> {

  private final StringBuilder contentBuilder;
  private final List<T> contentObjects;
  private final Function<T, String> convertObjectPropertiesToRow;

  public TextFileBuilder(List<T> contentObjects, Function<T, String> convertObjectPropertiesToRow) {
    this.contentObjects = contentObjects;
    this.contentBuilder = new StringBuilder();
    this.convertObjectPropertiesToRow = convertObjectPropertiesToRow;
  }

  /**
   * This method generates the final textfile as a string, one input object per line.
   * @return String representation of textfile.
   */
  public String getRowString() {
    for (T object : contentObjects) {
      String row = convertObjectPropertiesToRow.apply(object);
      contentBuilder.append(row).append("\n");
    }
    return contentBuilder.toString();
  }
}
