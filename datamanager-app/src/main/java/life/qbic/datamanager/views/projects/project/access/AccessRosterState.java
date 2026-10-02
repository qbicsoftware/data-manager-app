package life.qbic.datamanager.views.projects.project.access;

import java.util.Optional;
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.AccessFilter;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;

/**
 * The user-configurable view state of the project access roster: the search query, the principal
 * type filter and the optional role filter. It is mirrored into the URL query parameters so the
 * search and filter settings are preserved during natural browser navigation (see
 * {@link AccessRosterStateCodec}).
 *
 * @param search         the search query applied to principal names and descriptions
 * @param filter         the principal type filter (all / people / groups)
 * @param role           the optional role filter restricting the roster to one project role
 */
public record AccessRosterState(String search, AccessFilter filter, Optional<ProjectRole> role) {

  public AccessRosterState {
    filter = filter == null ? AccessFilter.ALL : filter;
    role = role == null ? Optional.empty() : role;
  }

  /**
   * The default roster state: no search, no type filter, no role filter.
   */
  public static AccessRosterState defaults() {
    return new AccessRosterState("", AccessFilter.ALL, Optional.empty());
  }

  public AccessRosterState withSearch(String search) {
    return new AccessRosterState(search, filter, role);
  }

  public AccessRosterState withFilter(AccessFilter filter) {
    return new AccessRosterState(search, filter, role);
  }

  public AccessRosterState withRole(Optional<ProjectRole> role) {
    return new AccessRosterState(search, filter, role);
  }
}