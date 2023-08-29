package life.qbic.authorization.authorities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import life.qbic.authentication.domain.user.concept.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Provides granted authorities given a user
 */
@Service
public class QBiCUserAuthorityService implements UserAuthorityProvider {

  private final UserRoleRepository userRoleRepository;

  public QBiCUserAuthorityService(
      @Autowired UserRoleRepository userRoleRepository
  ) {
    this.userRoleRepository = userRoleRepository;
  }

  @Override
  public List<GrantedAuthority> getAuthoritiesByUserId(UserId userId) {
    List<UserRole> roles = userRoleRepository.findAllByUserId(userId.get());
    return roles.stream().flatMap(this::getAuthoritiesForRole).toList();
  }

  private Stream<GrantedAuthority> getAuthoritiesForRole(UserRole userRole) {
    List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
    Role role = userRole.role();
    grantedAuthorities.add(role);
    grantedAuthorities.addAll(role.permissions());
    return grantedAuthorities.stream();
  }
}
