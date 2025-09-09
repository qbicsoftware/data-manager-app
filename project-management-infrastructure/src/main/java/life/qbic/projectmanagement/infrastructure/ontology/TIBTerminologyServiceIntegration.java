package life.qbic.projectmanagement.infrastructure.ontology;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ontology.LookupException;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.application.ontology.TerminologySelect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * <b>TIB Terminology Service</b>
 * <p>
 * Integrates the TIB Terminology Service API Endpoint to support rich ontology terms.
 *
 * @since 1.4.0
 */
@Service
@Profile("production")
public class TIBTerminologyServiceIntegration implements TerminologySelect {

  private static final Logger log = logger(TIBTerminologyServiceIntegration.class);
  private static final int TIMEOUT_5_SECONDS = 5;
  private static final HttpClient HTTP_CLIENT = httpClient(TIMEOUT_5_SECONDS);

  private static final List<String> ONTOLOGIES_WHITELIST = List.of(
      "bao", // Bio-assay Ontology
      "bto", // Brenda Tissue Ontology
      "chebi", // Chemical Entities of Biological Interest
      "edam", // Bioinformatics operations, data types, formats, identifiers and topics
      "efo", // Experimental Factor Ontology
      "envo", // Environmental Factor Ontology#
      "go", // Gene Ontology
      "mi", // Molecular Interaction
      "ms",  // PSI Mass Spectrometry Ontology
      "ncit", // National Cancer Institute Thesaurus
      "po" // Plant Ontology
  );

  private final URI selectEndpointAbsoluteUrl;
  private final URI searchEndpointAbsoluteUrl;
  private final RequestCache cache;

  @Autowired
  public TIBTerminologyServiceIntegration(
      @Value("${terminology.service.tib.endpoint.select}") String selectEndpoint,
      @Value("${terminology.service.tib.endpoint.search}") String searchEndpoint,
      @Value("${terminology.service.tib.api.url}") String tibApiUrl) {
    this.selectEndpointAbsoluteUrl = URI.create(tibApiUrl).resolve(selectEndpoint);
    this.searchEndpointAbsoluteUrl = URI.create(tibApiUrl).resolve(searchEndpoint);
    this.cache = new RequestCache(1000);
  }

  /**
   * Converts a {@link TibTerm} to a {@link OntologyClass}.
   * <p>
   * DISCLAIMER: the TIB terms do not contain ontology version and ontology iri in the result
   * objects. So the ontology class object will not contain this information.
   *
   * @param term the term to convert
   * @return the converted term as ontology class, missing ontology version and ontology IRI
   * @since 1.4.0
   */
  private static OntologyClass convert(TibTerm term) {
    return new OntologyClass(term.ontologyName, "", "", term.label, term.shortForm,
        term.getDescription().orElse(""), term.iri);
  }

  /**
   * Creates a comma-separated list of all white-listed ontologies to be used in the API as query
   * parameters.
   *
   * @return a concatenated String of whitelisted ontologies
   * @since 1.4.0
   */
  private static String createOntologyFilterQueryParameter() {
    return String.join(",", ONTOLOGIES_WHITELIST);
  }

  private static HttpClient httpClient(int timeoutSeconds) {
    return HttpClient.newBuilder().version(Version.HTTP_2)
        .followRedirects(Redirect.NORMAL).connectTimeout(
            Duration.ofSeconds(timeoutSeconds)).build();
  }

  /**
   * Wraps a general exception with a custom message as a {@link LookupException} to comply with the
   * interface requirements.
   *
   * @param message a custom message about what has happened.
   * @param e       the exception to wrap
   * @return a lookup exception
   * @since 1.4.0
   */
  private static LookupException wrap(String message, Exception e) {
    return new LookupException(message, e);
  }

  /**
   * Wraps an {@link IOException} with a default message for IO-related exceptions.
   *
   * @param e the exception
   * @return a lookup exception
   * @since 1.4.0
   */
  private static LookupException wrapIO(IOException e) {
    return wrap("Terminology service search failed. Service might not be reachable", e);
  }

  /**
   * Wraps an {@link InterruptedException} with a default message for interrupted-related
   * exceptions.
   *
   * @param e the exception
   * @return a lookup exception
   * @since 1.4.0
   */
  private static LookupException wrapInterrupted(InterruptedException e) {
    return wrap("Terminology service search failed. Process was interrupted", e);
  }

  /**
   * Wraps an {@link Exception} with a default message for unknown exceptions.
   *
   * @param e the exception
   * @return a lookup exception
   * @since 1.4.0
   */
  private static LookupException wrapUnknown(Exception e) {
    return new LookupException("Unknown exception during terminology search", e);
  }

  /**
   * Wraps an {@link JsonProcessingException} with a default message for JSON processing-related
   * exceptions.
   *
   * @param e the exception
   * @return a lookup exception
   * @since 1.4.0
   */
  private static LookupException wrapProcessingException(JsonProcessingException e) {
    return new LookupException("Terminology Term Failure: Cannot process response.", e);
  }

  @Override
  public List<OntologyClass> query(String searchTerm, int offset, int limit)
      throws LookupException {
    try {
      List<TibTerm> result = select(searchTerm, offset, limit);
      return result.stream().map(TIBTerminologyServiceIntegration::convert).toList();
    } catch (IOException e) {
      throw wrapIO(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw wrapInterrupted(e);
    } catch (Exception e) {
      throw wrapUnknown(e);
    }
  }

  @Override
  public Optional<OntologyClass> searchByCurie(String curie) throws LookupException {
    try {
      return Optional.of(convert(cache.findByCurie(curie)
          .orElseThrow(() -> new LookupException("Term for curie '%s' not found in cache.".formatted(curie)))));
    } catch (LookupException e) {
      log.debug("Error searching by CURIE: " + curie, e);
      try {
        return searchByOboIdExact(curie)
            .map(this::updateCache)
            .map(TIBTerminologyServiceIntegration::convert);
      } catch (IOException ex) {
        throw wrapIO(ex);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw wrapInterrupted(ex);
      }
    }
  }

  @Override
  public List<OntologyClass> search(String searchTerm, int offset, int limit)
      throws LookupException {
    try {
      List<TibTerm> result = fullSearch(searchTerm, offset, limit);
      return result.stream().map(TIBTerminologyServiceIntegration::convert).toList();
    } catch (IOException e) {
      throw wrapIO(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw wrapInterrupted(e);
    } catch (Exception e) {
      throw wrapUnknown(e);
    }
  }

  /**
   * Queries the /search endpoint of the TIB terminology service. This endpoint provides the
   * ontology term `description` property, which the /select endpoint does not.
   *
   * @param searchTerm the search term
   * @param offset     the offset of results to query the result
   * @param limit      the max number of results to return per page
   * @return a list of matching terms.
   * @throws IOException          if e.g. the service cannot be reached
   * @throws InterruptedException the query is interrupted before succeeding
   * @since 1.4.0
   */
  private List<TibTerm> fullSearch(String searchTerm, int offset, int limit)
      throws IOException, InterruptedException {
    if (searchTerm.isBlank()) { // avoid unnecessary API calls
      return List.of();
    }
    HttpRequest termSelectQuery = HttpRequest.newBuilder().uri(URI.create(
            searchEndpointAbsoluteUrl.toString() + "?q="
                + URLEncoder.encode(
                searchTerm,
                StandardCharsets.UTF_8)
                + "&rows=" + limit + "&start=" + offset
                + "&ontology=" + createOntologyFilterQueryParameter()))
        .header("Content-Type", "application/json").GET().build();
    var response = HTTP_CLIENT.send(termSelectQuery, BodyHandlers.ofString());
    return parseResponse(response).stream().toList();
  }

  /**
   * Queries the /select endpoint of the TIB terminology service, which is optimized for the
   * auto-complete use case. This endpoint DOES NOT provide the ontology term `description`
   * property.
   * <p>
   * Use {@link #fullSearch(String, int, int)} instead.
   *
   * @param searchTerm the search term
   * @param offset     the offset of results to query the result
   * @param limit      the max number of results to return per page
   * @return a list of matching terms.
   * @throws IOException          if e.g. the service cannot be reached
   * @throws InterruptedException the query is interrupted before succeeding
   * @since 1.4.0
   */
  private List<TibTerm> select(String searchTerm, int offset, int limit)
      throws IOException, InterruptedException {
    if (searchTerm.length() < 2) { // avoid unnecessary API calls
      return List.of();
    }
    HttpRequest termSelectQuery = HttpRequest.newBuilder().uri(URI.create(
            selectEndpointAbsoluteUrl.toString() +
                "?q=" + URLEncoder.encode(searchTerm, StandardCharsets.UTF_8) + "&rows="
                + limit + "&start=" + offset + "&ontology="
                + createOntologyFilterQueryParameter()))
        .header("Content-Type", "application/json").GET().build();
    var response = HTTP_CLIENT.send(termSelectQuery, BodyHandlers.ofString());
    return parseResponse(response).stream().toList();
  }

  /**
   * Queries the /search endpoint of the TIB terminology service, but filters any results by the
   * terms `obo_id` property.
   * <p>
   *
   * @param oboId  the search term
   * @param offset the offset of results to query the result
   * @param limit  the max number of results to return per page
   * @return a list of matching terms.
   * @throws IOException          if e.g. the service cannot be reached
   * @throws InterruptedException the query is interrupted before succeeding
   * @since 1.4.0
   */
  private List<TibTerm> searchByOboId(String oboId, int offset, int limit)
      throws IOException, InterruptedException {
    if (oboId.isBlank()) { // avoid unnecessary API calls
      return List.of();
    }
    HttpRequest termSelectQuery = HttpRequest.newBuilder().uri(URI.create(
            searchEndpointAbsoluteUrl.toString() + "?q="
                + URLEncoder.encode(
                //obo_id query field requires `:` separator instead of `_`
                oboId.replace("_", ":"), StandardCharsets.UTF_8)
                + "&queryFields=obo_id"
                + "&rows=" + limit + "&start=" + offset
                + "&ontology=" + createOntologyFilterQueryParameter()))
        .header("Content-Type", "application/json").GET().build();
    var response = HTTP_CLIENT.send(termSelectQuery, BodyHandlers.ofString());
    return parseResponse(response);
  }

  /**
   * Queries the /search endpoint of the TIB terminology service, but filters any results by the
   * terms `obo_id` property.
   * <p>
   *
   * @param oboId the obo id to match exactly
   * @return a list of matching terms.
   * @throws IOException          if e.g. the service cannot be reached
   * @throws InterruptedException the query is interrupted before succeeding
   * @since 1.4.0
   */
  private Optional<TibTerm> searchByOboIdExact(String oboId)
      throws IOException, InterruptedException {
    if (oboId.isBlank()) { // avoid unnecessary API calls
      return Optional.empty();
    }
    HttpRequest termSelectQuery = HttpRequest.newBuilder().uri(URI.create(
            searchEndpointAbsoluteUrl.toString() + "?q="
                + URLEncoder.encode(
                //obo_id query field requires `:` separator instead of `_`
                oboId.replace("_", ":"), StandardCharsets.UTF_8)
                + "&queryFields=obo_id"
                + "&exact=true"
                + "&ontology=" + createOntologyFilterQueryParameter()))
        .header("Content-Type", "application/json").GET().build();
    var response = HTTP_CLIENT.send(termSelectQuery, BodyHandlers.ofString());
    log.debug("Received response code '%d' for term query %s".formatted(response.statusCode(), oboId));
    // If the HTTP status code is not 200, the
    if (response.statusCode() == 404) {
      return Optional.empty();
    }
    if (response.statusCode() == 200) {
      return parseResponse(response).stream().findFirst();
    }
    log.error("Received response code '%d' for term query %s".formatted(response.statusCode(), oboId));
    return Optional.empty();
  }

  /**
   * Parses the TIB service response object and returns the wrapped terms.
   *
   * @param response the TIB service response
   * @return a list of contained terms
   * @since 1.4.0
   */
  private List<TibTerm> parseResponse(HttpResponse<String> response) {
    ObjectMapper mapper = new ObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(
        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    try {
      JsonNode node = mapper.readTree(response.body()).at("/response/docs");
      List<TibTerm> terms = new ArrayList<>();
      for (JsonNode currentNode : node) {
        terms.add(mapper.treeToValue(currentNode, TibTerm.class));
      }
      return terms;
    } catch (JsonProcessingException e) {
      throw wrapProcessingException(e);
    }
  }

  // adds a term to the cache
  private TibTerm updateCache(TibTerm term) {
    cache.add(term);
    return term;
  }

  /**
   * In-memory cache for {@link TibTerm} as failsafe for network interrupts.
   *
   * @since 1.9.0
   */
  static class RequestCache {

    // Pretty random, we need to see what value actual makes sense
    private static final int DEFAULT_CACHE_SIZE = 500;

    private final List<TibTerm> cache = new ArrayList<>();
    private final int limit;
    private List<CacheEntryStat> accessFrequency = new ArrayList<>();

    RequestCache() {
      limit = DEFAULT_CACHE_SIZE;
    }

    RequestCache(int limit) {
      this.limit = limit;
    }

    /**
     * Adds a {@link TibTerm} to the in-memory cache.
     * <p>
     * If the cache max size is reached, the oldest entry will be replaced with the one passed to
     * the function.
     *
     * @param term the term to store in the cache
     * @since 1.9.0
     */
    void add(TibTerm term) {
      if (cache.contains(term)) {
        return;
      }
      if (cache.size() >= limit) {
        addByReplace(term);
        return;
      }
      cache.add(term);
      addStats(new CacheEntryStat(term));
    }

    // Puts the term with the time of caching into an own list for tracking
    private void addStats(CacheEntryStat cacheEntryStat) {
      if (accessFrequency.contains(cacheEntryStat)) {
        return;
      }
      accessFrequency.add(cacheEntryStat);
    }

    // A special case of adding by looking for the oldest cache entry and replacing it with
    // the provided one
    private void addByReplace(TibTerm term) {
      // We want to be sure that the access statistic list is in natural order
      ensureSorted();
      // We then remove the oldest cache entry
      if (!cache.isEmpty()) {
        cache.set(0, term);
        addStats(new CacheEntryStat(term));
      }
    }

    // Ensures the natural order sorting by datetime, when the cache entry has been created
    // Oldest entry will be the first element, newest the last element of the list
    private void ensureSorted() {
      accessFrequency = accessFrequency.stream()
          .sorted(Comparator.comparing(CacheEntryStat::created, Instant::compareTo))
          .collect(Collectors.toList());
    }

    /**
     * Searches for a matching {@link TibTerm} in the cache.
     *
     * @param curie the CURIE to search for
     * @return the search result, {@link Optional#empty()} if no match was found
     * @since 1.9.0
     */
    Optional<TibTerm> findByCurie(String curie) {
      return cache.stream().filter(term -> term.oboId.equals(curie)).findFirst();
    }
  }

  /**
   * A small container for when a cache entry has been created.
   *
   * @since 1.9.0
   */
  static class CacheEntryStat {

    private final TibTerm term;
    private final Instant created;

    CacheEntryStat(TibTerm term) {
      this.term = term;
      created = Instant.now();
    }

    /**
     * When the cache entry has been created
     *
     * @return the instant of creation
     * @since 1.9.0
     */
    Instant created() {
      return created;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CacheEntryStat that = (CacheEntryStat) o;
      return Objects.equals(term, that.term);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(term);
    }
  }
}
