package life.qbic.projectmanagement.infrastructure.ontology;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Duration;
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

  private final String selectEndpoint;
  private final String tibApiUrl;
  private final URI selectEndpointAbsuluteUrl;

  @Autowired
  public TIBTerminologyServiceIntegration(
      @Value("${tib.terminology.service.endpoint.select}") String selectEndpoint,
      @Value("${tib.terminology.service.api.url}") String tibApiUrl) {
    this.selectEndpoint = selectEndpoint;
    this.tibApiUrl = tibApiUrl;
    this.selectEndpointAbsuluteUrl = URI.create(tibApiUrl).resolve(selectEndpoint);
  }

  @Override
  public List<OntologyTerm> query(String searchTerm, int offset, int limit) {
    try {
      List<TibTerm> result = select(searchTerm, offset, limit);
    } catch (IOException | InterruptedException e) {
      log.error("TIB Service query failed. ", e);
      throw new ApplicationException("Query failed. Please try again.");
    }
    return List.of();
  }

  public List<TibTerm> select(String searchTerm, int offset, int limit)
      throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2)
        .followRedirects(Redirect.NORMAL).connectTimeout(
            Duration.ofSeconds(10)).build();
    HttpRequest termSelectQuery = HttpRequest.newBuilder().uri(URI.create(selectEndpointAbsuluteUrl.toString() + "?q=" + searchTerm))
        .header("Content-Type", "application/json").GET().build();
    var response = client.send(termSelectQuery, BodyHandlers.ofString());
    return List.of();
  }
}
