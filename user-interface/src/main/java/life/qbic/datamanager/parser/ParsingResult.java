package life.qbic.datamanager.parser;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * <b>Parsing Result</b>
 * <p>
 * A parsing result represents a two dimensional grid of information.
 * <p>
 * A grid is contained of values, which is a list of rows. Every row is a list of String values.
 * <p>
 * Its main feature is a property list (=> 'keys') with its String value and position in every row.
 * <p>
 * Example for a grid:
 *
 * <table>
 *   <tr>
 *   <th>key 'A' - position 0</th>
 *   <th>key 'B' - position 1</th>
 *   <th>key 'C' - position 2</th>
 *   </tr>
 *   <tr>
 *     <td>Value A1</td>
 *     <td>Value B1</td>
 *     <td>Value C1</td>
 *   </tr>
 *   <tr>
 *     <td>Value A2</td>
 *     <td>Value B2</td>
 *     <td>Value C2</td>
 *   </tr>
 *   <tr>
 *     <td>...</td>
 *     <td>...</td>
 *     <td>...</td>
 *   </tr>
 * </table>
 * <p>
 * So the resulting stored positions of every key in a row can be accessed via {@link #keys()} and would look like:
 *
 * <ul>
 *   <li>A - 0</li>
 *   <li>B - 1</li>
 *   <li>C - 2</li>
 * </ul>
 * <p>
 * and iterating through the rows would look like:
 *
 * <ul>
 *   <li>Value A1, Value B1, Value C1</li>
 *   <li>Value A2, Value B2, Value C2</li>
 *   <li>...</li>
 * </ul>
 *
 * @since 1.4.0
 */
public record ParsingResult(Map<String, Integer> keys, List<Row> rows) {

  public ParsingResult(Map<String, Integer> keys, List<Row> rows) {
    this.keys = Map.copyOf(keys);
    this.rows = List.copyOf(rows);
  }

  public Stream<Row> rowsStream() {
    return rows.stream();
  }

  public Iterator<Row> iterator() {
    return rows.iterator();
  }

  public List<String> getRow(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= rows.size()) {
      throw new IndexOutOfBoundsException(
          "Row index out of bounds: %s but size is %s".formatted(rowIndex, rows.size()));
    }
    return rows.get(rowIndex).values;
  }

  public record Row(List<String> values) {

    public Row(List<String> values) {
      this.values = List.copyOf(values);
    }

  }

}
