package life.qbic.authorization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * <b>User Role Storage Interface</b>
 * <p>
 * Provides access to the persistent user roles.
 */
@Service
public interface UserRoleRepository extends JpaRepository<UserRole, String> {

}
