package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
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
 * The component is dialog-free and uses the {@link ProjectSharingComposer} inline at the top for
 * granting access to one or several people and groups in a single action. The access roster below
 * is composed of sections ("People with access" / "Groups with access"), a search + type filter, and
 * shared rows with an always-visible role control and an inline removal confirmation. Users without
 * access-administration rights see a read-only view (group names and descriptions only, never
 * membership data).
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
  private final transient AuthenticationToUserIdTranslator authenticationToUserIdTranslator;

  private final Div header;
  private final ProjectSharingComposer composer;
  private final TextField searchField;
  private final Select<AccessFilter> filterSelect;
  private final Div peopleSection;
  private final Div groupsSection;

  private Context context;
  private boolean canChangeAccess = false;
  private List<ProjectUser> users = List.of();
  private List<ProjectGroup> groups = List.of();
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
    header.add(titleField);

    composer = new ProjectSharingComposer(userInformationService, groupInformationService);
    composer.addClassName("project-sharing-composer");
    composer.addGrantListener(this::onGrantRequested);
    composer.setVisible(false);

    searchField = new TextField();
    filterSelect = new Select<>();
    configureToolbar();

    peopleSection = new Div();
    peopleSection.addClassName("access-section");
    groupsSection = new Div();
    groupsSection.addClassName("access-section");

    Div toolbar = new Div(searchField, filterSelect);
    toolbar.addClassName("access-toolbar");

    Div roster = new Div(toolbar, peopleSection, groupsSection);
    roster.addClassName("access-roster");

    // DOM order keeps the composer first so it stacks on top on small screens; a CSS grid places
    // it in a right-hand rail on wide screens.
    Div body = new Div(composer, roster);
    body.addClassName("access-body");

    add(header, body);
  }

  private void configureToolbar() {
    searchField.setPlaceholder("Search people and groups");
    searchField.setClearButtonVisible(true);
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addClassName("access-search");
    searchField.addValueChangeListener(event -> renderAccessLists());

    filterSelect.setItems(AccessFilter.values());
    filterSelect.setItemLabelGenerator(AccessFilter::label);
    filterSelect.setValue(AccessFilter.ALL);
    filterSelect.addClassName("access-filter");
    filterSelect.getElement().setAttribute("aria-label", "Filter by principal type");
    filterSelect.addValueChangeListener(event -> renderAccessLists());
  }

  public void setContext(Context context) {
    if (context.projectId().isEmpty()) {
      throw new ApplicationException("no project id in context " + context);
    }
    this.context = context;
    this.canChangeAccess = userPermissions.changeProjectAccess(context.projectId().orElseThrow());
    composer.setVisible(canChangeAccess);
    openConfirmCell = null;
    openConfirmRestore = null;
    loadAccess();
  }

  private void loadAccess() {
    ProjectId projectId = context.projectId().orElseThrow();
    List<ProjectCollaborator> collaborators = projectAccessService.listCollaborators(projectId);
    List<SharedProjectGroup> sharedGroups = projectAccessService.listSharedGroups(projectId);
    composer.setAlreadyGranted(collaborators, sharedGroups);
    this.users = collaborators.stream().map(this::toProjectUser).toList();
    this.groups = sharedGroups.stream().map(this::toProjectGroup).toList();
    renderAccessLists();
  }

  private ProjectUser toProjectUser(ProjectCollaborator collaborator) {
    var userInfo = userInformationService.findById(collaborator.userId()).orElseThrow();
    return new ProjectUser(collaborator.userId(), userInfo.platformUserName(), userInfo.fullName(),
        userInfo.oidcId(), userInfo.oidcIssuer(), collaborator.projectRole());
  }

  private ProjectGroup toProjectGroup(SharedProjectGroup sharedProjectGroup) {
    return new ProjectGroup(sharedProjectGroup.groupId(), sharedProjectGroup.groupName(),
        sharedProjectGroup.groupDescription(), sharedProjectGroup.projectRole());
  }

  private void renderAccessLists() {
    openConfirmCell = null;
    openConfirmRestore = null;
    String query = searchField.getValue() == null ? "" : searchField.getValue().trim().toLowerCase();
    boolean queryActive = !query.isEmpty();
    AccessFilter filter = filterSelect.getValue() == null ? AccessFilter.ALL : filterSelect.getValue();
    boolean showPeople = filter != AccessFilter.GROUPS;
    boolean showGroups = filter != AccessFilter.PEOPLE;

    List<ProjectUser> filteredUsers = showPeople
        ? users.stream().filter(user -> matchesUser(user, query)).toList()
        : List.of();
    List<ProjectGroup> filteredGroups = showGroups
        ? groups.stream().filter(group -> matchesGroup(group, query)).toList()
        : List.of();

    renderSection(peopleSection, "People with access", filteredUsers, this::userRow, showPeople,
        queryActive);
    renderSection(groupsSection, "Groups with access", filteredGroups, this::groupRow, showGroups,
        queryActive);
  }

  private boolean matchesUser(ProjectUser user, String query) {
    if (query.isEmpty()) {
      return true;
    }
    return user.userName().toLowerCase().contains(query)
        || (user.fullName() != null && user.fullName().toLowerCase().contains(query))
        || (user.oidc() != null && user.oidc().toLowerCase().contains(query));
  }

  private boolean matchesGroup(ProjectGroup group, String query) {
    if (query.isEmpty()) {
      return true;
    }
    return group.groupName().toLowerCase().contains(query)
        || (group.groupDescription() != null
        && group.groupDescription().toLowerCase().contains(query));
  }

  private <T> void renderSection(Div section, String title, List<T> items,
      Function<T, Component> rowFactory, boolean visible, boolean queryActive) {
    section.removeAll();
    section.setVisible(visible);
    if (!visible) {
      return;
    }
    section.add(sectionHeader(title));
    if (items.isEmpty()) {
      Span empty = new Span(queryActive ? "No matches." : "None.");
      empty.addClassNames("secondary", "access-empty");
      section.add(empty);
    } else {
      items.forEach(item -> section.add(rowFactory.apply(item)));
    }
  }

  private Div sectionHeader(String title) {
    Span titleSpan = new Span(title);
    titleSpan.addClassName("access-section-title");
    Span roleHeader = new Span("Role");
    roleHeader.addClassName("access-role-header");
    Div sectionHeader = new Div(titleSpan, roleHeader);
    sectionHeader.addClassName("access-section-header");
    return sectionHeader;
  }

  private Component userRow(ProjectUser user) {
    Div actions = new Div();
    actions.addClassName("access-actions");
    actions.add(userRoleControl(user), userRemoveControl(user, actions));
    Div row = new Div(personIdentity(user), actions);
    row.addClassName("access-row");
    return row;
  }

  private Component groupRow(ProjectGroup group) {
    Div actions = new Div();
    actions.addClassName("access-actions");
    actions.add(groupRoleControl(group), groupRemoveControl(group, actions));
    Div row = new Div(groupIdentity(group), actions);
    row.addClassName("access-row");
    return row;
  }

  private Component personIdentity(ProjectUser user) {
    UserAvatar avatar = new UserAvatar();
    avatar.setUserId(user.userId());
    avatar.setName(user.userName());
    Span name = new Span();
    name.addClassName("access-name");
    Span userNameSpan = new Span(user.userName());
    userNameSpan.addClassName("bold");
    name.add(userNameSpan);
    if (user.fullName() != null && !user.fullName().isBlank()) {
      Span fullNameSpan = new Span(user.fullName());
      fullNameSpan.addClassName("access-full-name");
      name.add(fullNameSpan);
    }
    Div identity = new Div(typeTag(PrincipalType.USER), avatar, name);
    identity.addClassName("access-identity");
    identity.getElement().setAttribute("title",
        user.fullName() == null || user.fullName().isBlank() ? user.userName()
            : "%s (%s)".formatted(user.userName(), user.fullName()));
    return identity;
  }

  private Component groupIdentity(ProjectGroup group) {
    Icon groupIcon = VaadinIcon.USERS.create();
    groupIcon.addClassName("access-group-icon");
    Span name = new Span(group.groupName());
    name.addClassName("access-name");
    Div nameBlock = new Div(name);
    nameBlock.addClassName("access-name-block");
    if (group.groupDescription() != null && !group.groupDescription().isBlank()) {
      Span description = new Span(group.groupDescription());
      description.addClassName("access-description");
      nameBlock.add(description);
    }
    Div identity = new Div(typeTag(PrincipalType.GROUP), groupIcon, nameBlock);
    identity.addClassName("access-identity");
    identity.getElement().setAttribute("title",
        group.groupDescription() == null || group.groupDescription().isBlank()
            ? group.groupName()
            : "%s — %s".formatted(group.groupName(), group.groupDescription()));
    return identity;
  }

  private static Tag typeTag(PrincipalType type) {
    Tag tag = new Tag(type == PrincipalType.USER ? "User" : "Group");
    tag.setTagColor(type == PrincipalType.USER ? TagColor.CONTRAST : TagColor.TEAL);
    tag.addClassName("access-type-tag");
    return tag;
  }

  private static Tag roleBadge(ProjectRole role) {
    Tag tag = new Tag(role.label());
    tag.setTagColor(ProjectSharingComposer.roleColor(role));
    tag.addClassName("access-role-badge");
    return tag;
  }

  private Component userRoleControl(ProjectUser user) {
    boolean editable = canChangeAccess && !isCurrentUser(user)
        && user.projectRole() != ProjectRole.OWNER;
    if (!editable) {
      return roleBadge(user.projectRole());
    }
    Select<ProjectRole> roleSelect = createRoleSelect(user.projectRole());
    roleSelect.addValueChangeListener(event -> onUserRoleChanged(user, event.getValue()));
    return roleSelect;
  }

  private Component groupRoleControl(ProjectGroup group) {
    if (!canChangeAccess) {
      return roleBadge(group.projectRole());
    }
    Select<ProjectRole> roleSelect = createRoleSelect(group.projectRole());
    roleSelect.addValueChangeListener(event -> changeGroupRole(group, event.getValue()));
    return roleSelect;
  }

  private Component userRemoveControl(ProjectUser user, Div actionsCell) {
    boolean removable = canChangeAccess && !isCurrentUser(user)
        && user.projectRole() != ProjectRole.OWNER;
    if (!removable) {
      return new Span();
    }
    Button removeButton = new Button("Remove");
    removeButton.addClassName("remove-access-button");
    removeButton.addClickListener(clickEvent -> openInlineRemoveConfirm(actionsCell, removeButton,
        "Remove %s from this project?".formatted(user.userName()),
        () -> removeCollaborator(user)));
    return removeButton;
  }

  private Component groupRemoveControl(ProjectGroup group, Div actionsCell) {
    if (!canChangeAccess) {
      return new Span();
    }
    Button removeButton = new Button("Remove");
    removeButton.addClassName("remove-access-button");
    removeButton.addClickListener(clickEvent -> openInlineRemoveConfirm(actionsCell, removeButton,
        "Remove %s from this project?".formatted(group.groupName()),
        () -> revokeGroup(group)));
    return removeButton;
  }

  /**
   * Replaces the given action cell's content with an inline removal confirmation. No dialog is used;
   * the row keeps its context and the confirmation is keyboard-operable. Only one row can be in
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

  private Select<ProjectRole> createRoleSelect(ProjectRole currentRole) {
    Select<ProjectRole> roleSelect = new Select<>();
    roleSelect.addClassName("project-role-select");
    roleSelect.setItemLabelGenerator(ProjectRole::label);
    roleSelect.setItems(ProjectRole.READ, ProjectRole.WRITE, ProjectRole.ADMIN);
    roleSelect.setRenderer(new ComponentRenderer<>(projectRole -> {
      Span roleLabel = new Span(projectRole.label());
      roleLabel.addClassName("project-role-label");
      Span roleDescription = new Span(ProjectRoleRecommendationRenderer.render(projectRole));
      roleDescription.addClassName("project-role-description");
      Div projectRoleDiv = new Div(roleLabel, roleDescription);
      projectRoleDiv.addClassName("project-role-item");
      return projectRoleDiv;
    }));
    roleSelect.setValue(currentRole);
    return roleSelect;
  }

  private boolean isCurrentUser(ProjectUser projectUser) {
    var userId = this.authenticationToUserIdTranslator.translateToUserId(
        SecurityContextHolder.getContext().getAuthentication()).orElseThrow();
    return Objects.equals(projectUser.userId(), userId);
  }

  private void removeCollaborator(ProjectUser projectUser) {
    ProjectId projectId = context.projectId().orElseThrow();
    projectAccessService.removeCollaborator(projectId, projectUser.userId());
    loadAccess();
  }

  private void onUserRoleChanged(ProjectUser projectUser, ProjectRole projectRole) {
    if (!canChangeAccess) {
      displayError(INVALID_ROLE_EDIT, "You don't have permission to change this project role");
      loadAccess();
      return;
    }
    try {
      projectAccessService.changeRole(context.projectId().orElseThrow(), projectUser.userId(),
          projectRole);
    } catch (ApplicationException e) {
      displayError(INVALID_ROLE_EDIT, "You don't have permission to change this project role");
    }
    loadAccess();
  }

  private void changeGroupRole(ProjectGroup projectGroup, ProjectRole projectRole) {
    if (!canChangeAccess) {
      displayError(INVALID_ROLE_EDIT,
          "You don't have permission to change the role of this group");
      loadAccess();
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
    loadAccess();
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
    loadAccess();
  }

  private void onGrantRequested(GrantRequestedEvent event) {
    ProjectId projectId = context.projectId().orElseThrow();
    int granted = 0;
    List<String> problems = new ArrayList<>();
    for (GrantRequest request : event.requests()) {
      try {
        if (request.type() == PrincipalType.USER) {
          projectAccessService.addCollaborator(projectId, request.id(), request.role());
        } else {
          projectAccessService.addAuthorityAccess(projectId,
              GroupSidProvider.GROUP_SID_PREFIX + request.id(), request.role());
        }
        granted++;
      } catch (RuntimeException e) {
        problems.add(ProjectSharingComposer.describeFailure(request, e));
      }
    }
    loadAccess();
    if (!problems.isEmpty()) {
      composer.showInlineError(granted > 0
          ? "Access granted to %d, but some requests failed:".formatted(granted)
          : "Access could not be granted:", problems);
    } else if (granted > 0) {
      composer.reset();
      composer.showInlineConfirmation(granted == 1
          ? "Access granted to 1 principal. The list below is up to date."
          : "Access granted to %d principals. The list below is up to date."
              .formatted(granted));
    }
  }

  private void displayError(String title, String description) {
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  /**
   * The type filter of the access roster.
   */
  public enum AccessFilter {
    ALL("All"),
    PEOPLE("People"),
    GROUPS("Groups");

    private final String label;

    AccessFilter(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
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

    protected void setOidc(String oidcIssuer, String oidc) {      if (oidcIssuer.isEmpty() || oidc.isEmpty()) {
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
