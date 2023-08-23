package life.qbic.datamanager.views.projects.project.access;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.authorization.ProjectPermissionService;
import life.qbic.authorization.security.QbicUserDetails;
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

@AnonymousAllowed
public class ProjectAccessMain extends Div implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final ProjectPermissionService projectPermissionService;

  //FixMe implement UserInformationService
  private final UserRepository userRepository;
  private static final Logger log = logger(ProjectAccessMain.class);
  private Grid<UserProjectRole> projectRoleGrid = new Grid<>();

  protected ProjectAccessMain(@Autowired ProjectPermissionService projectPermissionService,
      @Autowired UserRepository userRepository) {
    Objects.requireNonNull(projectPermissionService);
    Objects.requireNonNull(userRepository);
    this.projectPermissionService = projectPermissionService;
    this.userRepository = userRepository;
    layoutComponent();
    log.debug(String.format(
        "New instance for ProjectAccessMain(#%s)",
        System.identityHashCode(this)));
  }

  private void layoutComponent() {
    projectRoleGrid = createUserProjectRoleGrid();
    add(projectRoleGrid);
  }

  private Grid<UserProjectRole> createUserProjectRoleGrid() {
    Grid<UserProjectRole> projectRoleGrid = new Grid<>(UserProjectRole.class);
    projectRoleGrid.addColumn(UserProjectRole::userName).setHeader("User Name");
    projectRoleGrid.addColumn(UserProjectRole::projectRole).setHeader("User Role");
    return projectRoleGrid;
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
    List<UserProjectRole> userProjectRoleList = new ArrayList<>();
    projectPermissionService.loadUsersWithProjectPermission(projectId)
        .forEach(uId -> userProjectRoleList.add(
            new UserProjectRole(loadUserName(uId), loadUserRole(projectId, uId))));
    setGridData(userProjectRoleList);
  }

  private String loadUserName(UserId userId) {
    //FixMe This should be handled by UserInformationService
    User user = userRepository.findById(userId).orElseThrow();
    return user.fullName().get();
  }

  private String loadUserRole(ProjectId projectId, UserId userId) {
    //ToDo Clean up String extraction
    return projectPermissionService.loadUserPermissions(userId, projectId).stream().map(
            GrantedAuthority::getAuthority).filter(it -> it.startsWith("ROLE_"))
        .map(s -> s.substring(5))
        .collect(Collectors.joining());
  }

  private void setGridData(List<UserProjectRole> userProjectRoles) {
    projectRoleGrid.setItems(userProjectRoles);
  }

  private record UserProjectRole(String userName, String projectRole) {

  }

}
