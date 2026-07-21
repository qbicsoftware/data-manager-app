package life.qbic.projectmanagement.application.associated_dataset;

/**
 * The query parameters for searching an external data source.
 *
 * <p>An empty or blank {@code query} string means "list all available
 * records" (with pagination applied). Pagination is zero-indexed for
 * page number.</p>
 *
 * @since 1.12.0
 */
public record SearchQuery(
    /**
     * Free-text search term (title, PID, creator, etc.), or empty/null
     * for "list all" with pagination.
     */
    String query,

    /** Zero-indexed page number. */
    int page,

    /** Number of results per page. */
    int pageSize
) {

  public SearchQuery {
    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative, got: " + page);
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be positive, got: " + pageSize);
    }
  }

  /**
   * Returns the effective search term: empty string if null/blank,
   * meaning "list all".
   */
  public String effectiveQuery() {
    return query == null || query.isBlank() ? "" : query;
  }

  /**
   * Convenience factory for a "list all" query (blank search term).
   */
  public static SearchQuery listAll(int page, int pageSize) {
    return new SearchQuery("", page, pageSize);
  }
}
