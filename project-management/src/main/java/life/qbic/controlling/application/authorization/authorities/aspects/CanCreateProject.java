package life.qbic.controlling.application.authorization.authorities.aspects;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Limits the activity to users with authority to create a project
 */
@PreAuthorize("hasAuthority('project:create')")
public @interface CanCreateProject {

}
