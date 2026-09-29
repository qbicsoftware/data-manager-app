package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import java.util.List;
import life.qbic.projectmanagement.application.authorization.authorities.UserAuthorityProvider;
import life.qbic.usergroups.api.GroupAdministrationPermission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * <b>QBiC group administration permission</b>
 *
 * <p>Composition-root adapter for the {@link GroupAdministrationPermission} port of the user
 * groups context. Resolves the given user's granted authorities through the existing
 * {@link UserAuthorityProvider} and checks for the system role {@code ROLE_ADMIN}.</p>
 *
 * <p>This is the single admin-gate seam for all admin-governed group surfaces (org-group
 * creation here, membership/manager management in story FEAT-USER-GROUPS-02).</p>
 *
 * @since 1.21.0
 */
@Component
public class QbicGroupAdministrationPermission implements GroupAdministrationPermission {

  private static final String ADMIN_ROLE = "ROLE_ADMIN";

  private final UserAuthorityProvider userAuthorityProvider;

  public QbicGroupAdministrationPermission(UserAuthorityProvider userAuthorityProvider) {
    this.userAuthorityProvider = requireNonNull(userAuthorityProvider,
        "userAuthorityProvider must not be null");
  }

  @Override
  public boolean isAdmin(String userId) {
    if (userId == null || userId.isBlank()) {
      return false;
    }
    List<GrantedAuthority> authorities = userAuthorityProvider.getAuthoritiesByUserId(userId);
    return authorities.stream().anyMatch(authority -> ADMIN_ROLE.equals(authority.getAuthority()));
  }
}