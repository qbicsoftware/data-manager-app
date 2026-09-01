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
    int pageSize,

    /**
     * Optional access-status filter for the external source.
     * {@code null} means no filter (return all records).
     * {@link DatasetAccessFilter#RESTRICTED} returns only
     * access-restricted records; {@link DatasetAccessFilter#PUBLIC}
     * returns only publicly accessible records.
     *
     * <p>Source-neutral: the port must not know how any particular source
     * encodes this; translation to a source-specific wire value happens in
     * the source adapter.</p>
     *
     * @since 1.12.0
     */
    DatasetAccessFilter accessFilter
) {

  /**
   * Backward-compatible constructor — no access filter (returns all records).
   */
  public SearchQuery(String query, int page, int pageSize) {
    this(query, page, pageSize, null);
  }

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
   * Convenience factory for a "list all" query (blank search term,
   * no access filter).
   */
  public static SearchQuery listAll(int page, int pageSize) {
    return new SearchQuery("", page, pageSize, null);
  }

  /**
   * Convenience factory for a "list all" query filtered by access
   * status.
   *
   * @param page         zero-indexed page number
   * @param pageSize     results per page
   * @param accessFilter the access-status filter, or {@code null} for none
   */
  public static SearchQuery listAll(int page, int pageSize,
      DatasetAccessFilter accessFilter) {
    return new SearchQuery("", page, pageSize, accessFilter);
  }
}
