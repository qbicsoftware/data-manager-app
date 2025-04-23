package life.qbic.projectmanagement.infrastructure.contact;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
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
  private final String paginatedQuery;
  private static final String OIDC_ISSUER = "https://orcid.org";
  private final String token;
  private final String refreshToken;
  private final HttpClient httpClient;

  @Autowired
  public OrcidRepository(
      @Value("${qbic.external-service.person-search.orcid.client-id}") String clientID,
      @Value("${qbic.external-service.person-search.orcid.client-secret}") String clientSecret,
      @Value("${qbic.external-service.person-search.orcid.token-uri}") String tokenEndpoint,
      @Value("${qbic.external-service.person-search.orcid.extended-search-uri}") String paginatedQuery) {
    this.paginatedQuery = paginatedQuery;
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
    String url = paginatedQuery;
    url += "+AND+email:*"; //require an email to be present
    url += "+AND+given-names:*"; //require an email to be present
    url += "+AND+family-name:*"; //require an email to be present
    var queryUrl = String.format(url, offset, limit, query);
    URI uri;
    try {
      uri = URI.create(queryUrl);
    } catch (IllegalArgumentException invalidQuery) {
      log.error(invalidQuery.getMessage(), invalidQuery);
      return List.of();
    }
    HttpRequest request = HttpRequest.newBuilder().uri(uri)
        .headers("Authorization", "Bearer " + token, "Accept", "application/json").GET()
        .build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, BodyHandlers.ofString());
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      return List.of();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error(e.getMessage(), e);
      return List.of();
    }
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node;
    try {
      node = mapper.readTree(response.body());
    } catch (JsonProcessingException e) {
      log.error(e.getMessage(), e);
      return List.of();
    }
    if (!node.has("expanded-result")) {
      log.error("Could not parse orcid response: " + node);
      return List.of();
    }
    var value = node.get("expanded-result");
    HttpStatus httpStatus = HttpStatus.resolve(response.statusCode());
    if (value.isEmpty()) {
      return List.of();
    }
    if (httpStatus != null && httpStatus.is2xxSuccessful()) {
      List<OrcidEntry> foundRecords = Arrays.stream(mapper.convertValue(value, OrcidRecord[].class))
          .map(OrcidRepository::convert).toList();
      return foundRecords.stream()
          .filter(Objects::nonNull)
          .toList();
    } else {
      log.error("Error getting orcid records due to " + response.statusCode());
      return List.of();
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

    @Override
    public String toString() {
      return new StringJoiner(", ", OrcidRecord.class.getSimpleName() + "[", "]")
          .add("orcidID='" + orcidID + "'")
          .add("givenName='" + givenName + "'")
          .add("familyName='" + familyName + "'")
          .add("creditName='" + creditName + "'")
          .add("email=" + Arrays.toString(email))
          .toString();
    }
  }

  static class QueryException extends RuntimeException {

    public QueryException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
