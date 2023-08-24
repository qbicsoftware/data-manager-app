package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.authorization.acl.ProjectAccessService;
import life.qbic.authorization.security.QbicUserDetails;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

//@Route(value = "projects/:projectId?/access", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "PROJECT_MANAGER"})
public class ProjectAccessMain extends Div implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final ProjectAccessService projectAccessService;
  private final UserDetailsService userDetailsService;
  private static final Logger log = logger(ProjectAccessMain.class);
  private Grid<UserProjectRole> projectRoleGrid = new Grid<>();

  protected ProjectAccessMain(@Autowired ProjectAccessService projectAccessService,
      @Autowired UserDetailsService userDetailsService,
      @Autowired UserRepository userRepository) {
    this.userDetailsService = userDetailsService;
    requireNonNull(projectAccessService);
    requireNonNull(userRepository);
    requireNonNull(userDetailsService, "userDetailsService must not be null");
    this.projectAccessService = projectAccessService;
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
    loadInformationForProject(projectId);
  }

  private void loadInformationForProject(ProjectId projectId) {
    List<String> usernames = projectAccessService.listUsernames(projectId);
    List<QbicUserDetails> users = usernames.stream()
        .map(it -> (QbicUserDetails) userDetailsService.loadUserByUsername(it)).toList();
    List<QbicUserDetails> projectManagers = users.stream()
        .filter(it -> it.hasAuthority(new SimpleGrantedAuthority("ROLE_PROJECT_MANAGER")))
        .toList();
    List<QbicUserDetails> otherUsers = users.stream()
        .filter(it -> !projectManagers.contains(it))
        .toList();
    List<UserProjectRole> userProjectRoleList = new ArrayList<>(
        projectManagers.stream().map(it -> new UserProjectRole(it.getUsername(), "Project Manager"))
            .toList());
    userProjectRoleList.addAll(
        otherUsers.stream().map(it -> new UserProjectRole(it.getUsername(), "Viewer"))
            .toList());
    setGridData(userProjectRoleList.stream().distinct().collect(Collectors.toList()));
  }

  private void setGridData(List<UserProjectRole> userProjectRoles) {
    projectRoleGrid.setItems(userProjectRoles);
  }

  private record UserProjectRole(String userName, String projectRole) {

  }

}
