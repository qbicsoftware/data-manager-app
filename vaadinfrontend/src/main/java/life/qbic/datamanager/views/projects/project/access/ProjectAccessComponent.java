package life.qbic.datamanager.views.projects.project.access;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authentication.domain.user.repository.UserInformationService;
import life.qbic.authorization.ProjectPermissionService;
import life.qbic.authorization.security.QbicUserDetails;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.general.PageArea;
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
public class ProjectAccessComponent extends PageArea implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final ProjectPermissionService projectPermissionService;
  private final transient UserInformationService userInformationService;
  private static final Logger log = logger(ProjectAccessComponent.class);
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span buttonBar = new Span();
  private final Span titleField = new Span();
  private final Grid<UserProjectRole> projectRoleGrid = new Grid<>(UserProjectRole.class);


  protected ProjectAccessComponent(@Autowired ProjectPermissionService projectPermissionService,
      @Autowired UserInformationService userInformationService) {
    Objects.requireNonNull(projectPermissionService);
    Objects.requireNonNull(userInformationService);
    this.projectPermissionService = projectPermissionService;
    this.userInformationService = userInformationService;
    layoutComponent();
    this.addClassName("project-access-component");
    log.debug(String.format(
        "New instance for ProjectAccessComponent(#%s)",
        System.identityHashCode(this)));
  }

  private void layoutComponent() {
    initHeader();
    initContent();
  }

  private void initHeader() {
    header.addClassName("access-header");
    titleField.setText("Project Access Management");
    titleField.addClassName("title");
    initButtonBar();
    header.add(titleField, buttonBar);
    add(header);
  }

  private void initButtonBar() {
    Button addButton = new Button("Add");
    addButton.addClickListener(event -> openAddUserToProjectDialog());
    addButton.addClassName("primary");
    buttonBar.add(addButton);
  }

  private void initContent() {
    content.addClassName("access-content");
    layoutProjectUserRoleGrid();
    content.add(projectRoleGrid);
    add(content);
  }

  private void layoutProjectUserRoleGrid() {
    projectRoleGrid.addColumn(UserProjectRole::userName).setHeader("User Name");
    projectRoleGrid.addColumn(UserProjectRole::projectRole).setHeader("User Role");
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
    User user = userInformationService.findById(userId).orElseThrow();
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

  private void openAddUserToProjectDialog() {
    AddUserToProjectDialog addUserToProjectDialog = new AddUserToProjectDialog(
        userInformationService);
    addAddUserToProjectDialogListeners(addUserToProjectDialog);
    addUserToProjectDialog.open();
  }

  private void addAddUserToProjectDialogListeners(AddUserToProjectDialog addUserToProjectDialog) {
    addUserToProjectDialog.addCancelEventListener(
        addUserToProjectDialogCancelEvent -> addUserToProjectDialog.close());
    addUserToProjectDialog.addConfirmEventListener(addUserToProjectDialogConfirmEvent -> {
      List<User> selectedUsers = addUserToProjectDialog.getSelectedUsers().stream().toList();
      if (!selectedUsers.isEmpty()) {
        addUsersToProject(addUserToProjectDialog.getSelectedUsers().stream().toList());
      }
      addUserToProjectDialog.close();
    });
  }

  // FixMe add users to project
  private void addUsersToProject(List<User> users) {
    users.forEach(user -> System.out.println(user.fullName().get()));
  }

  private record UserProjectRole(String userName, String projectRole) {

  }

}
