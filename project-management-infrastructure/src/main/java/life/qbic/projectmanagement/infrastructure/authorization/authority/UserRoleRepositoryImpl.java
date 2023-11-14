package life.qbic.projectmanagement.infrastructure.authorization.authority;

import java.util.List;
import life.qbic.projectmanagement.application.authorization.authorities.UserRole;
import life.qbic.projectmanagement.application.authorization.authorities.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>User Repository Implementation</b>
 * <p>
 * Implementation of the {@link UserRoleRepository} interface.
 *
 * @since 1.0.0
 */
@Component
public class UserRoleRepositoryImpl implements UserRoleRepository {

  private final UserRoleJpa userRoleJpa;

  @Autowired
  public UserRoleRepositoryImpl(UserRoleJpa userRoleJpa) {
    this.userRoleJpa = userRoleJpa;
  }

  @Override
  public List<UserRole> findAllByUserId(String userId) {
    return userRoleJpa.findAllByUserId(userId);
  }
}
