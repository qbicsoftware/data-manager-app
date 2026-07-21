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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

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

  SearchResultResponse search(String instanceUrl, SearchParams params);
  RecordResponse getRecord(String instanceUrl, String recordId);

  /**
   * Parameters for a search request. All nullable fields are optional.
   */
  record SearchParams(
      String query,
      int page,
      int size
  ) {
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
  final class SearchResultResponse {
    @JsonProperty("hits") public Hits hits;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hits {
      @JsonProperty("total") public int total;
      @JsonProperty("hits") public List<Hit> hits = List.of();
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
  final class Hit {
    @JsonProperty("id") public String id;
    @JsonProperty("links") public HitLinks links;
    @JsonProperty("metadata") public HitMetadata metadata;
    @JsonProperty("created") public String created;
    @JsonProperty("access") public RecordAccess access;
    @JsonProperty("pids") public Pids pids;
    @JsonProperty("versions") public RecordVersions versions;
    @JsonProperty("parent") public Parent parent;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class HitLinks {
    @JsonProperty("self_html") public String selfHtml;
  }

  /**
   * Persistent identifiers block (v12). DOIs are at
   * {@code pids.doi.identifier}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  final class Pids {
    @JsonProperty("doi") public PidEntry doi;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class PidEntry {
    @JsonProperty("identifier") public String identifier;
  }

  /**
   * Version block (v12). {@code versions.index} is 1-based; 0 indicates
   * the first published version (Zenodo returns index 1 for the first).
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  final class RecordVersions {
    @JsonProperty("is_latest") public boolean isLatest;
    @JsonProperty("index") public int index;
  }

  /**
   * Parent block (v12). Community membership is at
   * {@code parent.communities.entries}, where each entry carries the
   * community {@code slug} and {@code metadata.title}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  final class Parent {
    @JsonProperty("communities") public ParentCommunities communities;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class ParentCommunities {
    @JsonProperty("default") public String defaultCommunity;
    @JsonProperty("ids") public List<String> ids;
    @JsonProperty("entries") public List<Community> entries;
  }

  /**
   * A single community entry from {@code parent.communities.entries}
   * (v12).
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  final class Community {
    @JsonProperty("id") public String id;
    @JsonProperty("slug") public String slug;
    @JsonProperty("metadata") public CommunityMetadata metadata;

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
  final class CommunityMetadata {
    @JsonProperty("title") public String title;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class HitMetadata {
    @JsonProperty("title") public String title;
    @JsonProperty("publication_date") public String publicationDate;
    @JsonProperty("description") public String description;
    @JsonProperty("creators") public List<Creator> creators;
    @JsonProperty("resource_type") public ResourceType resourceType;
  }

  /**
   * Creator (v12). Identity is nested under {@code person_or_org};
   * affiliations are siblings at the creator level.
   *
   * <p>{@link #resolvedName()} and {@link #resolvedAffiliation()} provide
   * null-safe access to display values, extracting from
   * {@code person_or_org.name} and the first affiliation entry.</p>
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  final class Creator {
    @JsonIgnore public PersonOrOrg personOrOrg;
    @JsonIgnore public List<Affiliation> affiliations;

    @JsonProperty("person_or_org")
    private void setPersonOrOrg(PersonOrOrg po) { this.personOrOrg = po; }

    @JsonProperty("affiliations")
    private void setAffiliations(List<Affiliation> af) { this.affiliations = af; }

    public String resolvedName() {
      return (personOrOrg == null || personOrOrg.name == null
          || personOrOrg.name.isBlank()) ? null : personOrOrg.name;
    }

    public String resolvedAffiliation() {
      if (affiliations == null || affiliations.isEmpty()) return null;
      Affiliation first = affiliations.getFirst();
      return (first == null || first.name == null || first.name.isBlank())
          ? null : first.name;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class PersonOrOrg {
    @JsonProperty("name") public String name;
    @JsonProperty("type") public String type;
    @JsonProperty("given_name") public String givenName;
    @JsonProperty("family_name") public String familyName;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class Affiliation {
    @JsonProperty("id") public String id;
    @JsonProperty("name") public String name;
  }

  /**
   * Resource type (v12). The title field is a localised object
   * (e.g. {@code {"en": "Dataset"}}). {@link #resolvedTitle()} returns
   * the {@code en} value when present, falling back to the first
   * available language key.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  final class ResourceType {
    @JsonProperty("id") public String id;
    @JsonIgnore private String resolvedTitle;

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
  final class RecordResponse {
    @JsonProperty("id") public String id;
    @JsonProperty("links") public HitLinks links;
    @JsonProperty("metadata") public RecordMetadata metadata;
    @JsonProperty("created") public String created;
    @JsonProperty("updated") public String updated;
    @JsonProperty("is_published") public boolean isPublished;
    @JsonProperty("access") public RecordAccess access;
    @JsonProperty("pids") public Pids pids;
    @JsonProperty("versions") public RecordVersions versions;
    @JsonProperty("parent") public Parent parent;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class RecordMetadata {
    @JsonProperty("title") public String title;
    @JsonProperty("publication_date") public String publicationDate;
    @JsonProperty("description") public String description;
    @JsonProperty("creators") public List<Creator> creators;
    @JsonProperty("resource_type") public ResourceType resourceType;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class RecordAccess {
    @JsonProperty("record") public String record;
    @JsonProperty("files") public String files;
    @JsonProperty("status") public String status;
    @JsonProperty("embargo") public Embargo embargo;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class Embargo {
    @JsonProperty("active") public boolean active;
    @JsonProperty("until") public String until;
  }

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
    public SearchResultResponse search(String instanceUrl, SearchParams params) {
      Objects.requireNonNull(instanceUrl, "instanceUrl must not be null");
      Objects.requireNonNull(params, "params must not be null");

      String url = buildSearchUrl(instanceUrl, params);
      String body = getWithRetry(url, null, "search records");
      return parseJson(body, SearchResultResponse.class);
    }

    @Override
    public RecordResponse getRecord(String instanceUrl, String recordId) {
      Objects.requireNonNull(instanceUrl, "instanceUrl must not be null");
      Objects.requireNonNull(recordId, "recordId must not be null");

      String url = normalizeBaseUrl(instanceUrl) + "/api/records/"
          + URLEncoder.encode(recordId, StandardCharsets.UTF_8);
      String body = getWithRetry(url, null, "get record " + recordId);
      return parseJson(body, RecordResponse.class);
    }

    // ── Internals ───────────────────────────────────────────────────

    private String buildSearchUrl(String instanceUrl, SearchParams params) {
      StringBuilder sb = new StringBuilder(normalizeBaseUrl(instanceUrl))
          .append("/api/records?page=").append(params.page())
          .append("&size=").append(params.size());
      
      // When there's a search query, rely on the API's default relevance-based sorting.
      // When listing records without a query (browsing), sort by newest first.
      if (params.query() != null && !params.query().isBlank()) {
        sb.append("&q=").append(
            URLEncoder.encode(params.query(), StandardCharsets.UTF_8));
      } else {
        sb.append("&sort=newest");
      }
      
      return sb.toString();
    }

    private String normalizeBaseUrl(String url) {
      return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String getWithRetry(String url, String authHeader, String operationName) {
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
          if (status >= 400 && status < 500 && status != 429) {
            throw new ApplicationException(
                "InvenioRDM request failed (%s) with status %d. URL: %s"
                    .formatted(operationName, status, url));
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
          throw new ApplicationException(
              "InvenioRDM request failed (%s) with status %d after %d attempts. URL: %s"
                  .formatted(operationName, status, MAX_ATTEMPTS, url));

        } catch (IOException e) {
          lastIo = e;
          if (currentAttempt < MAX_ATTEMPTS) {
            log.warn("InvenioRDM I/O error (%s): %s, retrying in %dms (attempt %d/%d)"
                .formatted(operationName, e.getMessage(), backoffMs, currentAttempt, MAX_ATTEMPTS));
            sleep(backoffMs);
            backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
            retryCount++;
            continue;
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new ApplicationException(
              "InvenioRDM request interrupted (%s)".formatted(operationName), e);
        }
      }

      throw new ApplicationException(
          "InvenioRDM request failed (%s) after %d attempts. Last status=%s, last error=%s. URL: %s"
              .formatted(operationName, MAX_ATTEMPTS, lastStatus,
                  lastIo == null ? "n/a" : lastIo.getMessage(), url),
          lastIo);
    }

    private static boolean isSuccess(int status) {
      return status >= 200 && status < 300;
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
        throw new ApplicationException(
            "Failed to parse InvenioRDM JSON response as " + type.getSimpleName(), e);
      }
    }
  }
}
