package life.qbic.projectmanagement.infrastructure.external.invenio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
 * <p>Implements the endpoints and DTO shapes described in the InvenioRDM
 * OpenAPI specification:
 * <a href="https://inveniosoftware.github.io/invenio-openapi/">inveniosoftware.github.io/invenio-openapi</a>
 * (OpenAPI 3.1.1, API version 12.0.0). The specification is the source
 * of truth for all request/response structures used by this client.</p>
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
   * Single search hit, mirroring InvenioRDM search JSON structure.
   * Access is extracted from {@code metadata.access_right} (Zenodo-specific).
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  final class Hit {
    @JsonProperty("id") public String id;
    @JsonProperty("doi") public String doi;
    @JsonProperty("title") public String title;
    @JsonProperty("links") public HitLinks links;
    @JsonProperty("metadata") public HitMetadata metadata;
    @JsonProperty("created") public String created;
    @JsonProperty("revision") public int revision;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class HitLinks {
    @JsonProperty("self_html") public String selfHtml;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class HitMetadata {
    @JsonProperty("title") public String title;
    @JsonProperty("doi") public String doi;
    @JsonProperty("publication_date") public String publicationDate;
    @JsonProperty("description") public String description;
    @JsonProperty("creators") public List<Creator> creators;
    @JsonProperty("resource_type") public ResourceType resourceType;
    @JsonProperty("access_right") public String accessRight;
    @JsonProperty("relations") public Relations relations;
    @JsonProperty("version") public String version;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class Creator {
    @JsonProperty("name") public String name;
    @JsonProperty("affiliation") public String affiliation;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class ResourceType {
    @JsonProperty("type") public String type;
    @JsonProperty("title") public String title;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class Relations {
    @JsonProperty("version") public List<VersionRelation> version;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class VersionRelation {
    @JsonProperty("index") public int index;
    @JsonProperty("is_last") public boolean isLast;
    @JsonProperty("parent") public ParentRelation parent;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class ParentRelation {
    @JsonProperty("pid_type") public String pidType;
    @JsonProperty("pid_value") public String pidValue;
  }

  /**
   * {@code links.versions} in the InvenioRDM response is a URL string
   * pointing to the REST versions endpoint — it is not a structured
   * block. Do not attempt to map it as {@code @JsonProperty("versions")}
   * on {@link Hit} or {@link RecordResponse}. The authoritative version
   * metadata lives in {@link Relations#version}.
   */

  /**
   * Single record detail response, mirroring InvenioRDM record JSON structure.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  final class RecordResponse {
    @JsonProperty("id") public String id;
    @JsonProperty("doi") public String doi;
    @JsonProperty("links") public HitLinks links;
    @JsonProperty("metadata") public RecordMetadata metadata;
    @JsonProperty("created") public String created;
    @JsonProperty("revision") public int revision;
    @JsonProperty("is_published") public boolean isPublished;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class RecordMetadata {
    @JsonProperty("title") public String title;
    @JsonProperty("doi") public String doi;
    @JsonProperty("publication_date") public String publicationDate;
    @JsonProperty("description") public String description;
    @JsonProperty("creators") public List<Creator> creators;
    @JsonProperty("resource_type") public ResourceType resourceType;
    @JsonProperty("access_right") public String accessRight;
    @JsonProperty("relations") public Relations relations;
    @JsonProperty("version") public String version;
    @JsonProperty("access") public RecordAccess access;
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
              .header("Accept", "application/json")
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
