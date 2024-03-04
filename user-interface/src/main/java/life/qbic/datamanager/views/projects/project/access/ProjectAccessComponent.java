package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.service.AccessDomainService;
import life.qbic.projectmanagement.infrastructure.project.access.SidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;

/**
 * Project Access Component
 * <p>
 * The access component is a {@link PageArea} component,
 * which shows the current permissions for all users and user groups within a {@link Project}.
 * <p>
 * Additionally, it provides the possibility to add or revoke project access for individual users
 * and user groups for the selected {@link Project}.
 * <p>
 */
@SpringComponent
@UIScope
public class ProjectAccessComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = 6832688939965353201L;
  private final ProjectAccessService projectAccessService;
  private final UserPermissions userPermissions;
  private final SidRepository sidRepository;
  private final UserInformationService userInformationService;
  private static final Logger log = logger(ProjectAccessMain.class);
  private final Grid<UserProjectAccess> userProjectAccessGrid;
  private final AccessDomainService accessDomainService;
  private Context context;
  private Button editButton;
  private Span controlsContainer;

  protected ProjectAccessComponent(
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserPermissions userPermissions,
      @Autowired UserInformationService userInformationService,
      @Autowired SidRepository sidRepository,
      @Autowired AccessDomainService accessDomainService) {
    this.projectAccessService = projectAccessService;
    this.userInformationService = userInformationService;
    this.sidRepository = sidRepository;
    this.accessDomainService = accessDomainService;
    requireNonNull(projectAccessService, "projectAccessService must not be null");
    requireNonNull(userInformationService, "userRepository must not be null");
    requireNonNull(sidRepository, "sidRepository must not be null");
    requireNonNull(accessDomainService, "accessDomainService must not be null");
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");

    userProjectAccessGrid = new Grid<>();

    var header = initHeader();
    var content = initContent();
    add(header, content);
    this.addClassName("project-access-component");
    log.debug(String.format(
        "New instance for ProjectAccessMain(#%s)",
        System.identityHashCode(this)));

  }

  public void setContext(Context context) {
    ProjectId projectId = context.projectId()
        .orElseThrow(() -> new ApplicationException("no project id in context " + context));
    this.context = context;
    if (!userPermissions.changeProjectAccess(projectId)) {
      removeControls();
    } else {
      addControls();
    }
    loadInformationForProject(projectId);
  }

  private void addControls() {
    controlsContainer.add(createControls());
  }

  private Span createControls() {
    var controls = new Span();
    var editButton = new Button("Edit");
    editButton.addClickListener(event -> openEditUserAccessToProjectDialog());
    controls.add(editButton);
    return controls;
  }

  private void removeControls() {
    controlsContainer.removeAll();
  }

  private Div initHeader() {
    Span titleField = new Span();
    var header = new Div();
    header.addClassName("header");
    titleField.setText("Project Access Management");
    titleField.addClassName("title");
    this.controlsContainer = new Span();
    header.add(titleField, controlsContainer);
    return header;
  }

  private Div initContent() {
    Div contentDiv = new Div();
    contentDiv.addClassName("content");
    var projectAccess = layoutUserProjectAccessGrid();
    contentDiv.add(projectAccess);
    return contentDiv;
  }

  private Div layoutUserProjectAccessGrid() {
    Span userProjectAccessDescription = new Span("Users with access to this project");
    Column<UserProjectAccess> usernameColumn = userProjectAccessGrid.addColumn(
            userProjectAccess -> userProjectAccess.userInfo().userName())
        .setHeader("username");
    Column<UserProjectAccess> projectRoleColumn = userProjectAccessGrid.addColumn(
            UserProjectAccess::projectRole)
        .setRenderer(new ComponentRenderer<>(projectAccess -> {
          if (projectAccess.projectRole() == ProjectRole.OWNER) {
            return new Span("owner");
          }
          if (userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
            Select<ProjectRole> roleSelect = new Select<>();
            roleSelect.setItemLabelGenerator(item -> item.name().toLowerCase());
            roleSelect.setItems(
                ProjectRole.READER,
                ProjectRole.EDITOR,
                ProjectRole.ADMIN
            );
            roleSelect.setValue(projectAccess.projectRole());
            roleSelect.addValueChangeListener(
                valueChanged -> onProjectRoleSelectionChanged(projectAccess, valueChanged));
            return roleSelect;
          } else {
            return new Span(projectAccess.projectRole().name().toLowerCase());
          }
        }))
        .setHeader("role");
    userProjectAccessGrid.setMultiSort(false);
    userProjectAccessGrid.sort(
        List.of(new GridSortOrder<>(projectRoleColumn, SortDirection.DESCENDING),
            new GridSortOrder<>(usernameColumn, SortDirection.ASCENDING)));
    Div userProjectAccess = new Div(userProjectAccessDescription, userProjectAccessGrid);
    userProjectAccess.addClassName("user-access");
    return userProjectAccess;
  }

  private void onProjectRoleSelectionChanged(UserProjectAccess projectAccess,
      ComponentValueChangeEvent<Select<ProjectRole>, ProjectRole> valueChanged) {
    projectAccessService.addProjectRole(context.projectId().orElseThrow(),
        projectAccess.userInfo.id(), valueChanged.getValue());
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
  }

  private Optional<UserProjectAccess> userProjectAccessFromCollaborator(
      ProjectCollaborator collaborator) {
    Optional<UserInfo> optionalUserInfo = userInformationService.findById(collaborator.userId());
    if (optionalUserInfo.isEmpty()) {
      // user not found -> no longer in the system?
      log.warn("User %s could not be found but has access to a project".formatted(
          collaborator.userId()));
      return Optional.empty();
    }
    UserInfo userInfo = optionalUserInfo.get();
    return Optional.of(new UserProjectAccess(userInfo, collaborator.projectRole()));
  }

  //shows active users in the UI
  private void loadProjectAccessibleUsers(ProjectId projectId) {
    List<ProjectCollaborator> collaborators = projectAccessService.listCollaborators(projectId);

    List<UserProjectAccess> accessList = collaborators.stream()
        .map(this::userProjectAccessFromCollaborator)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();

    setUserProjectAccessGridData(accessList);
  }

  private String formatAuthorityToReadableString(String authority) {
    return authority.replaceFirst("ROLE_", "")
        .replaceAll("_", " ")
        .toLowerCase();
  }

  private void setUserProjectAccessGridData(List<UserProjectAccess> userProjectAccesses) {
    userProjectAccessGrid.setItems(userProjectAccesses);
  }

  private void openEditUserAccessToProjectDialog() {
    EditUserAccessToProjectDialog editUserAccessToProjectDialog = new EditUserAccessToProjectDialog(
        projectAccessService,
        context.projectId().orElseThrow(),
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
      loadInformationForProject(context.projectId().orElseThrow());
      editUserAccessToProjectDialog.close();
    });
  }

  private void addUsersToProject(List<UserInfo> users) {
    for (UserInfo user : users) {
      projectAccessService.grant(user.id(), context.projectId().orElseThrow(), BasePermission.READ);
      accessDomainService.grantProjectAccessFor(context.projectId().orElseThrow().value(),
          user.id());
    }
  }

  private void removeUsersFromProject(List<UserInfo> users) {
    for (UserInfo user : users) {
      projectAccessService.denyAll(user.id(), context.projectId().orElseThrow());
    }
  }

  record UserProjectAccess(UserInfo userInfo, ProjectRole projectRole) {

  }

}
