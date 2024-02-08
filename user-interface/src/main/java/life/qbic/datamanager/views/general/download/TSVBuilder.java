package life.qbic.datamanager.views.general.download;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The TSVBuilder class is used for building tab-separated values (TSV) data.
 * It primarily serves as a container for different types of data objects, which can be formatted
 * into a TSV string. Each 'column' in the TSV formatting corresponds to a 'getter' function for a
 * specific data value of `T` object.
 * @param <T> the type of the objects this class operates on.
 */
public class TSVBuilder<T> {
  private StringBuilder contentBuilder;
  private List<String> header;
  private List<Function<T,String>> getters;
  private List<T> contentObjects;

  public TSVBuilder(List<T> contentObjects) {
    this.contentBuilder = new StringBuilder();
    this.contentObjects = contentObjects;
    this.header = new ArrayList<>();
    this.getters = new ArrayList<>();
  }

  /**
   * Method to add a column to the TSV structure.
   * Each column corresponds to a specific 'getter' function that retrieves a data value from `T` object.
   * @param colName Name of the column.
   * @param getData Getter function that retrieves specific data from `T`.
   */
  public void addColumn(String colName, Function<T,String> getData) {
    header.add(colName);
    getters.add(getData);
  }

  /**
   * This method generates the final TSV as a string.
   * @return String representation of the TSV.
   */
  public String getTSVString() {
    contentBuilder.append(String.join("\t", header)).append("\n");
    for(T object : contentObjects) {
      List<String> row = new ArrayList<>();
      for(Function<T,String> getter : getters) {
        row.add(getter.apply(object));
      }
      contentBuilder.append(String.join("\t", row)).append("\n");
    }
    return contentBuilder.toString();
  }
}