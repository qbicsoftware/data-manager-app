package life.qbic.projectmanagement.domain.model.measurement;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record NGSIndex(String indexI5, String indexI7) {

  public static NGSIndex doubleIndexing(String indexI5, String indexI7) {
    return new NGSIndex(indexI5, indexI7);
  }

  public static NGSIndex singleIndexing(String indexI7) {
    return new NGSIndex("", indexI7);
  }
}
