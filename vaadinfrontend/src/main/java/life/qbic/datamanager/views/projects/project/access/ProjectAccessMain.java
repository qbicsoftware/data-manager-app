package life.qbic.datamanager.views.projects.project.access;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.stream.Collectors;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authorization.ProjectPermissionService;
import life.qbic.authorization.security.QbicUserDetails;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

@Route(value = "projects/:projectId?/access", layout = MainLayout.class)
@AnonymousAllowed
public class ProjectAccessMain extends Div implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final ProjectPermissionService projectPermissionService;
  private static final Logger log = logger(ProjectAccessMain.class);

  protected ProjectAccessMain(@Autowired ProjectPermissionService projectPermissionService) {
    this.projectPermissionService = projectPermissionService;
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    ProjectId projectId = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .map(ProjectId::parse).orElseThrow();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof QbicUserDetails) {
      UserId userId = ((QbicUserDetails) authentication.getPrincipal()).getUserId();
      loadInformationForProjectIdUserId(projectId, userId);
    }
  }

  private void loadInformationForProjectIdUserId(ProjectId projectId, UserId userId) {
    log.info(projectPermissionService.loadUserPermissions(userId, projectId).stream().map(
            GrantedAuthority::getAuthority).filter(it -> it.startsWith("ROLE_"))
        .collect(Collectors.joining()));
  }

}
