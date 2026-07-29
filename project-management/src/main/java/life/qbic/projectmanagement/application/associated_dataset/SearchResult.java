package life.qbic.projectmanagement.application.associated_dataset;

import java.util.List;
import java.util.Objects;

/**
 * A paginated search result from an external data source.
 *
 * <p>Wraps the list of {@link SearchHit}s returned by one page of a
 * search, along with total count and pagination metadata.</p>
 *
 * @since 1.12.0
 */
public record SearchResult(
    /** The hits on this page. */
    List<SearchHit> hits,

    /** Total number of matching records across all pages. */
    int totalHits,

    /** Zero-indexed page number. */
    int page,

    /** Number of results per page. */
    int pageSize
) {

  public SearchResult {
    Objects.requireNonNull(hits, "hits must not be null");
    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be positive");
    }
    hits = List.copyOf(hits);
  }

  /**
   * Whether there are more pages after this one.
   */
  public boolean hasMorePages() {
    return (page + 1) * pageSize < totalHits;
  }
}
