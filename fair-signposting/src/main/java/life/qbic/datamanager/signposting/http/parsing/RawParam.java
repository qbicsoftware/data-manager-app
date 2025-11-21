package life.qbic.datamanager.signposting.http.parsing;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record RawParam(String name, String value) {

  public static RawParam emptyParameter(String name) {
    return new RawParam(name, "");
  }

  public static RawParam withValue(String name, String value) {
    return new RawParam(name, value);
  }

}
