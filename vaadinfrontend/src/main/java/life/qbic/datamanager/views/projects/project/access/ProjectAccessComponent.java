package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.repository.UserInformationService;
import life.qbic.authorization.acl.ProjectAccessService;
import life.qbic.authorization.security.QbicUserDetails;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.general.PageArea;
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

@Route(value = "projects/:projectId?/access", layout = MainLayout.class)
public class ProjectAccessComponent extends PageArea implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final transient UserInformationService userInformationService;
  private final ProjectAccessService projectAccessService;
  private final UserDetailsService userDetailsService;

  private static final Logger log = logger(ProjectAccessComponent.class);
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span buttonBar = new Span();
  private final Span titleField = new Span();
  private final Grid<UserProjectRole> projectRoleGrid = new Grid<>(UserProjectRole.class);


  protected ProjectAccessComponent(
      @Autowired UserInformationService userInformationService,
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserDetailsService userDetailsService) {
    this.projectAccessService = projectAccessService;
    this.userDetailsService = userDetailsService;
    requireNonNull(
        userInformationService); //FIXME why another information service; remove user information service
    requireNonNull(projectAccessService, "projectAccessService must not be null");
    requireNonNull(userDetailsService, "userDetailsService must not be null");
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

  // FIXME: provide project
  private void addUsersToProject(List<User> users) {
    for (User user : users) {
//      projectAccessService.grant(user.emailAddress(), /* enter project id here */, BasePermission.READ);
    }
  }

  private record UserProjectRole(String userName, String projectRole) {

  }

}
