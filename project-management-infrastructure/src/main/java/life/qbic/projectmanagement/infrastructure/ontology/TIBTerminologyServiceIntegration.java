package life.qbic.projectmanagement.infrastructure.ontology;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ontology.TerminologySelect;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class TIBTerminologyServiceIntegration implements TerminologySelect {

  private static final Logger log = logger(TIBTerminologyServiceIntegration.class);
  private static final List<String> ONTOLOGIES_WHITELIST = new ArrayList<>();

  static {
    ONTOLOGIES_WHITELIST.add("bao");
    ONTOLOGIES_WHITELIST.add("bto");
    ONTOLOGIES_WHITELIST.add("efo");
    ONTOLOGIES_WHITELIST.add("ms");
    ONTOLOGIES_WHITELIST.add("ncit");
    ONTOLOGIES_WHITELIST.add("envo");
  }

  private final URI selectEndpointAbsoluteUrl;

  @Autowired
  public TIBTerminologyServiceIntegration(
      @Value("${tib.terminology.service.endpoint.select}") String selectEndpoint,
      @Value("${tib.terminology.service.api.url}") String tibApiUrl) {
    this.selectEndpointAbsoluteUrl = URI.create(tibApiUrl).resolve(selectEndpoint);
  }

  private static OntologyTerm convert(TibTerm term) {
    return new OntologyTerm(term.ontologyPrefix, "", term.iri, term.label, term.shortForm, "", "");
  }

  private static String createOntologyFilterQueryParameter() {
    return String.join(",", ONTOLOGIES_WHITELIST);
  }

  @Override
  public List<OntologyTerm> query(String searchTerm, int offset, int limit) {
    try {
      List<TibTerm> result = select(searchTerm, offset, limit);
      return result.stream().map(TIBTerminologyServiceIntegration::convert).toList();
    } catch (IOException | InterruptedException e) {
      log.error("TIB Service query failed. ", e);
      throw new ApplicationException("Query failed. Please try again.");
    }
  }

  public List<TibTerm> select(String searchTerm, int offset, int limit)
      throws IOException, InterruptedException {
    if (searchTerm.length() < 2) {
      return List.of();
    }
    HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2)
        .followRedirects(Redirect.NORMAL).connectTimeout(
            Duration.ofSeconds(10)).build();
    HttpRequest termSelectQuery = HttpRequest.newBuilder().uri(URI.create(
            selectEndpointAbsoluteUrl.toString() + "?q=" + searchTerm.replace(" ", "%20") + "&rows="
                + limit + "&start=" + offset + "&ontology=" + createOntologyFilterQueryParameter()))
        .header("Content-Type", "application/json").GET().build();
    var response = client.send(termSelectQuery, BodyHandlers.ofString());
    return parseResponse(response);
  }

  private List<TibTerm> parseResponse(HttpResponse<String> response) {
    ObjectMapper mapper = new ObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      JsonNode node = mapper.readTree(response.body()).at("/response/docs");
      List<TibTerm> terms = new ArrayList<>();
      for (JsonNode currentNode : node) {
        terms.add(mapper.treeToValue(currentNode, TibTerm.class));
      }
      return terms;
    } catch (JsonProcessingException e) {
      log.error("Terminology Term Failure: Cannot process API response.", e);
    }
    return List.of();
  }
}


