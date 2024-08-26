package life.qbic.datamanager.parser.xlsx;

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
import life.qbic.datamanager.parser.MetadataParser;
import life.qbic.datamanager.parser.ParsingResult;

/**
 * <b>TSV Parser</b>
 * <p>
 * Tab-seperated value format support for the {@link MetadataParser} interface.
 *
 * Support for UTF-16 encoding available.
 *
 * @since 1.4.0
 */
public class TSVParser implements MetadataParser {

  private static final String VALUE_SEPARATOR = "\t";

  private boolean firstRowIsHeader = false;

  private boolean headerToLowerCase = false;

  private TSVParser() {

  }

  private TSVParser(boolean firstRowIsHeader, boolean headerToLowerCase) {
    this.firstRowIsHeader = firstRowIsHeader;
    this.headerToLowerCase = headerToLowerCase;
  }

  public static TSVParser createWithHeaderToLowerCase(boolean firstRowIsHeader) {
    return new TSVParser(firstRowIsHeader, true);
  }

  @Override
  public ParsingResult parse(InputStream inputStream) {
    var content = new ArrayList<String>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,  StandardCharsets.UTF_16))) {
      content = (ArrayList<String>) reader.lines().toList();
    } catch (IOException e) {
      throw new ParsingException("Cannot read from input stream", e);
    }
    if (content.isEmpty()) {
      throw new ParsingException("No content provided!");
    }
    var keyMap = new HashMap<String, Integer>();

    var header = content.get(0).split(VALUE_SEPARATOR);
    for (int i = 0; i < header.length; i++) {
      keyMap.put(header[i], i);
    }

    var values = firstRowIsHeader ? content.subList(1, content.size()) : content;
    var iterator = values.iterator();
    List<List<String>> rows = new ArrayList<>();
    while (iterator.hasNext()) {
      var row = iterator.next().split(VALUE_SEPARATOR);
      String[] rowData = new String[header.length];
      for (Entry<String, Integer> key : keyMap.entrySet()) {
        rowData[key.getValue()] = row[key.getValue()];
      }
      rows.add(Arrays.stream(rowData).toList());
    }
    return new ParsingResult(keyMap, rows);
  }
}
