package life.qbic.projectmanagement.infrastructure.organisations;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import life.qbic.projectmanagement.application.OrganisationRepository;
import life.qbic.projectmanagement.domain.Organisation;
import life.qbic.projectmanagement.infrastructure.organisations.RorApi.RorEntry;
import org.springframework.context.annotation.Profile;

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
@Profile("production")
public class CachedOrganisationRepository implements OrganisationRepository {

  private static final int DEFAULT_CACHE_SIZE = 50;
  private static final String ROR_ID_PATTERN = "0[a-z|0-9]{6}[0-9]{2}$";
  private final Map<String, String> iriToOrganisation = new HashMap<>();
  private final int configuredCacheSize;
  private boolean cacheUsedForLastRequest = false;

  private final RorApi rorApi;

  public CachedOrganisationRepository(RorApi rorApi) {
    this.rorApi = Objects.requireNonNull(rorApi);
    this.configuredCacheSize = DEFAULT_CACHE_SIZE;
  }

  public CachedOrganisationRepository(int cacheSize, RorApi rorApi) {
    this.configuredCacheSize = cacheSize;
    this.rorApi = Objects.requireNonNull(rorApi);
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
    return extractRorId(iri)
        .flatMap(this::findOrganisationInROR);
  }

  private Optional<Organisation> findOrganisationInROR(String rorId) {
    Optional<RorEntry> rorEntry = rorApi.find(rorId);
    rorEntry.ifPresent(entry -> {
      updateCache(entry);
      cacheUsedForLastRequest = false;
    });
    return rorEntry
        .map(entry -> new Organisation(entry.getId(), entry.getDisplayedName()));
  }

  private void updateCache(RorEntry rorEntry) {
    if (iriToOrganisation.size() == configuredCacheSize) {
      String firstKey = iriToOrganisation.keySet().stream().toList().get(0);
      iriToOrganisation.remove(firstKey);
    }
    iriToOrganisation.put(rorEntry.getId(), rorEntry.getDisplayedName());
  }

  public int cacheEntries() {
    return iriToOrganisation.size();
  }

  public boolean cacheUsedForLastRequest() {
    return cacheUsedForLastRequest;
  }
}
