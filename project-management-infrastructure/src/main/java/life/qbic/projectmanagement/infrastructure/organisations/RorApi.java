package life.qbic.projectmanagement.infrastructure.organisations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

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

      try (HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2)
          .followRedirects(Redirect.NORMAL).connectTimeout(
              Duration.ofSeconds(10)).build();) {

        URI requestUri = organisationApiEndpoint.resolve(rorId);

        var queryBuilder = HttpRequest.newBuilder()
            .uri(requestUri)
            .header("Content-Type", "application/json")
            .header("Client-Id", apiClientId)
            .GET();

        var rorQuery = queryBuilder.build();
        var result = client.send(rorQuery, BodyHandlers.ofString());
        //If a valid RoRId was provided but the ID does not exist we fail
        if (result.statusCode() == 404) {
          log.warn(
              "Provided Organisation ROR id: %s was not found via API call to %s".formatted(rorId,
                  requestUri));
          return Optional.empty();
        }
        //If a valid RoRId was provided but the ID does not exist we fail
        else if (result.statusCode() != 200) {
          log.warn(
              ("Unexpected error retrieving an organization with ROR id %s. API call to %s").formatted(
                  rorId,
                  requestUri));
          return Optional.empty();
        }
        RorEntry rorEntry = new ObjectMapper().configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(result.body(), RorEntryV2.class);
        return Optional.of(rorEntry);
      } catch (IOException | InterruptedException e) {
        log.error("Finding ROR entry failed for organisation: %s".formatted(rorId), e);
        /* Clean up whatever needs to be handled before interrupting  */
        Thread.currentThread().interrupt();
        return Optional.empty();
      }
    }
  }


}
