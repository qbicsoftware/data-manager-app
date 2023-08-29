package life.qbic.authorization.acl;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.core.Authentication;

/**
 * <b>QBiC's implementation of the Spring PermissionEvaluator interface</b>
 * <p>
 * This class shall be used to check if the current user has the permission to have access to the
 * targetDomainObject of interest in the context of user authorization
 */
public class QbicPermissionEvaluator extends AclPermissionEvaluator {

  public QbicPermissionEvaluator(@Autowired AclService aclService) {
    super(aclService);
  }

  private static boolean isEmptyOptional(Object object) {
    if (object instanceof Optional<?>) {
      return ((Optional<?>) object).isEmpty();
    }
    return false;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject,
      Object permission) {
    if (isEmptyOptional(targetDomainObject)) {
      return true;
    }
    return super.hasPermission(authentication, targetDomainObject, permission);
  }
}
