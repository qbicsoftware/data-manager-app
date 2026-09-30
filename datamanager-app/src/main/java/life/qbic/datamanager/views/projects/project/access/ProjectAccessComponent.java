package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.access.AddCollaboratorToProjectDialog.ConfirmEvent;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.SharedProjectGroup;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.api.GroupSidProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Project Access Component
 * <p>
 * The access component is a {@link PageArea} component, which shows the current permissions for all
 * users and user groups within a {@link Project}.
 * <p>
 * Additionally, it provides the possibility to add or revoke project access for individual users
 * and user groups for the selected {@link Project}.
 * <p>
 */
@SpringComponent
@UIScope
public class ProjectAccessComponent extends PageArea {

  private static final Logger log = logger(ProjectAccessMain.class);
  @Serial
  private static final long serialVersionUID = 6832688939965353201L;
  public static final String INVALID_USER_REMOVAL = "Invalid user removal";
  public static final String INVALID_ROLE_EDIT = "Invalid role edit";

  private final transient ProjectAccessService projectAccessService;
  private final transient UserInformationService userInformationService;
  private final transient GroupInformationService groupInformationService;
  private final transient UserPermissions userPermissions;
  private final Grid<ProjectUser> projectUserGrid;
  private final Grid<ProjectGroup> projectGroupGrid;
  private final Div header;
  private final Span buttonBar;
  private final transient AuthenticationToUserIdTranslator authenticationToUserIdTranslator;
  private Context context;

  protected ProjectAccessComponent(
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserInformationService userInformationService,
      @Autowired GroupInformationService groupInformationService,
      UserPermissions userPermissions,
      AuthenticationToUserIdTranslator authenticationToUserIdTranslator) {
    this.projectAccessService = requireNonNull(projectAccessService,
        "projectAccessService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.groupInformationService = requireNonNull(groupInformationService,
        "groupInformationService must not be null");
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");
    this.authenticationToUserIdTranslator = requireNonNull(authenticationToUserIdTranslator,
        "authenticationToUserIdTranslator must not be null");
    this.addClassName("project-access-component");
    log.debug("New instance for %s(#%d)".formatted(ProjectAccessComponent.class.getSimpleName(),
        System.identityHashCode(this)));
    header = new Div();
    header.addClassName("header");
    Span titleField = new Span();
    titleField.setText("Project Access Management");
    titleField.addClassName("title");
    buttonBar = new Span();
    Button addCollaboratorButton = new Button("Add people or groups");
    addCollaboratorButton.addClickListener(event -> openAddCollaboratorDialog());
    buttonBar.add(addCollaboratorButton);
    header.add(titleField);
    add(header);
    Span userProjectAccessDescription = new Span("Users with access to this project.");
    projectUserGrid = createProjectUserGrid();
    add(userProjectAccessDescription, projectUserGrid);
    Span groupProjectAccessDescription = new Span("Groups with access to this project.");
    projectGroupGrid = createProjectGroupGrid();
    add(groupProjectAccessDescription, projectGroupGrid);
  }

  private static UserInfoComponent renderUserInfo(ProjectUser projectUser) {
    UserAvatar userAvatar = new UserAvatar();
    userAvatar.setUserId(projectUser.userId());
    userAvatar.setName(projectUser.userName());
    UserInfoComponent userInfoCellComponent = new UserInfoComponent(userAvatar,
        projectUser.userName, projectUser.fullName);
    userInfoCellComponent.setOidc(projectUser.oidcIssuer, projectUser.oidc);
    return userInfoCellComponent;
  }

  public void setContext(Context context) {
    if (context.projectId().isEmpty()) {
      throw new ApplicationException("no project id in context " + context);
    }
    this.context = context;
    setProjectInformation();
  }

  private void setProjectInformation() {
    refreshProjectUserGrid();
    refreshProjectGroupGrid();
    showControls(userPermissions.changeProjectAccess(context.projectId().orElseThrow()));
  }

  private void refreshProjectUserGrid() {
    loadProjectUsers();
    projectUserGrid.setItems(loadProjectUsers());
  }

  private boolean isCurrentUser(ProjectUser projectUser) {
    var userId = this.authenticationToUserIdTranslator.translateToUserId(
        SecurityContextHolder.getContext().getAuthentication()).orElseThrow();
    return Objects.equals(projectUser.userId(), userId);
  }

  private void showControls(boolean isVisible) {
    boolean containsButtonBar = header.getChildren()
        .anyMatch(component -> component.equals(buttonBar));
    if (isVisible) {
      if (!containsButtonBar) {
        header.add(buttonBar);
      }
    } else {
      if (containsButtonBar) {
        header.remove(buttonBar);
      }
    }
  }

  private Grid<ProjectUser> createProjectUserGrid() {
    Grid<ProjectUser> pUserGrid = new Grid<>(ProjectUser.class, false);
    Editor<ProjectUser> editor = pUserGrid.getEditor();
    Binder<ProjectUser> binder = new Binder<>(ProjectUser.class);
    editor.setBinder(binder);
    var projectUserInfoColumn = pUserGrid.addComponentColumn(
            ProjectAccessComponent::renderUserInfo)
        .setKey("userinfo")
        .setHeader("User Info")
        .setAutoWidth(true)
        .setSortable(true)
        .setComparator(ProjectUser::userName)
        .setResizable(true);
    var projectRoleColumn = pUserGrid.addColumn(
            collaborator -> "Role: " + collaborator.projectRole().label())
        .setKey("projectRole").setHeader("Role")
        .setEditorComponent(
            this::renderProjectRoleComponent)
        .setSortable(true)
        .setComparator(projectUser -> projectUser.projectRole().label())
        .setResizable(true)
        .setAutoWidth(true);
    pUserGrid.addComponentColumn(projectUser -> {
          //You can't remove or edit your own role
          if (isCurrentUser(projectUser)) {
            return new Span();
          }
          //You can't remove or edit the project owner
          if (projectUser.projectRole() == ProjectRole.OWNER) {
            return new Span();
          }
          //You don't have the rights to change the user
          if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
            return new Span();
          }
          return changeProjectAccessCell(projectUser);
        })
        .setHeader("Action")
        .setAutoWidth(true);
    pUserGrid.sort(
        List.of(new GridSortOrder<>(projectUserInfoColumn, SortDirection.DESCENDING),
            new GridSortOrder<>(projectRoleColumn, SortDirection.DESCENDING)));
    pUserGrid.setSelectionMode(SelectionMode.NONE);
    pUserGrid.setColumnReorderingAllowed(true);
    return pUserGrid;
  }

  private Span changeProjectAccessCell(ProjectUser projectUser) {
    Span changeProjectAccessCell = new Span();
    //We want to ensure that even if the frontend components are shown no event is propagated
    // if the user doesn't have the correct role or tries to remove himself/the project owner
    Button removeButton = new Button("Remove", clickEvent -> {
      if (isCurrentUser(projectUser)) {
        displayError(INVALID_USER_REMOVAL, "You can't remove yourself from a project");
        return;
      }
      if (projectUser.projectRole() == ProjectRole.OWNER) {
        displayError(INVALID_USER_REMOVAL, "You can't remove the owner of a project");
        return;
      }
      if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
        displayError(INVALID_USER_REMOVAL,
            "You don't have permission to remove the user from this project");
        return;
      }
      AlertDialog.danger(this,
          "Remove user from project",
          "Are you sure you want to remove the user %s from the project?".formatted(
              projectUser.userName()),
          "Remove user",
          "Keep user",
          () -> removeCollaborator(projectUser)).open();
    });
    //We want to ensure that even if the frontend components are shown no event is propagated
    // if the user doesn't have the correct role or tries to edit himself/the project owner
    Button editButton = new Button("Edit", clickEvent -> {
      if (isCurrentUser(projectUser)) {
        displayError(INVALID_ROLE_EDIT, "You can't change your own project role");
        return;
      }
      if (projectUser.projectRole() == ProjectRole.OWNER) {
        displayError(INVALID_ROLE_EDIT, "You can't change the owner of this project");
        return;
      }
      if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
        displayError(INVALID_ROLE_EDIT,
            "You don't have permission to change the role of this collaborator");
        return;
      }
      if (projectUserGrid.getEditor().isOpen()) {
        projectUserGrid.getEditor().cancel();
        projectUserGrid.getEditor().closeEditor();
        return;
      }
      projectUserGrid.getEditor().editItem(projectUser);
    });
    changeProjectAccessCell.add(editButton, removeButton);
    changeProjectAccessCell.addClassName("change-project-access-cell");
    return changeProjectAccessCell;
  }

  private List<ProjectUser> loadProjectUsers() {
    var projectCollaborators = projectAccessService.listCollaborators(
        context.projectId().orElseThrow());
    return projectCollaborators.stream().map(collaborator ->
    {
      var userInfo = userInformationService.findById(collaborator.userId()).orElseThrow();
      var oidcId = "";
      var oidcIssuer = "";
      if (userInfo.oidcId() != null) {
        oidcId = userInfo.oidcId();
      }
      if (userInfo.oidcIssuer() != null) {
        oidcIssuer = userInfo.oidcIssuer();
      }
      return new ProjectUser(collaborator.userId(), userInfo.platformUserName(),
          userInfo.fullName(), oidcId, oidcIssuer, collaborator.projectRole());
    }).toList();
  }

  private Component renderProjectRoleComponent(
      ProjectUser projectUser) {
    String labelPrefix = "Role: ";
    Select<ProjectRole> roleSelect = new Select<>();
    roleSelect.addClassName("project-role-select");
    roleSelect.setItemLabelGenerator(ProjectRole::label);
    roleSelect.setPrefixComponent(new Span(labelPrefix));
    roleSelect.setItems(
        ProjectRole.READ,
        ProjectRole.WRITE,
        ProjectRole.ADMIN
    );
    roleSelect.setRenderer(new ComponentRenderer<>(
        projectRole -> {
          Span roleLabel = new Span(projectRole.label());
          roleLabel.addClassName("project-role-label");

          Span roleDescription = new Span(projectRole.description());
          roleDescription.addClassName("project-role-description");

          Div projectRoleDiv = new Div();
          projectRoleDiv.addClassName("project-role-item");
          projectRoleDiv.add(roleLabel, roleDescription);
          return projectRoleDiv;
        }));

    roleSelect.setValue(projectUser.projectRole());
    roleSelect.addValueChangeListener(valueChanged -> {
      onProjectRoleSelectionChanged(projectUser, valueChanged);
      projectUserGrid.getEditor().save();
      projectUserGrid.getEditor().closeEditor();
      refreshProjectUserGrid();
    });
    return roleSelect;
  }

  private void removeCollaborator(ProjectUser projectUser) {
    ProjectId projectId = context.projectId().orElseThrow();
    projectAccessService.removeCollaborator(projectId, projectUser.userId());
    refreshProjectUserGrid();
  }

  private void changeGroupRole(ProjectGroup projectGroup, ProjectRole projectRole) {
    ProjectId projectId = context.projectId().orElseThrow();
    try {
      projectAccessService.changeAuthorityAccess(projectId,
          GroupSidProvider.GROUP_SID_PREFIX + projectGroup.groupId(), projectRole);
    } catch (ApplicationException e) {
      displayError(INVALID_ROLE_EDIT,
          "You don't have permission to change the role of this group");
      return;
    }
    refreshProjectGroupGrid();
  }

  private void revokeGroup(ProjectGroup projectGroup) {
    ProjectId projectId = context.projectId().orElseThrow();
    try {
      projectAccessService.removeAuthorityAccess(projectId,
          GroupSidProvider.GROUP_SID_PREFIX + projectGroup.groupId());
    } catch (ApplicationException e) {
      displayError(INVALID_USER_REMOVAL, "You can't remove this group from the project");
      return;
    }
    refreshProjectGroupGrid();
  }

  private void onProjectRoleSelectionChanged(ProjectUser projectUser,
      ComponentValueChangeEvent<Select<ProjectRole>, ProjectRole> valueChanged) {
    projectAccessService.changeRole(context.projectId().orElseThrow(), projectUser.userId(),
        valueChanged.getValue());
  }

  private Grid<ProjectGroup> createProjectGroupGrid() {
    Grid<ProjectGroup> groupGrid = new Grid<>(ProjectGroup.class, false);
    Editor<ProjectGroup> editor = groupGrid.getEditor();
    Binder<ProjectGroup> binder = new Binder<>(ProjectGroup.class);
    editor.setBinder(binder);
    groupGrid.addColumn(ProjectGroup::groupName)
        .setKey("groupName")
        .setHeader("Group")
        .setAutoWidth(true)
        .setSortable(true)
        .setSortProperty("groupName")
        .setResizable(true);
    groupGrid.addColumn(ProjectGroup::groupDescription)
        .setKey("groupDescription")
        .setHeader("Description")
        .setWidth("28em")
        .setFlexGrow(1)
        .setResizable(true);
    groupGrid.setPartNameGenerator(projectGroup -> "group-description-row");
    var projectRoleColumn = groupGrid.addColumn(projectGroup -> "Role: " + projectGroup.projectRole().label())
        .setKey("projectRole")
        .setHeader("Role")
        .setEditorComponent(this::renderProjectGroupRoleComponent)
        .setAutoWidth(true)
        .setSortable(true)
        .setResizable(true);
    groupGrid.addComponentColumn(projectGroup -> {
          //You don't have the rights to change the group grants
          if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
            return new Span();
          }
          return changeProjectGroupAccessCell(projectGroup);
        })
        .setKey("action")
        .setHeader("Action")
        .setAutoWidth(true);
    groupGrid.setSelectionMode(Grid.SelectionMode.NONE);
    groupGrid.setColumnReorderingAllowed(true);
    return groupGrid;
  }

  private Span changeProjectGroupAccessCell(ProjectGroup projectGroup) {
    Span changeProjectGroupAccessCell = new Span();
    //We want to ensure that even if the frontend components are shown no event is propagated
    // if the user doesn't have the correct role
    Button removeButton = new Button("Remove", clickEvent -> {
      if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
        displayError(INVALID_USER_REMOVAL,
            "You don't have permission to remove the group from this project");
        return;
      }
      AlertDialog.danger(this,
          "Remove group from project",
          "Are you sure you want to remove the group %s from the project?".formatted(
              projectGroup.groupName()),
          "Remove group",
          "Keep group",
          () -> revokeGroup(projectGroup)).open();
    });
    Button editButton = new Button("Edit", clickEvent -> {
      //We want to ensure that even if the frontend components are shown no event is propagated
      // if the user doesn't have the correct role
      if (!userPermissions.changeProjectAccess(context.projectId().orElseThrow())) {
        displayError(INVALID_ROLE_EDIT,
            "You don't have permission to change the role of this group");
        return;
      }
      if (projectGroupGrid.getEditor().isOpen()) {
        projectGroupGrid.getEditor().cancel();
        projectGroupGrid.getEditor().closeEditor();
        return;
      }
      projectGroupGrid.getEditor().editItem(projectGroup);
    });
    changeProjectGroupAccessCell.add(editButton, removeButton);
    changeProjectGroupAccessCell.addClassName("change-project-access-cell");
    return changeProjectGroupAccessCell;
  }

  private Component renderProjectGroupRoleComponent(
      ProjectGroup projectGroup) {
    String labelPrefix = "Role: ";
    Select<ProjectRole> roleSelect = new Select<>();
    roleSelect.addClassName("project-role-select");
    roleSelect.setItemLabelGenerator(ProjectRole::label);
    roleSelect.setPrefixComponent(new Span(labelPrefix));
    roleSelect.setItems(
        ProjectRole.READ,
        ProjectRole.WRITE,
        ProjectRole.ADMIN
    );
    roleSelect.setRenderer(new ComponentRenderer<>(
        projectRole -> {
          Span roleLabel = new Span(projectRole.label());
          roleLabel.addClassName("project-role-label");

          Span roleDescription = new Span(projectRole.description());
          roleDescription.addClassName("project-role-description");

          Div projectRoleDiv = new Div();
          projectRoleDiv.addClassName("project-role-item");
          projectRoleDiv.add(roleLabel, roleDescription);
          return projectRoleDiv;
        }));

    roleSelect.setValue(projectGroup.projectRole());
    roleSelect.addValueChangeListener(valueChanged -> {
      changeGroupRole(projectGroup, valueChanged.getValue());
      projectGroupGrid.getEditor().save();
      projectGroupGrid.getEditor().closeEditor();
      refreshProjectGroupGrid();
    });
    return roleSelect;
  }

  private void refreshProjectGroupGrid() {
    List<SharedProjectGroup> sharedGroups = projectAccessService.listSharedGroups(
        context.projectId().orElseThrow());
    projectGroupGrid.setItems(
        sharedGroups.stream().map(this::toProjectGroup).toList());
  }

  private ProjectGroup toProjectGroup(SharedProjectGroup sharedProjectGroup) {
    return new ProjectGroup(sharedProjectGroup.groupId(), sharedProjectGroup.groupName(),
        sharedProjectGroup.groupDescription(), sharedProjectGroup.projectRole());
  }

  private void openAddCollaboratorDialog() {
    ProjectId projectId = context.projectId().orElseThrow();
    List<ProjectCollaborator> alreadyExistingCollaborators = projectAccessService
        .listCollaborators(projectId);
    List<SharedProjectGroup> alreadySharedGroups = projectAccessService.listSharedGroups(projectId);
    AddCollaboratorToProjectDialog addCollaboratorToProjectDialog = new AddCollaboratorToProjectDialog(
        userInformationService, projectId, alreadyExistingCollaborators, groupInformationService,
        alreadySharedGroups);
    addCollaboratorToProjectDialog.open();
    addCollaboratorToProjectDialog.addCancelListener(event -> event.getSource().close());
    addCollaboratorToProjectDialog.addConfirmListener(this::onAddCollaboratorConfirmed);
    addCollaboratorToProjectDialog.addGroupConfirmListener(this::onGroupSharedConfirmed);
  }

  private void onGroupSharedConfirmed(
      AddCollaboratorToProjectDialog.GroupConfirmEvent event) {
    ProjectId projectId = context.projectId().orElseThrow();
    try {
      projectAccessService.addAuthorityAccess(projectId,
          GroupSidProvider.GROUP_SID_PREFIX + event.groupId(),
          event.projectRole());
    } catch (ApplicationException e) {
      displayError("Invalid group sharing",
          "This group is already shared onto the project. Please change the project role instead.");
      return;
    }
    refreshProjectUserGrid();
    refreshProjectGroupGrid();
    event.getSource().close();
  }

  private void onAddCollaboratorConfirmed(ConfirmEvent event) {
    projectAccessService.addCollaborator(context.projectId().orElseThrow(),
        event.projectCollaborator()
            .userId(), event.projectCollaborator().projectRole());
    refreshProjectUserGrid();
    event.getSource().close();
  }

  private void displayError(String title, String description) {
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  /**
   * A user in a specific project.
   *
   * @param userId      the collaborating user
   * @param userName    the unique username of the user
   * @param fullName    the full name of the user
   * @param oidc        the oidc of the user
   * @param projectRole the role of the user within the project
   */
  public record ProjectUser(String userId, String userName, String fullName, String oidc,
                            String oidcIssuer, ProjectRole projectRole) {
  }

  /**
   * A user group shared onto a project.
   *
   * @param groupId          the stable user group id
   * @param groupName        the group's display name
   * @param groupDescription the group's description, may be {@code null}
   * @param projectRole      the project role granted to the group
   */
  public record ProjectGroup(String groupId, String groupName, String groupDescription,
                             ProjectRole projectRole) {
  }

  /**
   * A component displaying a users avatar, orcid, full name and username
   */
  public static class UserInfoComponent extends Div {

    private final Div userInfoContent;

    public UserInfoComponent(UserAvatar userAvatar, String userName, String fullName) {
      addClassName("user-info-component");
      userAvatar.addClassName("avatar");
      userInfoContent = new Div();
      userInfoContent.addClassName("user-info");
      add(userAvatar, userInfoContent);
      setUserNameAndFullName(userName, fullName);
    }

    private void setUserNameAndFullName(String userName, String fullName) {
      Span fullNameSpan = new Span(fullName);
      Span userNameSpan = new Span(userName);
      userNameSpan.addClassName("bold");
      Span userNameAndFullName = new Span(userNameSpan, fullNameSpan);
      userNameAndFullName.addClassName("user-name-and-full-name");
      userInfoContent.add(userNameAndFullName);
    }

    protected void setOidc(String oidcIssuer, String oidc) {
      if (oidcIssuer.isEmpty() || oidc.isEmpty()) {
        return;
      }
      Arrays.stream(OidcType.values())
          .filter(ot -> ot.getIssuer().equals(oidcIssuer))
          .findFirst()
          .ifPresentOrElse(oidcType -> addOidcInfoItem(oidcType, oidc),
              () -> log.warn("Unknown oidc Issuer %s".formatted(oidcIssuer)));
    }

    private void addOidcInfoItem(OidcType oidcType, String oidc) {
      String oidcUrl = String.format(oidcType.getUrl()) + oidc;
      Anchor oidcLink = new Anchor(oidcUrl, oidcUrl);
      oidcLink.setTarget(AnchorTarget.BLANK);
      OidcLogo oidcLogo = new OidcLogo(oidcType);
      Span oidcSpan = new Span(oidcLogo, oidcLink);
      oidcSpan.addClassNames("icon-size-m oidc");
      userInfoContent.add(oidcSpan);
    }
  }
}
