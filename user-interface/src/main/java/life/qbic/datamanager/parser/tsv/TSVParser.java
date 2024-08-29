package life.qbic.datamanager.parser.tsv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import life.qbic.datamanager.parser.MetadataParser;
import life.qbic.datamanager.parser.ParsingResult;
import life.qbic.datamanager.parser.ParsingResult.Row;
import life.qbic.datamanager.parser.Sanitizer;

/**
 * <b>TSV Parser</b>
 * <p>
 * Tab-seperated value format support for the {@link MetadataParser} interface.
 * <p>
 * Support for UTF-16 encoding available.
 * <p>
 * This implementation always considers the first line as the header, and will use its information
 * to create the {@link ParsingResult#keys()} in the returned {@link ParsingResult} object
 * instance.
 *
 * @since 1.4.0
 */
public class TSVParser implements MetadataParser {

  private static final String VALUE_SEPARATOR = "\t";

  private TSVParser() {

  }

  public static TSVParser create() {
    return new TSVParser();
  }

  /**
   * Prevents nasty {@link IndexOutOfBoundsException} and supports a more fluent API and cleaner
   * code through the usage of Java's {@link Optional}.
   *
   * @param array the array to access an element from
   * @param index the index of the element in the array to access
   * @return the array element at position of the index wrapped in {@link Optional}, or
   * {@link Optional#empty}, if the index is out of bounds.
   * @since 1.4.0
   */
  private static Optional<String> safeAccess(String[] array, Integer index) {
    if (index >= array.length || index < 0) {
      return Optional.empty();
    }
    return Optional.of(array[index]);
  }

  @Override
  public ParsingResult parse(InputStream inputStream) {
    List<String> content;
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(inputStream, StandardCharsets.UTF_16))) {
      content = reader.lines().toList();
    } catch (IOException e) {
      throw new ParsingException("Cannot read from input stream", e);
    }
    if (content.isEmpty()) {
      throw new ParsingException("No content provided!");
    }
    var propertyToIndex = new HashMap<String, Integer>();

    var header = content.get(0).split(VALUE_SEPARATOR);
    for (int i = 0; i < header.length; i++) {
      propertyToIndex.put(Sanitizer.headerEncoder(header[i]), i);
    }

    var values = content.subList(1, content.size());
    List<ParsingResult.Row> rows = new ArrayList<>();
    for (String row : values) {
      var rowContent = row.split(VALUE_SEPARATOR);
      String[] rowData = new String[header.length];
      for (Entry<String, Integer> propertyEntry : propertyToIndex.entrySet()) {
        rowData[propertyEntry.getValue()] = safeAccess(rowContent, propertyEntry.getValue()).orElse(
            "");
      }
      if (Sanitizer.containsInformation(rowData)) {
        rows.add(new Row(Arrays.stream(rowData).toList()));
      }
    }
    return new ParsingResult(propertyToIndex, rows);
  }
}
