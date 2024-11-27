package life.qbic.projectmanagement.infrastructure;

import static life.qbic.logging.service.LoggerFactory.logger;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.OrganisationRepository;
import life.qbic.projectmanagement.domain.Organisation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>Cached Organisation Repository</b>
 *
 * <p>Implementation of the {@link OrganisationRepository} interface.</p>
 * <p>
 * This implementation allows for caching of organisation lookups via their ROR-ID.
 * <p>
 * The caching enables faster lookups for frequent organisation requests, which we assume will be
 * the most likely case. ROR-IDs are stable and persistent by definition, so locally caching them is
 * a valid strategy.
 *
 * @since 1.0.0s
 */
public class CachedOrganisationRepository implements OrganisationRepository {

  private static final Logger log = logger(CachedOrganisationRepository.class);
  private static final int DEFAULT_CACHE_SIZE = 50;
  private static final String ROR_API_URL = "https://api.ror.org/organizations/%s";
  private static final String ROR_ID_PATTERN = "0[a-z|0-9]{6}[0-9]{2}$";
  private final Map<String, String> iriToOrganisation = new HashMap<>();
  private final int configuredCacheSize;

  private boolean cacheUsedForLastRequest = false;



  public CachedOrganisationRepository(int cacheSize) {
    this.configuredCacheSize = cacheSize;
  }

  public CachedOrganisationRepository() {
    this.configuredCacheSize = DEFAULT_CACHE_SIZE;
  }

  private static Optional<String> extractRorId(String text) {
    var pattern = Pattern.compile(ROR_ID_PATTERN);
    var matcher = pattern.matcher(text);

    return matcher.results().map(MatchResult::group).findFirst();
  }

  @Override
  public Optional<Organisation> resolve(String iri) {
    return lookupCache(iri).or(() -> lookupROR(iri));
  }

  private Optional<Organisation> lookupCache(String iri) {
    if (iriToOrganisation.containsKey(iri)) {
      cacheUsedForLastRequest = true;
      return Optional.of(new Organisation(iri, iriToOrganisation.get(iri)));
    }
    return Optional.empty();
  }

  private Optional<Organisation> lookupROR(String iri) {
    return extractRorId(iri).map(this::findOrganisationInROR).or(Optional::empty);
  }

  private Organisation findOrganisationInROR(String rorId) {
    try {
      HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2)
          .followRedirects(Redirect.NORMAL).connectTimeout(
              Duration.ofSeconds(10)).build();
      HttpRequest rorQuery = HttpRequest.newBuilder().uri(URI.create(ROR_API_URL.formatted(rorId)))
          .header("Content-Type", "application/json").GET().build();
      var result = client.send(rorQuery, BodyHandlers.ofString());
      //If a valid RoRId was provided but the ID does not exist we fail
      if (result.statusCode() != 200) {
        log.warn(
            "Provided Organisation ROR id: %s was not found via API call to %s".formatted(rorId,
                ROR_API_URL));
        return null;
      }
      RORentry rorEntry = new ObjectMapper().configure(
              DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .readValue(result.body(), RORentry.class);
      updateCache(rorEntry);
      cacheUsedForLastRequest = false;
      return new Organisation(rorEntry.getId(), rorEntry.getName());
    } catch (IOException | InterruptedException e) {
      log.error("Finding ROR entry failed for organisation: %s".formatted(rorId), e);
      /* Clean up whatever needs to be handled before interrupting  */
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private void updateCache(RORentry rorEntry) {
    if (iriToOrganisation.size() == configuredCacheSize) {
      String firstKey = iriToOrganisation.keySet().stream().toList().get(0);
      iriToOrganisation.remove(firstKey);
    }
    iriToOrganisation.put(rorEntry.getId(), rorEntry.getName());
  }

  public int cacheEntries() {
    return iriToOrganisation.size();
  }

  public boolean cacheUsedForLastRequest() {
    return cacheUsedForLastRequest;
  }
}
