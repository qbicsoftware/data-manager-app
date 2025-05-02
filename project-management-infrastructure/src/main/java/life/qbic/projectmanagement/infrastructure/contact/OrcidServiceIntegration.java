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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.contact.OrcidEntry;
import life.qbic.projectmanagement.application.contact.PersonSelect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * <b>Orcid Service Integration</b>
 *
 * <p>Implementation for the {@link PersonSelect} interface.
 *
 * <p>Integrates the Orcid Service API Endpoint to support querying for publicly available orcid
 * information
 */
@Service
public class OrcidServiceIntegration implements PersonSelect {

  private static final Logger log = logger(OrcidServiceIntegration.class);
  private static final String OIDC_ISSUER = "https://orcid.org";
  private final String searchEndpoint;
  private final String token;
  private final String refreshToken;
  private final HttpClient httpClient;

  @Autowired
  public OrcidServiceIntegration(
      @Value("${qbic.external-service.person-search.orcid.client-id}") String clientID,
      @Value("${qbic.external-service.person-search.orcid.client-secret}") String clientSecret,
      @Value("${qbic.external-service.person-search.orcid.token-uri}") String tokenEndpoint,
      @Value("${qbic.external-service.person-search.orcid.extended-search-uri}") String searchEndpoint,
      @Value("${qbic.external-service.person-search.orcid.scope}") String scope,
      @Value("${qbic.external-service.person-search.orcid.grant-type}") String grantType) {
    this.searchEndpoint = searchEndpoint;
    httpClient = createHttpClient();
    var authResponse = authenticate(httpClient, clientID, clientSecret, tokenEndpoint, scope,
        grantType);
    this.refreshToken = authResponse.refresh_token();
    this.token = authResponse.access_token();
  }

  private static HttpClient createHttpClient() {
    return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(
            Duration.ofSeconds(10)).build();
  }

  private static AuthResponse authenticate(HttpClient client, String clientID, String clientSecret,
      String tokenEndpoint, String scope, String grantType) {
    var params = Map.of("client_id", clientID, "client_secret", clientSecret, "scope",
        scope, "grant_type", grantType);
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
      throw new OrcidServiceIntegration.QueryException("Authentication failed", e);
    }

  }

  private static OrcidEntry convert(OrcidRecord orcidRecord) {
    var emailList = Arrays.stream(orcidRecord.email()).toList();
    if (orcidRecord.orcidID() == null || orcidRecord.orcidID().isEmpty()) {
      return null;
    }
    if (orcidRecord.givenName() == null || orcidRecord.givenName().isEmpty()) {
      return null;
    }
    if (emailList.isEmpty()) {
      return null;
    }
    //The family Name is not required by orcid
    String familyName = Optional.ofNullable(orcidRecord.familyName()).orElse("");
    var fullName = (orcidRecord.givenName().trim() + " " + familyName).trim();
    var orcid = orcidRecord.orcidID();
    var email = emailList.stream().findFirst()
        .orElseThrow(); //cannot throw as isEmpty is checked before
    return new OrcidEntry(fullName, email, orcid, OIDC_ISSUER);
  }

  private static String buildQueryUrl(String query, int limit, int offset) {
    String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
    String queryChainSymbol = "&";
    Map<String, String> paginationParameters = Map.of("rows", String.valueOf(limit), "start",
        String.valueOf(offset));
    StringBuilder queryBuilder = new StringBuilder("?");
    for (Entry<String, String> entry : paginationParameters.entrySet()) {
      queryBuilder.append(entry.getKey()).append("=")
          .append(entry.getValue())
          .append(queryChainSymbol);
    }
    Map<String, String> additionalParameters = Map.of("email", "*", "given-names", "*",
        "family-name", "*");
    String combinedAdditionalParameters = URLEncoder.encode(additionalParameters.entrySet().stream()
        .map(entry -> entry.getKey() + ":" + entry.getValue())
        .collect(Collectors.joining(" & ")), StandardCharsets.UTF_8);
    return queryBuilder.append("q=").append(encodedQuery).append("&")
        .append(combinedAdditionalParameters).toString();
  }

  @Override
  public List<OrcidEntry> findAll(String query, int limit, int offset) {
    var urlWithEncodedQuery = searchEndpoint + "/" + buildQueryUrl(query, limit, offset);
    URI uri;
    try {
      uri = URI.create(urlWithEncodedQuery);
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
          .map(OrcidServiceIntegration::convert).toList();
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
