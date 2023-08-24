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
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.authentication.persistence.SidRepository;
import life.qbic.authorization.acl.ProjectAccessService;
import life.qbic.authorization.security.QbicUserDetails;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
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
@PermitAll
public class ProjectAccessComponent extends PageArea implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final ProjectAccessService projectAccessService;
  private final UserDetailsService userDetailsService;
  private final SidRepository sidRepository;
  private final UserRepository userRepository;
  private static final Logger log = logger(ProjectAccessComponent.class);
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span buttonBar = new Span();
  private final Span titleField = new Span();
  private final Grid<UserProjectRole> projectRoleGrid = new Grid<>(UserProjectRole.class);
  private ProjectId projectId;

  protected ProjectAccessComponent(
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserDetailsService userDetailsService,
      @Autowired UserRepository userRepository,
      @Autowired SidRepository sidRepository) {
    this.projectAccessService = projectAccessService;
    this.userDetailsService = userDetailsService;
    this.userRepository = userRepository;
    this.sidRepository = sidRepository;
    requireNonNull(projectAccessService, "projectAccessService must not be null");
    requireNonNull(userDetailsService, "userDetailsService must not be null");
    requireNonNull(userRepository, "userRepository must not be null");
    requireNonNull(sidRepository, "sidRepository must not be null");
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
    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> openEditUserAccessToProjectDialog());
    buttonBar.add(editButton);
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
    projectId = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
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

  private void openEditUserAccessToProjectDialog() {
    editUserAccessToProjectDialog editUserAccessToProjecTDialog = new editUserAccessToProjectDialog(
        projectAccessService,
        projectId,
        sidRepository, userRepository);
    addEditUserAccessToProjectDialogListeners(editUserAccessToProjecTDialog);
    editUserAccessToProjecTDialog.open();
  }

  private void addEditUserAccessToProjectDialogListeners(
      editUserAccessToProjectDialog editUserAccessToProjectDialog) {
    editUserAccessToProjectDialog.addCancelEventListener(
        addUserToProjectDialogCancelEvent -> editUserAccessToProjectDialog.close());
    editUserAccessToProjectDialog.addConfirmEventListener(addUserToProjectDialogConfirmEvent -> {
      List<User> addedUsers = editUserAccessToProjectDialog.getUserSelectionContent().addedUsers()
          .stream()
          .toList();
      List<User> removedUsers = editUserAccessToProjectDialog.getUserSelectionContent()
          .removedUsers()
          .stream().toList();
      if (!addedUsers.isEmpty()) {
        addUsersToProject(addedUsers);
      }
      if (!removedUsers.isEmpty()) {
        removeUsersFromProject(removedUsers);
      }
      loadInformationForProject(projectId);
      editUserAccessToProjectDialog.close();
    });
  }

  private void addUsersToProject(List<User> users) {
    for (User user : users) {
      projectAccessService.grant(user.emailAddress().get(), projectId, BasePermission.READ);
    }
  }

  private void removeUsersFromProject(List<User> users) {
    for (User user : users) {
      projectAccessService.deny(user.emailAddress().get(), projectId, BasePermission.READ);
    }
  }

  private record UserProjectRole(String userName, String projectRole) {

  }

}
