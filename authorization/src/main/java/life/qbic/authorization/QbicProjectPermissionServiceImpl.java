package life.qbic.authorization;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.projectmanagement.domain.project.ProjectId;
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
public class QbicProjectPermissionServiceImpl implements ProjectPermissionService {

  private final ProjectRoleRepository projectRoleRepository;
  private final UserRoleRepository userRoleRepository;

  public QbicProjectPermissionServiceImpl(@Autowired ProjectRoleRepository projectRoleRepository,
      @Autowired UserRoleRepository userRoleRepository) {
    this.projectRoleRepository = projectRoleRepository;
    this.userRoleRepository = userRoleRepository;
  }

  @Override
  public List<? extends GrantedAuthority> loadUserPermissions(UserId userId, ProjectId projectId) {
    Optional<ProjectRole> optionalProjectRole = projectRoleRepository.findByUserIdAndProjectId(
        userId.get(), projectId.value());
    if (optionalProjectRole.isEmpty()) {
      return new ArrayList<>();
    }
    String userRoleId = optionalProjectRole.get().userRoleId();
    Optional<UserRole> optionalRole = userRoleRepository.findById(userRoleId);
    if (optionalRole.isEmpty()) {
      return new ArrayList<>();
    }
    List<Permission> permissions = optionalRole.map(UserRole::permissions)
        .orElse(new ArrayList<>());
    List<GrantedAuthority> authorities = new ArrayList<>(permissions);
    authorities.add(optionalRole.get());
    return authorities;
  }

}
