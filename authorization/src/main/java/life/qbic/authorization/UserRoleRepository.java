package life.qbic.authorization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Service
public interface UserRoleRepository extends JpaRepository<UserRole, String> {

}
