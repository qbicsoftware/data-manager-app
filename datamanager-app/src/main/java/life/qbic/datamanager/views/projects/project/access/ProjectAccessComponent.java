package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
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
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.GrantRequest;
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.GrantRequestedEvent;
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.PrincipalType;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRoleRecommendationRenderer;
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
 * The component is dialog-free: people and groups are listed with an always-visible role control
 * and an inline removal confirmation. Sharing happens through the inline
 * {@link ProjectSharingComposer}, which supports granting access to one or several principals in a
 * single action. Users without access-administration rights see a read-only view (group names and
 * descriptions only, never membership data).
 */
@SpringComponent
@UIScope
public class ProjectAccessComponent extends PageArea {

  private static final Logger log = logger(ProjectAccessComponent.class);
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
  private final ProjectSharingComposer composer;
  private final Div header;
  private final Span buttonBar;
  private final Button shareButton;
  private final transient AuthenticationToUserIdTranslator authenticationToUserIdTranslator;
  private Context context;
  private boolean canChangeAccess = false;
  private Div openConfirmCell;
  private Component openConfirmRestore;

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
    buttonBar.addClassName("button-bar");
    shareButton = new Button("Add people or groups");
    shareButton.addClassName("share-project-button");
    shareButton.addClickListener(event -> toggleComposer());
    buttonBar.add(shareButton);
    header.add(titleField);

    composer = new ProjectSharingComposer(userInformationService, groupInformationService);
    composer.addClassName("project-sharing-composer");
    composer.addGrantListener(this::onGrantRequested);
    composer.setVisible(false);

    Span userProjectAccessDescription = new Span("Users with access to this project.");
    projectUserGrid = createProjectUserGrid();
    Span groupProjectAccessDescription = new Span("Groups with access to this project.");
    projectGroupGrid = createProjectGroupGrid();
    add(header, composer, userProjectAccessDescription, projectUserGrid,
        groupProjectAccessDescription, projectGroupGrid);
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
    this.canChangeAccess = userPermissions.changeProjectAccess(context.projectId().orElseThrow());
    setProjectInformation();
  }

  private void setProjectInformation() {
    openConfirmCell = null;
    openConfirmRestore = null;
    composer.reset();
    refreshProjectUserGrid();
    refreshProjectGroupGrid();
    showControls(canChangeAccess);
  }

  private void refreshProjectUserGrid() {
    openConfirmCell = null;
    openConfirmRestore = null;
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
      composer.setVisible(false);
    }
  }

  private void toggleComposer() {
    if (composer.isVisible()) {
      composer.setVisible(false);
      return;
    }
    ProjectId projectId = context.projectId().orElseThrow();
    composer.setAlreadyGranted(projectAccessService.listCollaborators(projectId),
        projectAccessService.listSharedGroups(projectId));
    composer.setVisible(true);
  }

  private void onGrantRequested(GrantRequestedEvent event) {
    ProjectId projectId = context.projectId().orElseThrow();
    int granted = 0;
    for (GrantRequest request : event.requests()) {
      try {
        if (request.type() == PrincipalType.USER) {
          projectAccessService.addCollaborator(projectId, request.id(), request.role());
        } else {
          projectAccessService.addAuthorityAccess(projectId,
              GroupSidProvider.GROUP_SID_PREFIX + request.id(), request.role());
        }
        granted++;
      } catch (ApplicationException e) {
        displayError("Invalid access grant",
            "One or more selected people or groups could not be granted access. They may already "
                + "have access or the change is not permitted.");
      }
    }
    composer.reset();
    composer.setVisible(false);
    refreshProjectUserGrid();
    refreshProjectGroupGrid();
    if (granted > 0) {
      displaySuccess("Access granted", granted == 1
          ? "Access was granted to 1 principal."
          : "Access was granted to %d principals.".formatted(granted));
    }
  }

  private Grid<ProjectUser> createProjectUserGrid() {
    Grid<ProjectUser> pUserGrid = new Grid<>(ProjectUser.class, false);
    var projectUserInfoColumn = pUserGrid.addComponentColumn(
            ProjectAccessComponent::renderUserInfo)
        .setKey("userinfo")
        .setHeader("User Info")
        .setAutoWidth(true)
        .setSortable(true)
        .setComparator(ProjectUser::userName)
        .setResizable(true);
    var projectRoleColumn = pUserGrid.addComponentColumn(this::renderUserRoleComponent)
        .setKey("projectRole").setHeader("Role")
        .setSortable(true)
        .setComparator(projectUser -> projectUser.projectRole().label())
        .setResizable(true)
        .setAutoWidth(true);
    pUserGrid.addComponentColumn(this::renderUserActionCell)
        .setKey("action")
        .setHeader("Action")
        .setAutoWidth(true);
    pUserGrid.sort(
        List.of(new GridSortOrder<>(projectUserInfoColumn, SortDirection.DESCENDING),
            new GridSortOrder<>(projectRoleColumn, SortDirection.DESCENDING)));
    pUserGrid.setSelectionMode(SelectionMode.NONE);
    pUserGrid.setColumnReorderingAllowed(true);
    return pUserGrid;
  }

  private Component renderUserRoleComponent(ProjectUser projectUser) {
    boolean editable = canChangeAccess && !isCurrentUser(projectUser)
        && projectUser.projectRole() != ProjectRole.OWNER;
    if (!editable) {
      return roleLabel(projectUser.projectRole());
    }
    Select<ProjectRole> roleSelect = createRoleSelect(projectUser.projectRole());
    roleSelect.addValueChangeListener(event -> onUserRoleChanged(projectUser, event.getValue()));
    return roleSelect;
  }

  private Component renderUserActionCell(ProjectUser projectUser) {
    Div cell = new Div();
    cell.addClassName("change-project-access-cell");
    // We want to ensure that even if the frontend components are shown no event is propagated
    // if the user doesn't have the correct role or tries to remove himself/the project owner
    if (!canChangeAccess || isCurrentUser(projectUser)
        || projectUser.projectRole() == ProjectRole.OWNER) {
      return cell;
    }
    Button removeButton = new Button("Remove");
    removeButton.addClassName("remove-access-button");
    removeButton.addClickListener(clickEvent -> {
      if (!canChangeAccess) {
        displayError(INVALID_USER_REMOVAL,
            "You don't have permission to remove the user from this project");
        return;
      }
      openInlineRemoveConfirm(cell, removeButton,
          "Remove %s from this project?".formatted(projectUser.userName()),
          () -> removeCollaborator(projectUser));
    });
    cell.add(removeButton);
    return cell;
  }

  /**
   * Replaces the given cell's content with an inline removal confirmation. No dialog is used; the
   * row keeps its context and the confirmation is keyboard-operable. Only one row can be in
   * confirmation state at a time.
   */
  private void openInlineRemoveConfirm(Div cell, Component restore, String question,
      Runnable onConfirm) {
    if (openConfirmCell != null && openConfirmCell != cell && openConfirmRestore != null) {
      openConfirmCell.removeAll();
      openConfirmCell.add(openConfirmRestore);
    }
    cell.removeAll();
    Span questionSpan = new Span(question);
    questionSpan.addClassName("inline-confirm-question");
    Button confirmButton = new Button("Remove", event -> onConfirm.run());
    confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
    confirmButton.addClassName("inline-confirm-remove");
    Button cancelButton = new Button("Cancel", event -> {
      cell.removeAll();
      cell.add(restore);
      if (openConfirmCell == cell) {
        openConfirmCell = null;
        openConfirmRestore = null;
      }
    });
    cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    cancelButton.addClassName("inline-confirm-cancel");
    Span inlineConfirm = new Span(questionSpan, confirmButton, cancelButton);
    inlineConfirm.addClassName("inline-confirm");
    cell.add(inlineConfirm);
    openConfirmCell = cell;
    openConfirmRestore = restore;
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

  private Select<ProjectRole> createRoleSelect(ProjectRole currentRole) {
    Select<ProjectRole> roleSelect = new Select<>();
    roleSelect.addClassName("project-role-select");
    roleSelect.setItemLabelGenerator(ProjectRole::label);
    roleSelect.setItems(
        ProjectRole.READ,
        ProjectRole.WRITE,
        ProjectRole.ADMIN
    );
    roleSelect.setRenderer(new ComponentRenderer<>(
        projectRole -> {
          Span roleLabel = new Span(projectRole.label());
          roleLabel.addClassName("project-role-label");

          Span roleDescription = new Span(ProjectRoleRecommendationRenderer.render(projectRole));
          roleDescription.addClassName("project-role-description");

          Div projectRoleDiv = new Div();
          projectRoleDiv.addClassName("project-role-item");
          projectRoleDiv.add(roleLabel, roleDescription);
          return projectRoleDiv;
        }));
    roleSelect.setValue(currentRole);
    return roleSelect;
  }

  private Span roleLabel(ProjectRole projectRole) {
    Span label = new Span(projectRole.label());
    label.addClassName("project-role-static");
    return label;
  }

  private void removeCollaborator(ProjectUser projectUser) {
    ProjectId projectId = context.projectId().orElseThrow();
    projectAccessService.removeCollaborator(projectId, projectUser.userId());
    refreshProjectUserGrid();
  }

  private void changeGroupRole(ProjectGroup projectGroup, ProjectRole projectRole) {
    if (!canChangeAccess) {
      displayError(INVALID_ROLE_EDIT,
          "You don't have permission to change the role of this group");
      refreshProjectGroupGrid();
      return;
    }
    ProjectId projectId = context.projectId().orElseThrow();
    try {
      projectAccessService.changeAuthorityAccess(projectId,
          GroupSidProvider.GROUP_SID_PREFIX + projectGroup.groupId(), projectRole);
    } catch (ApplicationException e) {
      displayError(INVALID_ROLE_EDIT,
          "You don't have permission to change the role of this group");
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

  private void onUserRoleChanged(ProjectUser projectUser, ProjectRole projectRole) {
    if (!canChangeAccess) {
      displayError(INVALID_ROLE_EDIT, "You don't have permission to change this project role");
      refreshProjectUserGrid();
      return;
    }
    try {
      projectAccessService.changeRole(context.projectId().orElseThrow(), projectUser.userId(),
          projectRole);
    } catch (ApplicationException e) {
      displayError(INVALID_ROLE_EDIT, "You don't have permission to change this project role");
    }
    refreshProjectUserGrid();
  }

  private Grid<ProjectGroup> createProjectGroupGrid() {
    Grid<ProjectGroup> groupGrid = new Grid<>(ProjectGroup.class, false);
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
    groupGrid.addComponentColumn(this::renderGroupRoleComponent)
        .setKey("projectRole")
        .setHeader("Role")
        .setAutoWidth(true)
        .setSortable(true)
        .setComparator(projectGroup -> projectGroup.projectRole().label())
        .setResizable(true);
    groupGrid.addComponentColumn(this::renderGroupActionCell)
        .setKey("action")
        .setHeader("Action")
        .setAutoWidth(true);
    groupGrid.setSelectionMode(Grid.SelectionMode.NONE);
    groupGrid.setColumnReorderingAllowed(true);
    return groupGrid;
  }

  private Component renderGroupRoleComponent(ProjectGroup projectGroup) {
    if (!canChangeAccess) {
      return roleLabel(projectGroup.projectRole());
    }
    Select<ProjectRole> roleSelect = createRoleSelect(projectGroup.projectRole());
    roleSelect.addValueChangeListener(event -> changeGroupRole(projectGroup, event.getValue()));
    return roleSelect;
  }

  private Component renderGroupActionCell(ProjectGroup projectGroup) {
    Div cell = new Div();
    cell.addClassName("change-project-access-cell");
    if (!canChangeAccess) {
      return cell;
    }
    Button removeButton = new Button("Remove");
    removeButton.addClassName("remove-access-button");
    removeButton.addClickListener(clickEvent -> {
      if (!canChangeAccess) {
        displayError(INVALID_USER_REMOVAL,
            "You don't have permission to remove the group from this project");
        return;
      }
      openInlineRemoveConfirm(cell, removeButton,
          "Remove %s from this project?".formatted(projectGroup.groupName()),
          () -> revokeGroup(projectGroup));
    });
    cell.add(removeButton);
    return cell;
  }

  private void refreshProjectGroupGrid() {
    openConfirmCell = null;
    openConfirmRestore = null;
    List<SharedProjectGroup> sharedGroups = projectAccessService.listSharedGroups(
        context.projectId().orElseThrow());
    projectGroupGrid.setItems(
        sharedGroups.stream().map(this::toProjectGroup).toList());
  }

  private ProjectGroup toProjectGroup(SharedProjectGroup sharedProjectGroup) {
    return new ProjectGroup(sharedProjectGroup.groupId(), sharedProjectGroup.groupName(),
        sharedProjectGroup.groupDescription(), sharedProjectGroup.projectRole());
  }

  private void displayError(String title, String description) {
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  private void displaySuccess(String title, String description) {
    SuccessMessage successMessage = new SuccessMessage(title, description);
    StyledNotification notification = new StyledNotification(successMessage);
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
