package life.qbic.projectmanagement.infrastructure.contact;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
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
  private static final String PAGINATED_QUERY = "https://pub.sandbox.orcid.org/v3.0/expanded-search/?start=%s&rows=%s&q=%s";
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
  }

  private static HttpClient createHttpClient() {
    return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(
            Duration.ofSeconds(10)).build();
  }

  private static AuthResponse authenticate(HttpClient client, String clientID, String clientSecret,
      String tokenEndpoint) {
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
      throw new OrcidRepository.QueryException("Authentication failed", e);
    }

  }

  private static PersonEntry convert(OrcidRecord record) {
    return new PersonEntry(record.givenName + record.familyName,
        Arrays.stream(record.email()).findFirst().orElse(""), "https://orcid.org/" + record.orcidID,
        record.orcidID);
  private static Contact convert(OrcidRecord record) {

    return new Contact(record.givenName + record.familyName,
        //FixMe How should incomplete orcid records be handled
        Arrays.stream(record.email()).findFirst().orElse(""), record.orcidID, "https://orcid.org");
  }

  @Override
  public List<Contact> findAll(String query, int limit, int offset) {
    //Orcid queries will fail if the user input is not sanitized
    var sanitizedInput = query.replaceAll("[^a-zA-Z0-9]", "");
    var queryUrl = String.format(PAGINATED_QUERY, offset, limit, sanitizedInput);
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(queryUrl))
        .headers("Authorization", "Bearer " + token, "Accept", "application/json").GET()
        .build();
    try {
      var response = httpClient.send(request, BodyHandlers.ofString());
      ObjectMapper mapper = new ObjectMapper();
      var node = mapper.readTree(response.body());
      var value = node.get("expanded-result");
      if (response.statusCode() == HttpStatus.OK.value()) {
        if (!value.isEmpty()) {
          return new ArrayList<>(
              Arrays.stream(mapper.convertValue(value, OrcidRecord[].class))
                  .map(OrcidRepository::convert).toList());
        } else {
          return new ArrayList<>();
        }
      } else {
        log.debug("Error getting orcid records due to " + response.statusCode());
        throw new OrcidRepository.QueryException("Person repository seems not available",
            new Throwable(response.body()));
      }
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.error("Error sending orcid request", e);
      throw new OrcidRepository.QueryException("Person repository seems not available", e);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record AuthResponse(String access_token, String token_type, String expires_in,
                      String refresh_token, String scope, String orcid) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record OrcidRecord(@JsonProperty("orcid-id") String orcidID,
                     @JsonProperty("given-names") String givenName,
                     @JsonProperty("family-names") String familyName,
                     @JsonProperty("credit-name") String creditName,
                     @JsonProperty("email") String[] email) {

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      OrcidRecord that = (OrcidRecord) o;
      return Objects.equals(orcidID, that.orcidID) && Objects.deepEquals(email,
          that.email) && Objects.equals(givenName, that.givenName)
          && Objects.equals(familyName, that.familyName) && Objects.equals(
          creditName, that.creditName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(orcidID, givenName, familyName, creditName, Arrays.hashCode(email));
    }
  }

  static class QueryException extends RuntimeException {

    public QueryException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
