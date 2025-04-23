package life.qbic.projectmanagement.infrastructure.contact;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
import life.qbic.projectmanagement.application.contact.OrcidEntry;
import life.qbic.projectmanagement.application.contact.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
  private static final String PAGINATED_QUERY = "https://pub.orcid.org/v3.0/expanded-search/?start=%s&rows=%s&q=%s";
  private static final String OIDC_ISSUER = "https://orcid.org";
  private final String token;
  private final String refreshToken;
  private final HttpClient httpClient;

  @Autowired
  public OrcidRepository(
      @Value("${spring.security.oauth2.client.registration.orcid.client-id}") String clientID,
      @Value("${spring.security.oauth2.client.registration.orcid.client-secret}") String clientSecret,
      @Value("${spring.security.oauth2.client.provider.orcid.token-uri}") String tokenEndpoint) {
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

  private static OrcidEntry convert(OrcidRecord orcidRecord) {
    var emailList = Arrays.stream(orcidRecord.email()).toList();
    var fullName = orcidRecord.givenName() + orcidRecord.familyName();
    var orcid = orcidRecord.orcidID();
    //If an orcid record does not contain a name or email it is considered invalid and will not be considered further
    if (orcid.isBlank()) {
      return null;
    }
    if (fullName.isBlank()) {
      return null;
    }
    if (emailList.isEmpty()) {
      return null;
    }
    var email = emailList.stream().findFirst().orElse("");
    return new OrcidEntry(fullName, email, orcid, OIDC_ISSUER);
  }

  @Override
  public List<OrcidEntry> findAll(String query, int limit, int offset) {
    var queryUrl = String.format(PAGINATED_QUERY, offset, limit, query);
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
          var foundRecords = new ArrayList<>(
              Arrays.stream(mapper.convertValue(value, OrcidRecord[].class))
                  .map(OrcidRepository::convert).toList());
          var invalidRecords = foundRecords.stream().filter(Objects::isNull).toList();
          //Filter null values resulting from invalid records on OrcIds side
          var validRecords = foundRecords.stream().filter(Objects::nonNull).toList();
          log.info(
              "From a total of %d parsed orcid records, %d were valid and %d were invalid".formatted(
                  foundRecords.size(), validRecords.size(), invalidRecords.size()));
          return validRecords;
        } else {
          return new ArrayList<>();
        }
      } else {
        log.error("Error getting orcid records due to " + response.statusCode());
        throw new OrcidRepository.QueryException(
            "Orcid Public repository does not seem to be available",
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
