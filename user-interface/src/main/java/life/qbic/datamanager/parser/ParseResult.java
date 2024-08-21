package life.qbic.datamanager.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record ParseResult(Map<String, Integer> keys, List<List<String>> values) {

  public Stream<List<String>> rows() {
    return values.stream();
  }

}
