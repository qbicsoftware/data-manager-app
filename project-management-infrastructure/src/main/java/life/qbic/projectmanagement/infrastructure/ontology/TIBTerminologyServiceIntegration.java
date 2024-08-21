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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ontology.LookupException;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.application.ontology.TerminologySelect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <b>TIB Terminology Service</b>
 * <p>
 * Integrates the tiB Terminology Service API Endpoint to support rich ontology terms.
 *
 * @since 1.4.0
 */
@Service
public class TIBTerminologyServiceIntegration implements TerminologySelect {

  private static final Logger log = logger(TIBTerminologyServiceIntegration.class);
  private static final int TIMEOUT_5_SECONDS = 5;
  private static final HttpClient HTTP_CLIENT = httpClient(TIMEOUT_5_SECONDS);

  private static final List<String> ONTOLOGIES_WHITELIST = List.of(
      "bao", // Bio-assay Ontology
      "bto", // Brenda Tissue Ontology
      "efo", // Experimental Factor Ontology
      "ms",  // PSI Mass Spectrometry Ontology
      "ncit", // National Cancer Institute Thesaurus
      "envo" // Environmental Factor Ontology
  );

  private final URI selectEndpointAbsoluteUrl;
  private final URI searchEndpointAbsoluteUrl;

  @Autowired
  public TIBTerminologyServiceIntegration(
      @Value("${terminology.service.tib.endpoint.select}") String selectEndpoint,
      @Value("${terminology.service.tib.endpoint.search}") String searchEndpoint,
      @Value("${terminology.service.tib.api.url}") String tibApiUrl) {
    this.selectEndpointAbsoluteUrl = URI.create(tibApiUrl).resolve(selectEndpoint);
    this.searchEndpointAbsoluteUrl = URI.create(tibApiUrl).resolve(searchEndpoint);
  }

  private static OntologyClass convert(TibTerm term) {
    return new OntologyClass(term.ontologyPrefix, "", term.iri, term.label, term.shortForm,
        term.getDescription().orElse(""), "");
  }

  private static String createOntologyFilterQueryParameter() {
    return String.join(",", ONTOLOGIES_WHITELIST);
  }

  private static HttpClient httpClient(int timeoutSeconds) {
    return HttpClient.newBuilder().version(Version.HTTP_2)
        .followRedirects(Redirect.NORMAL).connectTimeout(
            Duration.ofSeconds(timeoutSeconds)).build();
  }

  @Override
  public List<OntologyClass> query(String searchTerm, int offset, int limit)
      throws LookupException {
    try {
      List<TibTerm> result = select(searchTerm, offset, limit);
      return result.stream().map(TIBTerminologyServiceIntegration::convert).toList();
    } catch (IOException | InterruptedException e) {
      log.error("TIB Service search failed. ", e);
      throw new LookupException("Query failed. Please try again.");
    } catch (Exception e) {
      log.error("Unknown exception during TIB search. ", e);
      throw new LookupException("Query failed. Please try again.");
    }
  }

  @Override
  public Optional<OntologyClass> searchByCurie(String curie) throws LookupException {
    try {
      List<TibTerm> result = searchByOboId(curie, 0, 10);
      if (result.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(
          result.stream().map(TIBTerminologyServiceIntegration::convert).toList().get(0));
    } catch (IOException | InterruptedException e) {
      log.error("TIB Service search failed. ", e);
      throw new LookupException("Query failed. Please try again.");
    } catch (Exception e) {
      log.error("Unknown exception during TIB search. ", e);
      throw new LookupException("Query failed. Please try again.");
    }
  }

  @Override
  public List<OntologyClass> search(String searchTerm, int offset, int limit)
      throws LookupException {
    try {
      List<TibTerm> result = fullSearch(searchTerm, offset, limit);
      return result.stream().map(TIBTerminologyServiceIntegration::convert).toList();
    } catch (IOException | InterruptedException e) {
      log.error("TIB Service search failed. ", e);
      throw new LookupException("Query failed. Please try again.");
    } catch (Exception e) {
      log.error("Unknown exception during TIB search. ", e);
      throw new LookupException("Query failed. Please try again.");
    }
  }

  private List<TibTerm> fullSearch(String searchTerm, int offset, int limit)
      throws IOException, InterruptedException, ApplicationException {
    if (searchTerm.isBlank()) { // avoid unnecessary API calls
      return List.of();
    }
    HttpRequest termSelectQuery = HttpRequest.newBuilder().uri(URI.create(
            searchEndpointAbsoluteUrl.toString() + "?q=" + URLEncoder.encode(searchTerm,
                StandardCharsets.UTF_8) + "&rows="
                + limit + "&start=" + offset + "&ontology=" + createOntologyFilterQueryParameter()))
        .header("Content-Type", "application/json").GET().build();
    var response = HTTP_CLIENT.send(termSelectQuery, BodyHandlers.ofString());
    return parseResponse(response);
  }

  private List<TibTerm> select(String searchTerm, int offset, int limit)
      throws IOException, InterruptedException, ApplicationException {
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
    return parseResponse(response);
  }

  private List<TibTerm> searchByOboId(String oboId, int offset, int limit)
      throws IOException, InterruptedException, ApplicationException {
    if (oboId.isBlank()) { // avoid unnecessary API calls
      return List.of();
    }
    HttpRequest termSelectQuery = HttpRequest.newBuilder().uri(URI.create(
            searchEndpointAbsoluteUrl.toString() + "?q=" + URLEncoder.encode(oboId,
                StandardCharsets.UTF_8) + "&rows="
                + limit + "&start=" + offset + "&ontology=" + createOntologyFilterQueryParameter()
                + "&queryFields=obo_id"))
        .header("Content-Type", "application/json").GET().build();
    var response = HTTP_CLIENT.send(termSelectQuery, BodyHandlers.ofString());
    return parseResponse(response);
  }

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
      log.error("Terminology Term Failure: Cannot process API response.", e);
      throw new ApplicationException("Terminology service call failed.", e);
    }
  }
}


