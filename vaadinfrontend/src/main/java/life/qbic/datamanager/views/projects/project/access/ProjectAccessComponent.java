package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.authentication.persistence.SidRepository;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.service.AccessDomainService;
import life.qbic.user.api.UserInfo;
import life.qbic.user.api.UserInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * A component that allows to manage project access
 */

@Route(value = "projects/:projectId?/access", layout = ProjectMainLayout.class)
@PermitAll
public class ProjectAccessComponent extends PageArea implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final ProjectAccessService projectAccessService;
  private final UserDetailsService userDetailsService;
  private final SidRepository sidRepository;
  private final UserInformationService userInformationService;

  private final UserPermissions userPermissions;
  private static final Logger log = logger(ProjectAccessComponent.class);
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span buttonBar = new Span();
  private final Span titleField = new Span();
  private final Grid<UserProjectAccess> userProjectAccessGrid = new Grid<>(UserProjectAccess.class);
  private final Grid<RoleProjectAccess> roleProjectAccessGrid = new Grid<>(RoleProjectAccess.class);
  private ProjectId projectId;

  private final AccessDomainService accessDomainService;

  protected ProjectAccessComponent(
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserDetailsService userDetailsService,
      @Autowired UserInformationService userInformationService,
      @Autowired SidRepository sidRepository,
      @Autowired UserPermissions userPermissions,
      @Autowired AccessDomainService accessDomainService) {
    this.projectAccessService = projectAccessService;
    this.userDetailsService = userDetailsService;
    this.userInformationService = userInformationService;
    this.sidRepository = sidRepository;
    this.userPermissions = userPermissions;
    this.accessDomainService = accessDomainService;
    requireNonNull(projectAccessService, "projectAccessService must not be null");
    requireNonNull(userDetailsService, "userDetailsService must not be null");
    requireNonNull(userInformationService, "userRepository must not be null");
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
    layoutUserProjectAccessGrid();
    layoutRoleProjectAccessGrid();
    add(content);
  }

  private void layoutUserProjectAccessGrid() {
    Span userProjectAccessDescription = new Span("Users with access to this project");
    content.add(userProjectAccessDescription);
    userProjectAccessGrid.addColumn(UserProjectAccess::fullName).setHeader("User Name");
    userProjectAccessGrid.addColumn(UserProjectAccess::userName).setHeader("Email Address");
    userProjectAccessGrid.addColumn(UserProjectAccess::projectRole).setHeader("User Role");
    content.add(userProjectAccessGrid);
  }

  private void layoutRoleProjectAccessGrid() {
    Span roleProjectAccessDescription = new Span("Roles with access to this project");
    content.add(roleProjectAccessDescription);
    roleProjectAccessGrid.addColumn(RoleProjectAccess::projectRole).setHeader("User Role");
    content.add(roleProjectAccessGrid);
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    projectId = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .map(life.qbic.projectmanagement.domain.project.ProjectId::parse).orElseThrow();
    if (userPermissions.changeProjectAccess(projectId)) {
      loadInformationForProject(projectId);
    } else {
      event.rerouteToError(NotFoundException.class);
    }
  }

  private List<String> getProjectRoles(List<String> projectRoles, QbicUserDetails userDetails) {
    List<String> roles = new ArrayList<>();
    for (String projectRole : projectRoles) {
      if (userDetails.hasAuthority(projectRole)) {
        roles.add(projectRole);
      }
    }
    return roles.stream().sorted().toList();
  }

  private void loadInformationForProject(ProjectId projectId) {
    loadProjectAccessibleUsers(projectId);
    loadProjectAccessibleRoles(projectId);
  }

  //shows active users in the UI
  private void loadProjectAccessibleUsers(ProjectId projectId) {
    List<String> userIds = projectAccessService.listActiveUserIds(projectId);
    List<QbicUserDetails> users = new ArrayList<>();
    for(String id : userIds) {
      Optional<UserInfo> optionalInfo = userInformationService.findById(id);
      if(optionalInfo.isPresent()) {
        QbicUserDetails userDetails = (QbicUserDetails) userDetailsService.
            loadUserByUsername(optionalInfo.get().emailAddress());
        users.add(userDetails);
      }
    }
    List<String> authorities = projectAccessService.listAuthorities(projectId).stream().distinct()
        .toList();
    var entries = users.stream().map(userDetail -> {
      var roles = getProjectRoles(authorities, userDetail);
      roles = roles.stream()
          .map(this::formatAuthorityToReadableString)
          .toList();
      String fullName = userInformationService.findById(userDetail.getUserId()).get().fullName();
      return new UserProjectAccess(fullName, userDetail.getUsername(), String.join(", ", roles));
    }).toList();
    List<UserProjectAccess> userProjectAccesses = new ArrayList<>(entries);
    setUserProjectAccessGridData(userProjectAccesses.stream().distinct().collect(Collectors.toList()));
  }

  private String formatAuthorityToReadableString(String authority) {
    return authority.replaceFirst("ROLE_", "")
        .replaceAll("_", " ")
        .toLowerCase();
  }

  private void setUserProjectAccessGridData(List<UserProjectAccess> userProjectAccesses) {
    userProjectAccessGrid.setItems(userProjectAccesses);
  }

  private void loadProjectAccessibleRoles(ProjectId projectId) {
    List<String> authorities = projectAccessService.listAuthoritiesForPermission(projectId,
            BasePermission.READ).stream()
        .distinct()
        .toList();
    List<RoleProjectAccess> roleProjectAccesses = authorities.stream()
        .map(this::formatAuthorityToReadableString).map(RoleProjectAccess::new).toList();
    setRoleProjectAccessGridData(roleProjectAccesses);
  }

  private void setRoleProjectAccessGridData(List<RoleProjectAccess> roleProjectAccesses) {
    roleProjectAccessGrid.setItems(roleProjectAccesses);
  }

  private void openEditUserAccessToProjectDialog() {
    EditUserAccessToProjectDialog editUserAccessToProjectDialog = new EditUserAccessToProjectDialog(
        projectAccessService,
        projectId,
        sidRepository, userInformationService);
    addEditUserAccessToProjectDialogListeners(editUserAccessToProjectDialog);
    editUserAccessToProjectDialog.open();
  }

  private void addEditUserAccessToProjectDialogListeners(
      EditUserAccessToProjectDialog editUserAccessToProjectDialog) {
    editUserAccessToProjectDialog.addCancelEventListener(
        addUserToProjectDialogCancelEvent -> editUserAccessToProjectDialog.close());
    editUserAccessToProjectDialog.addConfirmEventListener(addUserToProjectDialogConfirmEvent -> {
      List<UserInfo> addedUsers = editUserAccessToProjectDialog.getUserSelectionContent().addedUsers()
          .stream()
          .toList();
      List<UserInfo> removedUsers = editUserAccessToProjectDialog.getUserSelectionContent()
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

  private void addUsersToProject(List<UserInfo> users) {
    for (UserInfo user : users) {
      projectAccessService.grant(user.emailAddress(), projectId, BasePermission.READ);
      accessDomainService.grantProjectAccessFor(projectId.value(), user.id());
    }
  }

  private void removeUsersFromProject(List<UserInfo> users) {
    for (UserInfo user : users) {
      projectAccessService.denyAll(user.emailAddress(), projectId);
    }
  }

  private record UserProjectAccess(String fullName, String userName, String projectRole) {

  }

  private record RoleProjectAccess(String projectRole) {

  }

}
