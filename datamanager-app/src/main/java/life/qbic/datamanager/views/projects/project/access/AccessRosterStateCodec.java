package life.qbic.datamanager.views.projects.project.access;

import com.vaadin.flow.router.QueryParameters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.AccessFilter;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;

/**
 * (De)serialises the {@link AccessRosterState} of the project access roster to and from the query
 * parameters of the route location.
 * <p>
 * Parameter layout (single-valued, stable order): {@code q} (search query), {@code filter}
 * (principal type: all/people/groups), {@code role} (role filter: read/write/admin/owner). Absent
 * or invalid values fall back to sensible defaults so that stale or hand-crafted URLs never break
 * the view. Values are URL-encoded by Vaadin's {@link QueryParameters} when written to the browser
 * history, so search terms containing special characters survive natural browser navigation.
 *
 * @since 1.20.0
 */
public final class AccessRosterStateCodec {

  private static final String SEARCH_PARAMETER = "q";
  private static final String TYPE_FILTER_PARAMETER = "filter";
  private static final String ROLE_FILTER_PARAMETER = "role";

  private AccessRosterStateCodec() {}

  /**
   * Parses an access roster state from URL query parameters, applying defaults for absent or
   * invalid values.
   *
   * @param parameters the query parameters of the current location
   * @return the parsed roster state, never {@code null}
   */
  public static AccessRosterState parse(QueryParameters parameters) {
    String search = parameters.getSingleParameter(SEARCH_PARAMETER)
        .map(String::trim)
        .orElse("");
    AccessFilter filter = parameters.getSingleParameter(TYPE_FILTER_PARAMETER)
        .flatMap(AccessRosterStateCodec::parseFilter)
        .orElse(AccessFilter.ALL);
    Optional<ProjectRole> role = parameters.getSingleParameter(ROLE_FILTER_PARAMETER)
        .flatMap(AccessRosterStateCodec::parseRole);
    return new AccessRosterState(search, filter, role);
  }

  private static Optional<AccessFilter> parseFilter(String value) {
    for (AccessFilter filter : AccessFilter.values()) {
      if (filter.name().equalsIgnoreCase(value)) {
        return Optional.of(filter);
      }
    }
    return Optional.empty();
  }

  private static Optional<ProjectRole> parseRole(String value) {
    for (ProjectRole role : ProjectRole.values()) {
      if (role.name().equalsIgnoreCase(value)) {
        return Optional.of(role);
      }
    }
    return Optional.empty();
  }

  /**
   * Serialises a roster state into query parameters. Default values (empty search, ALL filter,
   * no role) are omitted from the URL so it stays clean for the common case.
   */
  public static QueryParameters toQueryParameters(AccessRosterState state) {
    Map<String, List<String>> parameters = new LinkedHashMap<>();
    if (!state.search().isBlank()) {
      parameters.put(SEARCH_PARAMETER, List.of(state.search().trim()));
    }
    if (state.filter() != AccessFilter.ALL) {
      parameters.put(TYPE_FILTER_PARAMETER, List.of(state.filter().name().toLowerCase()));
    }
    state.role().ifPresent(role ->
        parameters.put(ROLE_FILTER_PARAMETER, List.of(role.name().toLowerCase())));
    return new QueryParameters(parameters);
  }
}