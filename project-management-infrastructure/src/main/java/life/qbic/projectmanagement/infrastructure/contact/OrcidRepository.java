package life.qbic.projectmanagement.infrastructure.contact;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.contact.PersonEntry;
import life.qbic.projectmanagement.application.contact.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Repository
public class OrcidRepository implements PersonRepository {

  private static final Logger log = logger(OrcidRepository.class);
  private final String tokenEndpoint;
  private String token;
  private String refreshToken;
  private HttpClient httpClient;

  @Autowired
  public OrcidRepository(@Value("${qbic.orcid.api.client.id}") String clientID,
      @Value("${qbic.orcid.api.client.secret}") String clientSecret,
      @Value("${qbic.orcid.api.endpoint.token}") String tokenEndpoint) {
    httpClient = createHttpClient();
    var authResponse = authenticate(httpClient, clientID, clientSecret, tokenEndpoint);
    this.refreshToken = authResponse.refresh_token();
    this.token = authResponse.access_token();
    this.tokenEndpoint = tokenEndpoint;
  }

  private static HttpClient createHttpClient() {
    return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(
            Duration.ofSeconds(10)).build();
  }

  private static AuthResponse authenticate(HttpClient client, String clientID, String clientSecret, String tokenEndpoint) {
    var params = Map.of("client_id", clientID, "client_secret", clientSecret, "scope",
        "/read-public", "grant_type", "client_credentials");

    String form = params.entrySet().stream()
        .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
            URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));

    // Build the HTTP request
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(tokenEndpoint))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(form))
        .build();

    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      ObjectMapper mapper = new ObjectMapper();
      try (var parser = mapper.createParser(response.body())) {
        return parser.readValueAs(AuthResponse.class);
      }
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.error("Error sending orcid request", e);
      throw new QueryException("Authentication failed", e);
    }

  }

  @Override
  public List<PersonEntry> findAll(String query, int limit, int offset) {
    return List.of();
  }
}

class QueryException extends RuntimeException {
  public QueryException(String message, Throwable cause) {
    super(message, cause);
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AuthResponse(String access_token, String token_type, String expires_in, String refresh_token, String scope, String orcid) {

}
