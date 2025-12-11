package life.qbic.projectmanagement.infrastructure.organisations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.eclipse.jetty.http.HttpStatus;

/**
 * API for research organisation registry. ROR (https://ror.org/)
 */
public interface RorApi {

  /**
   * <b>Research Organisation Registry Entry</b>
   *
   * <p>A ROR entry that is returned by the ROR API.</p>
   *
   * @since 1.0.0
   */
  interface RorEntry {

    /**
     *
     * @return the complete ROR identifier
     */
    String getId();

    /**
     *
     * @return the displayed name for the organization. This is not guaranteed to be in a specific
     * language.
     */
    String getDisplayedName();

  }

  /**
   * Searches for an entry in the research organisation registry. If no identifier is provided or
   * the provided identifier is empty, {@link Optional#empty()} is returned.
   *
   * @param rorId the ror identifier to search for e.g. 00v34f693 for QBiC
   * @return an optional RorEntry
   * @since 1.13.0
   */
  Optional<RorEntry> find(String rorId);

  final class RorApiV2 implements RorApi {

    private static final Logger log = LoggerFactory.logger(RorApiV2.class);
    private final URI organisationApiEndpoint;
    private final String apiClientId;

    public RorApiV2(String organisationApiEndpoint, String apiClientId) {
      this.organisationApiEndpoint = URI.create(organisationApiEndpoint);
      this.apiClientId = Objects.requireNonNull(apiClientId);
    }

    public static final class RorEntryV2 implements RorEntry {

      @JsonProperty("id")
      String id;

      @JsonProperty("names")
      List<OrganisationName> names;

      public static class OrganisationName {

        @JsonProperty("lang")
        String language;

        @JsonProperty("types")
        List<String> types;

        @JsonProperty("value")
        String value;

        public String getLanguage() {
          return language;
        }

        public List<String> getTypes() {
          return types;
        }

        public String getValue() {
          return value;
        }
      }

      public String getId() {
        return id;
      }

      @Override
      public String getDisplayedName() {
        return names.stream().filter(name -> name.getTypes().contains("ror_display")).findFirst()
            .map(OrganisationName::getValue)
            .orElseThrow();
      }
    }

    @Override
    public Optional<RorEntry> find(String rorId) {
      if (rorId == null || rorId.isBlank()) {
        log.warn("API called without ror identifier. Skipping call and returning empty.");
        return Optional.empty();
      }

      var request = createHttpRequest(organisationApiEndpoint.resolve(rorId));

      RorResponse result;
      try {
        result = sendRequest(request);
      } catch (RorRequestException e) {
        log.error(
            "Could not request information from %s.".formatted(request.uri()), e);
        return Optional.empty();
      }

      Optional<RorEntry> rorEntry = result.optionalBody()
          .flatMap(this::parseJson);

      if (rorEntry.isEmpty()) {
        log.warn(
            "No organisation with identifier %s was found on %s.".formatted(rorId, request.uri()));
      }

      return rorEntry;
    }

    private Optional<RorEntry> parseJson(String json) {
      try {
        RorEntry rorEntry = new ObjectMapper().configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(json, RorEntryV2.class);
        return Optional.of(rorEntry);
      } catch (JsonProcessingException e) {
        log.error("Could not parse response from ROR.", e);
        return Optional.empty();
      }
    }


    /**
     * Needs to be handled
     */
    private static class RorRequestException extends Exception {

      private final Integer httpStatusCode;


      public RorRequestException(Throwable cause) {
        super(cause);
        httpStatusCode = null;
      }

      public RorRequestException(String message, int httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
      }

      public Optional<Integer> getHttpStatusCode() {
        return Optional.ofNullable(httpStatusCode);
      }
    }

    private record RorResponse(String body) {

      public Optional<String> optionalBody() {
        return Optional.ofNullable(body);
      }

      @Override
      public String body() {
        if (Objects.isNull(body)) {
          throw new NoSuchElementException(
              "No body is present. Please use RorResponse#optionalBody instead");
        }
        return body;
      }

      static RorResponse empty() {
        return new RorResponse(null);
      }

      static RorResponse of(String body) {
        Objects.requireNonNull(body);
        return new RorResponse(body);
      }
    }

    private RorResponse sendRequest(HttpRequest request) throws RorRequestException {
      HttpResponse<String> result;
      try (HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2)
          .followRedirects(Redirect.NORMAL).connectTimeout(
              Duration.ofSeconds(10)).build();) {

        result = client.send(request, BodyHandlers.ofString());
        //If a valid RoRId was provided but the ID does not exist we fail
      } catch (IOException e) {
        throw new RorRequestException(e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RorRequestException(e);
      }

      if (result.statusCode() == HttpStatus.NOT_FOUND_404) {
        log.warn("Organisation not found for " + request.uri());
        return RorResponse.empty();
      }
      //If a valid RoRId was provided but the ID does not exist we fail
      if (result.statusCode() != 200) {
        throw new RorRequestException(
            "Unexpected HTTP status code returned from retrieving an organization.",
            result.statusCode());
      } else {
        return RorResponse.of(result.body());
      }
    }

    private HttpRequest createHttpRequest(URI requestUri) {
      var queryBuilder = HttpRequest.newBuilder()
          .uri(requestUri)
          .header("Content-Type", "application/json")
          .header("Client-Id", apiClientId)
          .GET();

      return queryBuilder.build();
    }
  }


}
