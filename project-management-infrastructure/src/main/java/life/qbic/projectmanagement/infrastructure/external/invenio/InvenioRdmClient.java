package life.qbic.projectmanagement.infrastructure.external.invenio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.associated_dataset.DatasetAccessFilter;

/**
 * Low-level HTTP client for the Invenio REST API.
 *
 * <p>DTOs model the InvenioRDM v12 record format (OpenAPI 3.1.1,
 * API version 12.0.0), served at {@code GET /api/records} by both
 * Zenodo and FDAT when requesting the content type
 * {@code application/vnd.inveniordm.v1+json}. See
 * <a href="https://inveniosoftware.github.io/invenio-openapi/">inveniosoftware.github.io/invenio-openapi</a>
 * for the specification; the shape was verified against live FDAT and
 * Zenodo responses.</p>
 *
 * <p>Implements the two endpoints required by FEAT-DATSET-01:
 * <ul>
 *   <li>{@code GET /api/records} — {@link #search(String, SearchParams)}
 *       — paginated record search (operationId: {@code searchRecords})</li>
 *   <li>{@code GET /api/records/{recordId}} —
 *       {@link #getRecord(String, String)} — resolve a single record
 *       (operationId: {@code getARecordById})</li>
 * </ul>
 *
 * <p>Handles:
 * <ul>
 *   <li>HTTP communication with {@code java.net.http.HttpClient} (HTTP/2,
 *       shared instance)</li>
 *   <li>Bounded synchronous retry (ADR-0002 §7: 3 attempts, exponential
 *       backoff, 5s ceiling) for transient errors: HTTP 5xx, 429, network
 *       errors, timeouts</li>
 *   <li>Retry-After header support on HTTP 429</li>
 *   <li>JSON deserialization via Jackson</li>
 * </ul>
 *
 * <p>Per ADR-0002 §9, HTTP 401/403/404 are <em>not</em> retried; they
 * indicate permanent access/credential errors that must be reported
 * back to the caller.</p>
 *
 * @since 1.12.0
 */
public interface InvenioRdmClient {

  /**
   * Base exception for all InvenioRDM client failures.
   *
   * <p>All specific exceptions extend this class. Callers may catch the
   * base type to handle all client errors uniformly, or catch specific
   * subclasses to differentiate between permanent failures, transient
   * retry exhaustion, parsing errors, and interruptions.</p>
   */
  abstract class InvenioRdmException extends RuntimeException {
    private final String url;

    protected InvenioRdmException(String message, String url) {
      super(message);
      this.url = url;
    }

    protected InvenioRdmException(String message, Throwable cause, String url) {
      super(message, cause);
      this.url = url;
    }

    /** The request URL that caused this failure. */
    public String getUrl() { return url; }
  }

  /**
   * Thrown when the InvenioRDM server returns a permanent error (4xx) that
   * is not retried, per ADR-0002 §9.
   *
   * <p>This covers HTTP 401, 403, 404, and other 4xx codes. HTTP 429
   * (Too Many Requests) is treated as transient and is retried.</p>
   *
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/401">HTTP 401</a>
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/403">HTTP 403</a>
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/404">HTTP 404</a>
   */
  final class InvenioRdmPermanentException extends InvenioRdmException {
    private final int statusCode;

    InvenioRdmPermanentException(String message, int statusCode, String url) {
      super(message, url);
      this.statusCode = statusCode;
    }

    /** The HTTP status code that caused this failure. */
    public int getStatusCode() { return statusCode; }
  }

  /**
   * Thrown when transient errors (5xx, 429, network failures) exhaust all
   * retry attempts.
   *
   * <p>The client retries up to 3 times with exponential backoff for:
   * <ul>
   *   <li>HTTP 5xx server errors</li>
   *   <li>HTTP 429 Too Many Requests (honours Retry-After header)</li>
   *   <li>Network I/O errors (timeouts, connection refused, etc.)</li>
   * </ul>
   * After exhausting all retries, this exception is thrown.</p>
   */
  final class InvenioRdmTransientException extends InvenioRdmException {
    private final int statusCode;  // -1 for I/O errors, otherwise HTTP status
    private final int attempts;
    private final Throwable lastError;

    InvenioRdmTransientException(String message, int statusCode, int attempts,
        Throwable lastError, String url) {
      super(message, lastError, url);
      this.statusCode = statusCode;
      this.attempts = attempts;
      this.lastError = lastError;
    }

    /** HTTP status code, or -1 for I/O errors. */
    public int getStatusCode() { return statusCode; }

    /** Number of attempts made. */
    public int getAttempts() { return attempts; }

    /** The last error encountered (HTTP exception or IOException). */
    public Throwable getLastError() { return lastError; }
  }

  /**
   * Thrown when the JSON response from the InvenioRDM server cannot be
   * parsed.
   *
   * <p>This may indicate a server response format change or corruption.
   * The response body is included in the exception message for diagnostics.</p>
   */
  final class InvenioRdmResponseParsingException extends InvenioRdmException {
    private final Class<?> targetType;

    InvenioRdmResponseParsingException(String message, Throwable cause, Class<?> targetType, String url) {
      super(message, cause, url);
      this.targetType = targetType;
    }

    /** The expected response type. */
    public Class<?> getTargetType() { return targetType; }
  }

  /**
   * Thrown when the HTTP request is interrupted.
   *
   * <p>This typically occurs if the calling thread is interrupted during
   * the retry sleep period.</p>
   */
  final class InvenioRdmInterruptedException extends InvenioRdmException {
    InvenioRdmInterruptedException(String message, Throwable cause, String url) {
      super(message, cause, url);
    }
  }

  /**
   * Search for records matching the given parameters.
   *
   * @param instanceUrl the base URL of the InvenioRDM instance
   * @param params search parameters (query, page, size)
   * @return search results containing matching records
   * @throws InvenioRdmPermanentException if the server returns a 4xx status code
   *     that is not retried (401, 403, 404, etc.)
   * @throws InvenioRdmTransientException if transient errors exhaust all retries
   *     (5xx, 429, network failures after 3 attempts)
   * @throws InvenioRdmResponseParsingException if the JSON response cannot be parsed
   */
  SearchResultResponse search(String instanceUrl, SearchParams params)
      throws InvenioRdmPermanentException, InvenioRdmTransientException,
             InvenioRdmResponseParsingException;

  /**
   * Search for records with an optional bearer token.
   *
   * <p>Per ADR-0002 D1, the token is carried as a {@code char[]} so that
   * callers can zero it after use; it must never be converted to an
   * immutable {@code String}. The client composes the
   * {@code Authorization: Bearer <token>} header internally, immediately
   * before sending the request.</p>
   *
   * @param instanceUrl the base URL of the InvenioRDM instance
   * @param params      search parameters (query, page, size)
   * @param token       the plaintext bearer token as a {@code char[]},
   *                    or {@code null} for unauthenticated (public)
   *                    access. The client does <em>not</em> zero this
   *                    array.
   * @return search results containing matching records
   * @throws InvenioRdmPermanentException on 4xx
   * @throws InvenioRdmTransientException on transient errors after retries
   */
  SearchResultResponse search(String instanceUrl, SearchParams params,
      char[] token)
      throws InvenioRdmPermanentException, InvenioRdmTransientException;

  /**
   * Retrieve a single record by its ID.
   *
   * @param instanceUrl the base URL of the InvenioRDM instance
   * @param recordId the record identifier
   * @return the record details
   * @throws InvenioRdmPermanentException if the server returns a 4xx status code
   *     that is not retried (401, 403, 404, etc.)
   * @throws InvenioRdmTransientException if transient errors exhaust all retries
   *     (5xx, 429, network failures after 3 attempts)
   * @throws InvenioRdmResponseParsingException if the JSON response cannot be parsed
   */
  RecordResponse getRecord(String instanceUrl, String recordId)
      throws InvenioRdmPermanentException, InvenioRdmTransientException,
             InvenioRdmResponseParsingException;

  /**
   * Retrieve a single record by its ID, with an optional bearer token.
   *
   * <p>Per ADR-0002 D1, the token is carried as a {@code char[]} so that
   * callers can zero it after use; it must never be converted to an
   * immutable {@code String}. The client composes the
   * {@code Authorization: Bearer <token>} header internally, immediately
   * before sending the request.</p>
   *
   * @param instanceUrl the base URL of the InvenioRDM instance
   * @param recordId    the record identifier
   * @param token       the plaintext bearer token as a {@code char[]},
   *                    or {@code null} for unauthenticated (public)
   *                    access. The client does <em>not</em> zero this
   *                    array.
   * @return the record details
   * @throws InvenioRdmPermanentException on 4xx
   * @throws InvenioRdmTransientException on transient errors after retries
   */
  RecordResponse getRecord(String instanceUrl, String recordId,
      char[] token)
      throws InvenioRdmPermanentException, InvenioRdmTransientException;

  /**
   * Retrieves the authenticated user's identity from the InvenioRDM instance.
   *
   * <p>This is the token validation endpoint defined in the InvenioRDM
   * OpenAPI specification (operationId: {@code getAUserById}).</p>
   *
   * <ul>
   *   <li>{@code 200} — token is valid; response contains the user's
   *       identity as a JSON object.</li>
   *   <li>{@code 401} — token is missing, invalid, or expired.</li>
   * </ul>
   *
   * <p>The response body is typed {@code type: object} in the official
   * spec (no named fields guaranteed). The DTO below captures best-effort
   * display values for informational/log purposes only — validation
   * succeeds based purely on the 200 status.</p>
   *
   * <p>Per ADR-0002 D1, the token is carried as a {@code char[]} so that
   * callers can zero it after use; it must never be converted to an
   * immutable {@code String}. The client composes the
   * {@code Authorization: Bearer <token>} header internally, immediately
   * before sending the request.</p>
   *
   * @param instanceUrl the base URL of the InvenioRDM instance
   * @param token       the plaintext bearer token as a {@code char[]}
   * @return authenticated user response
   * @throws InvenioRdmPermanentException on 4xx (401 = invalid token)
   * @throws InvenioRdmTransientException on 5xx or network errors after retries
   * @throws InvenioRdmResponseParsingException if the response cannot be parsed
   */
  AuthenticatedUserResponse getAuthenticatedUser(String instanceUrl, char[] token)
      throws InvenioRdmPermanentException, InvenioRdmTransientException,
             InvenioRdmResponseParsingException;

  /**
   * Creates a sharable access link for a restricted record.
   *
   * <p>Creates a secret link that grants view access to anyone with
   * the link. Used when connecting restricted datasets so project
   * collaborators can access the dataset without their own PAT.</p>
   *
   * <p>Requires the authenticated user to have permission to manage
   * access links on the record (typically the record owner).</p>
   *
   * <p>Per ADR-0002 D1, the token is carried as a {@code char[]} so that
   * callers can zero it after use; it must never be converted to an
   * immutable {@code String}. The client composes the
   * {@code Authorization: Bearer <token>} header internally, immediately
   * before sending the request.</p>
   *
   * @param instanceUrl the base URL of the InvenioRDM instance
   * @param recordId    the record identifier
   * @param token       the plaintext bearer token as a {@code char[]}
   * @return the created access link with token
   * @throws InvenioRdmPermanentException on 4xx (403 = insufficient permissions)
   * @throws InvenioRdmTransientException on 5xx or network errors after retries
   * @throws InvenioRdmResponseParsingException if the response cannot be parsed
   * @since 1.12.0
   */
  AccessLinkResponse createAccessLink(String instanceUrl, String recordId,
      char[] token)
      throws InvenioRdmPermanentException, InvenioRdmTransientException,
             InvenioRdmResponseParsingException;

  /**
   * Revokes (deletes) a previously created shareable access link.
   *
   * <p>Issues {@code DELETE /api/records/{recordId}/access/links/{linkId}}
   * on the source system so that previously shared view access is
   * withdrawn. Used when a connect attempt is rolled back or a connection
   * is removed from the application.</p>
   *
   * <p>Revoking an already-revoked link is treated as success: a 404 for
   * an absent link id does not throw, because the desired end state (no
   * active link) is already reached.</p>
   *
   * @param instanceUrl the base URL of the InvenioRDM instance
   * @param recordId    the record identifier the link belongs to
   * @param linkId      the access link id to delete
   * @param token       the plaintext bearer token as a {@code char[]}
   * @return {@code true} if a link was actually deleted; {@code false} if
   *         the link was already absent (404) and nothing changed
   * @throws InvenioRdmPermanentException on other 4xx (403 = insufficient permissions)
   * @throws InvenioRdmTransientException on 5xx or network errors after retries
   * @since 1.12.0
   */
  boolean revokeAccessLink(String instanceUrl, String recordId, String linkId,
      char[] token)
      throws InvenioRdmPermanentException, InvenioRdmTransientException;

  /**
   * Response from creating an access link.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record AccessLinkResponse(
      @JsonProperty("id") String id,
      @JsonProperty("token") String token
  ) {}

  /**
   * Parameters for a search request. All nullable fields are optional.
   */
  record SearchParams(
      String query,
      int page,
      int size,
      /**
       * Optional access-status filter. {@code null} = no filter (all
       * records). The neutral {@link DatasetAccessFilter} is translated to
       * the InvenioRDM {@code access.status} search facet value by
       * {@link #accessFilterWireValue()}. This keeps the abstract
       * {@link DatasetSource} port free of InvenioRDM vocabulary.
       *
       * @since 1.12.0
       */
      DatasetAccessFilter accessFilter
  ) {

    /**
     * Translates the neutral access filter into the InvenioRDM wire
     * facet value used in {@code q=access.status:<value>}, or {@code null}
     * when no filter is set.
     *
     * @return the wire facet value (e.g. {@code "restricted"},
     *         {@code "open"}), or {@code null} for no filter
     */
    public String accessFilterWireValue() {
      return accessFilter == null ? null : accessFilter.toSourceValue();
    }
    /**
     * Backward-compatible constructor — no access filter.
     */
    public SearchParams(String query, int page, int size) {
      this(query, page, size, null);
    }

    public SearchParams {
      if (page < 1) throw new IllegalArgumentException("page must be >= 1 for InvenioRDM API");
      if (size <= 0) throw new IllegalArgumentException("size must be > 0");
      // Zenodo max page size for anonymous is 25; clamp defensively for all instances
      size = Math.min(size, 25);
    }
  }

  /**
   * Search endpoint response, mirroring InvenioRDM JSON structure.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record SearchResultResponse(Hits hits) {
    /**
     * Paginated search results.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Hits(int total, List<Hit> hits) {
      /**
       * Null-safe access to hit list — defaults to empty list.
       */
      public List<Hit> hits() {
        return hits == null ? List.of() : hits;
      }
    }
  }

  /**
   * Single search hit (v12).
   *
   * <p>In the v12 format ({@code application/vnd.inveniordm.v1+json}),
   * both Zenodo and FDAT return identical shapes for hits. Access, PIDs,
   * versions, and community membership are all top-level fields.</p>
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Hit(
      @JsonProperty("id") String id,
      @JsonProperty("links") HitLinks links,
      @JsonProperty("metadata") HitMetadata metadata,
      @JsonProperty("created") String created,
      @JsonProperty("access") RecordAccess access,
      @JsonProperty("pids") Pids pids,
      @JsonProperty("versions") RecordVersions versions,
      @JsonProperty("parent") Parent parent
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record HitLinks(
      @JsonProperty("self_html") String selfHtml
  ) {}

  /**
   * Persistent identifiers block (v12). DOIs are at
   * {@code pids.doi.identifier}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Pids(
      @JsonProperty("doi") PidEntry doi
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record PidEntry(
      @JsonProperty("identifier") String identifier
  ) {}

  /**
   * Version block (v12). {@code index} is 1-based; 0 indicates
   * the first published version (Zenodo returns index 1 for the first).
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record RecordVersions(
      @JsonProperty("is_latest") boolean isLatest,
      @JsonProperty("index") int index
  ) {}

  /**
   * Parent block (v12). Community membership is at
   * {@code parent.communities.entries}, where each entry carries the
   * community {@code slug} and {@code metadata.title}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Parent(
      @JsonProperty("communities") ParentCommunities communities
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ParentCommunities(
      @JsonProperty("default") String defaultCommunity,
      @JsonProperty("ids") List<String> ids,
      @JsonProperty("entries") List<Community> entries
  ) {}

  /**
   * A single community entry from {@code parent.communities.entries}
   * (v12).
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Community(
      @JsonProperty("id") String id,
      @JsonProperty("slug") String slug,
      @JsonProperty("metadata") CommunityMetadata metadata
  ) {
    /**
     * Best-effort display label: prefers {@code metadata.title},
     * then {@code slug}, then the raw {@code id}.
     */
    @JsonIgnore
    public String displayLabel() {
      if (metadata != null && metadata.title != null && !metadata.title.isBlank()) {
        return metadata.title;
      }
      if (slug != null && !slug.isBlank()) {
        return slug;
      }
      return id;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record CommunityMetadata(
      @JsonProperty("title") String title
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record HitMetadata(
      @JsonProperty("title") String title,
      @JsonProperty("publication_date") String publicationDate,
      @JsonProperty("description") String description,
      @JsonProperty("creators") List<Creator> creators,
      @JsonProperty("resource_type") ResourceType resourceType
  ) {}

  /**
   * Creator (v12). Identity is nested under {@code person_or_org};
   * affiliations are siblings at the creator level.
   *
   * <p>{@link #resolvedName()} and {@link #resolvedAffiliation()} provide
   * null-safe access to display values, extracting from
   * {@code person_or_org.name} and the first affiliation entry.</p>
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Creator(
      @JsonProperty("person_or_org") PersonOrOrg personOrOrg,
      @JsonProperty("affiliations") List<Affiliation> affiliations
  ) {
    /**
     * Null-safe access to the creator's display name.
     * Extracts from {@code person_or_org.name}. Returns null if no person or organization is defined.
     */
    public String resolvedName() {
      return (personOrOrg == null || personOrOrg.name == null
          || personOrOrg.name.isBlank()) ? null : personOrOrg.name;
    }

    /**
     * Null-safe access to the first affiliation's display name.
     */
    public String resolvedAffiliation() {
      if (affiliations == null || affiliations.isEmpty()) return null;
      Affiliation first = affiliations.getFirst();
      return (first == null || first.name == null || first.name.isBlank())
          ? null : first.name;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record PersonOrOrg(
      @JsonProperty("name") String name,
      @JsonProperty("type") String type,
      @JsonProperty("given_name") String givenName,
      @JsonProperty("family_name") String familyName
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Affiliation(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name
  ) {}

  /**
   * Resource type (v12). The title field is a localised object
   * (e.g. {@code {"en": "Dataset"}}). {@link #resolvedTitle()} returns
   * the {@code en} value when present, falling back to the first
   * available language key.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  final class ResourceType {
    @JsonIgnore private String id;
    @JsonIgnore private String resolvedTitle;

    @JsonProperty("id")
    private void setId(String id) {
      this.id = id;
    }

    @JsonProperty("title")
    private void setTitleRaw(JsonNode node) {
      if (node == null || node.isNull()) {
        resolvedTitle = null;
        return;
      }
      if (node.isTextual()) {
        resolvedTitle = node.asText();
        return;
      }
      if (node.isObject()) {
        if (node.has("en") && node.get("en").isTextual()) {
          resolvedTitle = node.get("en").asText();
        } else {
          var it = node.fields();
          if (it.hasNext()) {
            JsonNode val = it.next().getValue();
            resolvedTitle = val.isTextual() ? val.asText() : null;
          } else {
            resolvedTitle = null;
          }
        }
      }
    }

    public String resolvedTitle() {
      return resolvedTitle;
    }
  }

  /**
   * Single record detail response (v12).
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record RecordResponse(
      @JsonProperty("id") String id,
      @JsonProperty("links") HitLinks links,
      @JsonProperty("metadata") RecordMetadata metadata,
      @JsonProperty("created") String created,
      @JsonProperty("updated") String updated,
      @JsonProperty("is_published") boolean isPublished,
      @JsonProperty("access") RecordAccess access,
      @JsonProperty("pids") Pids pids,
      @JsonProperty("versions") RecordVersions versions,
      @JsonProperty("parent") Parent parent
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record RecordMetadata(
      @JsonProperty("title") String title,
      @JsonProperty("publication_date") String publicationDate,
      @JsonProperty("description") String description,
      @JsonProperty("creators") List<Creator> creators,
      @JsonProperty("resource_type") ResourceType resourceType
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record RecordAccess(
      @JsonProperty("record") String invenioRecord,
      @JsonProperty("files") String files,
      @JsonProperty("status") String status
  ) {}

  /**
   * Authenticated user response from the InvenioRDM spec-defined
   * {@code GET /api/users} endpoint (operationId: {@code getAUserById}).
   *
   * <p>The response body is typed {@code type: object} in the official
   * spec (no named fields guaranteed). This DTO captures best-effort
   * display values. Validation succeeds purely on the 200 status;
   * the fields are for informational/log purposes only.</p>
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record AuthenticatedUserResponse(
      @JsonProperty("id") String id,
      @JsonProperty("username") String username,
      @JsonProperty("email") String email
  ) {}

  /**
   * Implementation of the InvenioRDM HTTP client.
   *
   * <p>Thread-safe: the underlying {@link HttpClient} is shared and
   * connection pools are reused. Retries run synchronously on the
   * calling thread (ADR-0002 §7).</p>
   */
  final class InvenioRdmHttpClient implements InvenioRdmClient {

    private static final Logger log = LoggerFactory.logger(InvenioRdmHttpClient.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000L;
    private static final long MAX_BACKOFF_MS = 5000L;
    private static final int HTTP_CONNECT_TIMEOUT_S = 10;
    private static final int HTTP_REQUEST_TIMEOUT_S = 30;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HttpClient httpClient;

    public InvenioRdmHttpClient() {
      this.httpClient = HttpClient.newBuilder()
          .version(Version.HTTP_2)
          .followRedirects(Redirect.NORMAL)
          .connectTimeout(Duration.ofSeconds(HTTP_CONNECT_TIMEOUT_S))
          .build();
    }

    @Override
    public SearchResultResponse search(String instanceUrl, SearchParams params)
        throws InvenioRdmPermanentException, InvenioRdmTransientException,
               InvenioRdmResponseParsingException {
      return search(instanceUrl, params, null);
    }

    @Override
    public SearchResultResponse search(String instanceUrl, SearchParams params,
        char[] token)
        throws InvenioRdmPermanentException, InvenioRdmTransientException {
      Objects.requireNonNull(instanceUrl, "instanceUrl must not be null");
      Objects.requireNonNull(params, "params must not be null");

      String url = buildSearchUrl(instanceUrl, params);
      String body = getWithRetry(url, bearerHeader(token), "search records");
      return parseJson(body, SearchResultResponse.class);
    }

    @Override
    public RecordResponse getRecord(String instanceUrl, String recordId)
        throws InvenioRdmPermanentException, InvenioRdmTransientException,
               InvenioRdmResponseParsingException {
      return getRecord(instanceUrl, recordId, null);
    }

    @Override
    public RecordResponse getRecord(String instanceUrl, String recordId,
        char[] token)
        throws InvenioRdmPermanentException, InvenioRdmTransientException {
      Objects.requireNonNull(instanceUrl, "instanceUrl must not be null");
      Objects.requireNonNull(recordId, "recordId must not be null");

      String url = normalizeBaseUrl(instanceUrl) + "/api/records/"
          + URLEncoder.encode(recordId, StandardCharsets.UTF_8);
      String body = getWithRetry(url, bearerHeader(token), "get record " + recordId);
      return parseJson(body, RecordResponse.class);
    }

    @Override
    public AuthenticatedUserResponse getAuthenticatedUser(
        String instanceUrl, char[] token)
        throws InvenioRdmPermanentException, InvenioRdmTransientException,
               InvenioRdmResponseParsingException {
      Objects.requireNonNull(instanceUrl, "instanceUrl must not be null");
      Objects.requireNonNull(token, "token must not be null");

      String url = normalizeBaseUrl(instanceUrl) + "/api/users";
      String body = getWithRetry(url, bearerHeader(token), "get authenticated user");
      return parseJson(body, AuthenticatedUserResponse.class);
    }

    @Override
    public AccessLinkResponse createAccessLink(String instanceUrl,
        String recordId, char[] token)
        throws InvenioRdmPermanentException, InvenioRdmTransientException,
               InvenioRdmResponseParsingException {
      Objects.requireNonNull(instanceUrl, "instanceUrl must not be null");
      Objects.requireNonNull(recordId, "recordId must not be null");
      Objects.requireNonNull(token, "token must not be null");

      String url = normalizeBaseUrl(instanceUrl) + "/api/records/"
          + URLEncoder.encode(recordId, StandardCharsets.UTF_8)
          + "/access/links";
      String body = postWithRetry(url, bearerHeader(token),
          "{\"permission\": \"view\"}", "create access link");
      return parseJson(body, AccessLinkResponse.class);
    }

    @Override
    public boolean revokeAccessLink(String instanceUrl, String recordId,
        String linkId, char[] token)
        throws InvenioRdmPermanentException, InvenioRdmTransientException {
      Objects.requireNonNull(instanceUrl, "instanceUrl must not be null");
      Objects.requireNonNull(recordId, "recordId must not be null");
      Objects.requireNonNull(linkId, "linkId must not be null");
      Objects.requireNonNull(token, "token must not be null");

      String url = normalizeBaseUrl(instanceUrl) + "/api/records/"
          + URLEncoder.encode(recordId, StandardCharsets.UTF_8)
          + "/access/links/"
          + URLEncoder.encode(linkId, StandardCharsets.UTF_8);
      // 404 (link already absent) is treated as success by deleteWithRetry.
      return deleteWithRetry(url, bearerHeader(token), "revoke access link");
    }

    // ── Internals ───────────────────────────────────────────────────

    private String buildSearchUrl(String instanceUrl, SearchParams params) {
      StringBuilder sb = new StringBuilder(normalizeBaseUrl(instanceUrl))
          .append("/api/records?page=").append(params.page())
          .append("&size=").append(params.size());
      
      // Build the effective query string, combining the user's free-text
      // query with the optional access-status filter.
      //
      // accessFilter + user query  → "access.status:<filter> AND <userQuery>"
      // accessFilter + no query    → "access.status:<filter>"
      // no filter + user query     → "<userQuery>"
      // no filter + no query       → sort=newest (no q param)
      String userQuery = (params.query() != null && !params.query().isBlank())
          ? params.query() : null;
      String filter = params.accessFilterWireValue();

      if (filter != null && userQuery != null) {
        // Combined: filter + user query
        sb.append("&q=").append(URLEncoder.encode(
            "access.status:" + filter + " AND " + userQuery,
            StandardCharsets.UTF_8));
      } else if (filter != null) {
        // Filter only, no user query
        sb.append("&q=").append(URLEncoder.encode(
            "access.status:" + filter, StandardCharsets.UTF_8));
      } else if (userQuery != null) {
        // User query only, no filter — rely on relevance-based sorting
        sb.append("&q=").append(
            URLEncoder.encode(userQuery, StandardCharsets.UTF_8));
      } else {
        // No query, no filter — browse by newest
        sb.append("&sort=newest");
      }
      
      return sb.toString();
    }

    private String normalizeBaseUrl(String url) {
      return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Composes the {@code Authorization: Bearer <token>} header value from
     * a {@code char[]} bearer token without materialising the token as an
     * immutable, interned {@code String} of its own.
     *
     * <p>The literal prefix and the token are copied into a single
     * contiguous {@code char[]} and converted to exactly one short-lived
     * (non-interned) {@code String}; the temporary buffer is zeroed before
     * returning. The caller's token {@code char[]} is <em>not</em>
     * modified.</p>
     *
     * <p>This honours ADR-0002 D1 (decryption boundary): the plaintext
     * token is never kept as a standalone, unresettable {@code String}
     * anywhere in the call path.</p>
     *
     * @param token the plaintext bearer token, or {@code null} for
     *              unauthenticated (public) access
     * @return the full header value (e.g. {@code "Bearer <token>"}), or
     *         {@code null} when {@code token} is {@code null}
     */
    private static String bearerHeader(char[] token) {
      if (token == null) {
        return null;
      }
      char[] prefix = "Bearer ".toCharArray();
      char[] combined = new char[prefix.length + token.length];
      System.arraycopy(prefix, 0, combined, 0, prefix.length);
      System.arraycopy(token, 0, combined, prefix.length, token.length);
      String header = new String(combined);
      Arrays.fill(combined, '\0');
      return header;
    }

    private String getWithRetry(String url, String authHeader, String operationName)
        throws InvenioRdmPermanentException, InvenioRdmTransientException {
      int retryCount = 0;
      long backoffMs = INITIAL_BACKOFF_MS;
      IOException lastIo = null;
      Integer lastStatus = null;

      while (retryCount < MAX_ATTEMPTS) {
        int currentAttempt = retryCount + 1;
        try {
          var requestBuilder = HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Accept", "application/vnd.inveniordm.v1+json")
              .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_S))
              .GET();
          if (authHeader != null) {
            requestBuilder.header("Authorization", authHeader);
          }
          HttpResponse<String> response = httpClient.send(
              requestBuilder.build(), BodyHandlers.ofString());

          int status = response.statusCode();

          // Success
          if (isSuccess(status)) {
            return response.body();
          }

          // Permanent failure: 4xx (except 429) — don't retry
          if (isClientError(status) && !isTooManyRequests(status)) {
            throw new InvenioRdmPermanentException(
                "InvenioRDM request failed (%s) with status %d. URL: %s"
                    .formatted(operationName, status, url),
                status, url);
          }

          // Transient: 5xx or 429 — retry with Retry-After honouring
          lastStatus = status;
          if (currentAttempt < MAX_ATTEMPTS) {
            long waitMs = status == 429
                ? parseRetryAfter(response, backoffMs)
                : backoffMs;
            log.warn("InvenioRDM transient failure (%s), status=%d, retrying in %dms (attempt %d/%d)"
                .formatted(operationName, status, waitMs, currentAttempt, MAX_ATTEMPTS));
            sleep(waitMs);
            backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
            retryCount++;
            continue;
          }

          // Exhausted retries on transient
          throw new InvenioRdmTransientException(
              "InvenioRDM request failed (%s) with status %d after %d attempts. URL: %s"
                  .formatted(operationName, status, MAX_ATTEMPTS, url),
              status, MAX_ATTEMPTS, null, url);

        } catch (IOException e) {
          lastIo = e;
          if (currentAttempt < MAX_ATTEMPTS) {
            log.warn("InvenioRDM I/O error (%s): %s, retrying in %dms (attempt %d/%d)"
                .formatted(operationName, e.getMessage(), backoffMs, currentAttempt, MAX_ATTEMPTS));
            sleep(backoffMs);
            backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
            retryCount++;
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new InvenioRdmInterruptedException(
              "InvenioRDM request interrupted (%s)".formatted(operationName),
              e, url);
        }
      }

      throw new InvenioRdmTransientException(
          "InvenioRDM request failed (%s) after %d attempts. Last status=%s, last error=%s. URL: %s"
              .formatted(operationName, MAX_ATTEMPTS, lastStatus,
                  lastIo == null ? "n/a" : lastIo.getMessage(), url),
          lastStatus != null ? lastStatus : -1, MAX_ATTEMPTS, lastIo, url);
    }

    private String postWithRetry(String url, String authHeader, String body,
        String operationName)
        throws InvenioRdmPermanentException, InvenioRdmTransientException {
      int retryCount = 0;
      long backoffMs = INITIAL_BACKOFF_MS;
      IOException lastIo = null;
      Integer lastStatus = null;

      while (retryCount < MAX_ATTEMPTS) {
        int currentAttempt = retryCount + 1;
        try {
          var requestBuilder = HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Accept", "application/vnd.inveniordm.v1+json")
              .header("Content-Type", "application/json")
              .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_S))
              .POST(HttpRequest.BodyPublishers.ofString(body));
          if (authHeader != null) {
            requestBuilder.header("Authorization", authHeader);
          }
          HttpResponse<String> response = httpClient.send(
              requestBuilder.build(), BodyHandlers.ofString());

          int status = response.statusCode();

          // Success
          if (isSuccess(status)) {
            return response.body();
          }

          // Permanent failure: 4xx (except 429) — don't retry
          if (isClientError(status) && !isTooManyRequests(status)) {
            throw new InvenioRdmPermanentException(
                "InvenioRDM request failed (%s) with status %d. URL: %s"
                    .formatted(operationName, status, url),
                status, url);
          }

          // Transient: 5xx or 429 — retry with Retry-After honouring
          lastStatus = status;
          if (currentAttempt < MAX_ATTEMPTS) {
            long waitMs = status == 429
                ? parseRetryAfter(response, backoffMs)
                : backoffMs;
            log.warn("InvenioRDM transient failure (%s), status=%d, retrying in %dms (attempt %d/%d)"
                .formatted(operationName, status, waitMs, currentAttempt, MAX_ATTEMPTS));
            sleep(waitMs);
            backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
            retryCount++;
            continue;
          }

          // Exhausted retries on transient
          throw new InvenioRdmTransientException(
              "InvenioRDM request failed (%s) with status %d after %d attempts. URL: %s"
                  .formatted(operationName, status, MAX_ATTEMPTS, url),
              status, MAX_ATTEMPTS, null, url);

        } catch (IOException e) {
          lastIo = e;
          if (currentAttempt < MAX_ATTEMPTS) {
            log.warn("InvenioRDM I/O error (%s): %s, retrying in %dms (attempt %d/%d)"
                .formatted(operationName, e.getMessage(), backoffMs, currentAttempt, MAX_ATTEMPTS));
            sleep(backoffMs);
            backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
            retryCount++;
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new InvenioRdmInterruptedException(
              "InvenioRDM request interrupted (%s)".formatted(operationName),
              e, url);
        }
      }

      throw new InvenioRdmTransientException(
          "InvenioRDM request failed (%s) after %d attempts. Last status=%s, last error=%s. URL: %s"
              .formatted(operationName, MAX_ATTEMPTS, lastStatus,
                  lastIo == null ? "n/a" : lastIo.getMessage(), url),
          lastStatus != null ? lastStatus : -1, MAX_ATTEMPTS, lastIo, url);
    }

    /**
     * Issues a {@code DELETE} request with the same retry semantics as
     * {@link #postWithRetry}.
     *
     * <p>A 2xx success returns {@code true}. A 404 Not Found — the target
     * resource is already absent, e.g. an access link that was already
     * revoked — is treated as success and returns {@code false} (nothing
     * was deleted because nothing existed). Other 4xx (except 429) are
     * permanent failures; 5xx and 429 are retried.</p>
     *
     * @param url           the full request URL
     * @param authHeader    the Authorization header value or {@code null}
     * @param operationName a human-readable label for logging
     * @return {@code true} if the resource was deleted; {@code false} if
     *         it was already absent (404)
     * @throws InvenioRdmPermanentException on non-404 client errors (except 429)
     * @throws InvenioRdmTransientException on 5xx or network errors after retries
     */
    private boolean deleteWithRetry(String url, String authHeader,
        String operationName)
        throws InvenioRdmPermanentException, InvenioRdmTransientException {
      int retryCount = 0;
      long backoffMs = INITIAL_BACKOFF_MS;
      IOException lastIo = null;
      Integer lastStatus = null;

      while (retryCount < MAX_ATTEMPTS) {
        int currentAttempt = retryCount + 1;
        try {
          var requestBuilder = HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Accept", "application/vnd.inveniordm.v1+json")
              .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_S))
              .DELETE();
          if (authHeader != null) {
            requestBuilder.header("Authorization", authHeader);
          }
          HttpResponse<String> response = httpClient.send(
              requestBuilder.build(), BodyHandlers.ofString());

          int status = response.statusCode();

          // Success
          if (isSuccess(status)) {
            return true;
          }

          // Already absent — treat as success, nothing was deleted
          if (status == 404) {
            log.info("%s: resource already absent (404), nothing to delete"
                .formatted(operationName));
            return false;
          }

          // Permanent failure: 4xx (except 429) — don't retry
          if (isClientError(status) && !isTooManyRequests(status)) {
            throw new InvenioRdmPermanentException(
                "InvenioRDM request failed (%s) with status %d. URL: %s"
                    .formatted(operationName, status, url),
                status, url);
          }

          // Transient: 5xx or 429 — retry with Retry-After honouring
          lastStatus = status;
          if (currentAttempt < MAX_ATTEMPTS) {
            long waitMs = status == 429
                ? parseRetryAfter(response, backoffMs)
                : backoffMs;
            log.warn("InvenioRDM transient failure (%s), status=%d, retrying in %dms (attempt %d/%d)"
                .formatted(operationName, status, waitMs, currentAttempt, MAX_ATTEMPTS));
            sleep(waitMs);
            backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
            retryCount++;
            continue;
          }

          // Exhausted retries on transient
          throw new InvenioRdmTransientException(
              "InvenioRDM request failed (%s) with status %d after %d attempts. URL: %s"
                  .formatted(operationName, status, MAX_ATTEMPTS, url),
              status, MAX_ATTEMPTS, null, url);

        } catch (IOException e) {
          lastIo = e;
          if (currentAttempt < MAX_ATTEMPTS) {
            log.warn("InvenioRDM I/O error (%s): %s, retrying in %dms (attempt %d/%d)"
                .formatted(operationName, e.getMessage(), backoffMs, currentAttempt, MAX_ATTEMPTS));
            sleep(backoffMs);
            backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
            retryCount++;
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new InvenioRdmInterruptedException(
              "InvenioRDM request interrupted (%s)".formatted(operationName),
              e, url);
        }
      }

      throw new InvenioRdmTransientException(
          "InvenioRDM request failed (%s) after %d attempts. Last status=%s, last error=%s. URL: %s"
              .formatted(operationName, MAX_ATTEMPTS, lastStatus,
                  lastIo == null ? "n/a" : lastIo.getMessage(), url),
          lastStatus != null ? lastStatus : -1, MAX_ATTEMPTS, lastIo, url);
    }

    /**
     * Returns true if the status code is a success (2xx).
     * @param status the status code to check
     * @return true if the status code is a success (2xx)
     */
    private static boolean isSuccess(int status) {
      return status >= 200 && status < 300;
    }

    /**
     * Returns true if the status code is a client error (4xx).
     * @param status the status code to check
     * @return true if the status code is a client error (4xx)
     */
    private static boolean isClientError(int status) {
      return status >= 400 && status < 500;
    }

    /**
     * Returns true if the status code is 429 Too Many Requests.
     * @param status the status code to check
     * @return true if the status code is 429 Too Many Requests
     */
    private static boolean isTooManyRequests(int status) {
      return status == 429;
    }

    private long parseRetryAfter(HttpResponse<String> response, long fallbackMs) {
      String header = response.headers().firstValue("Retry-After").orElse(null);
      if (header == null) return fallbackMs;
      try {
        long seconds = Long.parseLong(header);
        return Math.min(seconds * 1000L, MAX_BACKOFF_MS);
      } catch (NumberFormatException e) {
        return fallbackMs;
      }
    }

    private void sleep(long ms) {
      try {
        Thread.sleep(ms);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    private <T> T parseJson(String body, Class<T> type) {
      try {
        return OBJECT_MAPPER.readValue(body, type);
      } catch (IOException e) {
        throw new InvenioRdmResponseParsingException(
            "Failed to parse InvenioRDM JSON response as " + type.getSimpleName(),
            e, type, null);
      }
    }
  }
}
