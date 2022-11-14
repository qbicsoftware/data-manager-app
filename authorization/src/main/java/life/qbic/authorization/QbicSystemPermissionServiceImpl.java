package life.qbic.authorization;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.authentication.domain.user.concept.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Service
public class QbicSystemPermissionServiceImpl implements SystemPermissionService {

  private final SystemRoleRepository systemRoleRepository;
  private final UserRoleRepository userRoleRepository;

  public QbicSystemPermissionServiceImpl(@Autowired UserRoleRepository userRoleRepository,
      @Autowired SystemRoleRepository systemRoleRepository) {
    this.systemRoleRepository = systemRoleRepository;
    this.userRoleRepository = userRoleRepository;
  }

  @Override
  public List<GrantedAuthority> loadUserPermissions(UserId userId) {
    List<SystemRole> rolesOfGivenUser = systemRoleRepository.findAllByUserId(userId.get());
    List<GrantedAuthority> authorities = new ArrayList<>();
    for (SystemRole systemRole : rolesOfGivenUser) {
      Optional<UserRole> optionalUserRole = userRoleRepository.findById(systemRole.userRoleId());
      optionalUserRole.ifPresent(role -> {
        authorities.add(role);
        authorities.addAll(role.permissions());
      });
    }
    return authorities;
  }
}
