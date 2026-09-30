package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
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
import life.qbic.usergroups.api.GroupInfo;
import life.qbic.usergroups.api.GroupType;
import life.qbic.usergroups.api.GroupSidProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Project Access Component
 * <p>
 * The access component is a {@link PageArea} component, which shows the current permissions for all
 * users and user groups within a {@link Project}.
 * <p>
 * Following the measurements/samples layout, the access roster is a searchable table with a toolbar:
 * a search field, a type filter and a selection-based {@code Remove} action. The
 * {@link ProjectSharingComposer} sits in a sticky rail for granting access. Role editing is a
 * per-row {@link Select} (dialog-free). Users without access-administration rights see a read-only
 * view (group names and descriptions only, never membership data).
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
  private final transient Executor taskExecutor;
  private final UiHandle uiHandle = new UiHandle();

  private final Div header;
  private final ProjectSharingComposer composer;
  private final TextField searchField;
  private final Select<AccessFilter> filterSelect;
  private final Button changeRoleButton;
  private final Button removeButton;
  private final Div actionBar;
  private final Tag selectionCount;
  private final Grid<AccessEntry> grid;

  private Context context;
  private boolean canChangeAccess = false;
  private List<AccessEntry> entries = List.of();

  protected ProjectAccessComponent(
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserInformationService userInformationService,
      @Autowired GroupInformationService groupInformationService,
      UserPermissions userPermissions,
      AuthenticationToUserIdTranslator authenticationToUserIdTranslator,
      @Qualifier("taskExecutor") Executor taskExecutor) {
    this.projectAccessService = requireNonNull(projectAccessService,
        "projectAccessService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.groupInformationService = requireNonNull(groupInformationService,
        "groupInformationService must not be null");
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");
    this.authenticationToUserIdTranslator = requireNonNull(authenticationToUserIdTranslator,
        "authenticationToUserIdTranslator must not be null");
    this.taskExecutor = requireNonNull(taskExecutor, "taskExecutor must not be null");
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
    changeRoleButton = new Button("Change role");
    removeButton = new Button("Remove", VaadinIcon.TRASH.create());
    actionBar = new Div();
    selectionCount = new Tag("");
    grid = createGrid();
    configureToolbar();

    Div roster = new Div(toolbar(), actionBar, grid);
    roster.addClassName("access-roster");

    // DOM order keeps the composer first so it stacks on top on small screens; a CSS grid places
    // it in a right-hand rail on wide screens.
    Div body = new Div(composer, roster);
    body.addClassName("access-body");

    add(header, body);
  }

  private Component toolbar() {
    Div toolbar = new Div();
    toolbar.addClassName("access-toolbar");
    Div spacer = new Div();
    spacer.addClassName("flex-grow-1");
    toolbar.add(searchField, filterSelect, spacer, selectionCount, changeRoleButton, removeButton);
    return toolbar;
  }

  private void configureToolbar() {
    searchField.setPlaceholder("Search people and groups");
    searchField.setClearButtonVisible(true);
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addClassName("access-search");
    searchField.addValueChangeListener(event -> applyFilter());

    filterSelect.setItems(AccessFilter.values());
    filterSelect.setItemLabelGenerator(AccessFilter::label);
    filterSelect.setValue(AccessFilter.ALL);
    filterSelect.addClassName("access-filter");
    filterSelect.getElement().setAttribute("aria-label", "Filter by principal type");
    filterSelect.addValueChangeListener(event -> applyFilter());

    changeRoleButton.addClassName("access-change-role-button");
    changeRoleButton.setEnabled(false);
    changeRoleButton.setVisible(false);
    changeRoleButton.addClickListener(event -> showRoleChooser());

    selectionCount.addClassName("access-selection-count");
    selectionCount.setVisible(false);

    removeButton.addClassName("access-remove-button");
    removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    removeButton.setEnabled(false);
    removeButton.setVisible(false);
    removeButton.addClickListener(event -> showRemoveConfirm());

    actionBar.addClassName("access-inline-confirm");
    actionBar.setVisible(false);
  }

  private Grid<AccessEntry> createGrid() {
    Grid<AccessEntry> accessGrid = new Grid<>();
    accessGrid.addClassName("access-grid");
    accessGrid.setSelectionMode(SelectionMode.MULTI);
    // Small roster: let the native page scroll handle overflow instead of an embedded scrollbar.
    accessGrid.setAllRowsVisible(true);
    // Owner and the acting user can never be acted upon, so their checkboxes stay disabled.
    accessGrid.setItemSelectableProvider(this::isActionable);
    accessGrid.addColumn(new ComponentRenderer<>(this::principalCell))
        .setKey("principal")
        .setHeader("Principal")
        .setAutoWidth(true)
        .setFlexGrow(1)
        .setSortable(true)
        .setComparator(Comparator.comparing(AccessEntry::displayName,
            String.CASE_INSENSITIVE_ORDER));
    accessGrid.addColumn(new ComponentRenderer<>(entry -> roleBadge(entry.projectRole())))
        .setKey("role")
        .setHeader("Role")
        .setAutoWidth(true)
        .setFlexGrow(0);
    accessGrid.asMultiSelect().addSelectionListener(event -> updateActionButtons());
    return accessGrid;
  }

  public void setContext(Context context) {
    if (context.projectId().isEmpty()) {
      throw new ApplicationException("no project id in context " + context);
    }
    this.context = context;
    this.canChangeAccess = userPermissions.changeProjectAccess(context.projectId().orElseThrow());
    composer.setVisible(canChangeAccess);
    uiHandle.bind(UI.getCurrent());
    removeButton.setVisible(canChangeAccess);
    changeRoleButton.setVisible(canChangeAccess);
    grid.setSelectionMode(canChangeAccess ? SelectionMode.MULTI : SelectionMode.NONE);
    loadAccess();
  }

  private void loadAccess() {
    ProjectId projectId = context.projectId().orElseThrow();
    List<ProjectCollaborator> collaborators = projectAccessService.listCollaborators(projectId);
    List<SharedProjectGroup> sharedGroups = projectAccessService.listSharedGroups(projectId);
    composer.setAlreadyGranted(collaborators, sharedGroups);
    List<AccessEntry> loaded = new ArrayList<>();
    collaborators.forEach(collaborator -> loaded.add(toAccessEntry(collaborator)));
    sharedGroups.forEach(group -> loaded.add(toAccessEntry(group)));
    loaded.sort(Comparator
        .comparingInt((AccessEntry entry) -> roleRank(entry.projectRole()))
        .thenComparing(AccessEntry::displayName, String.CASE_INSENSITIVE_ORDER));
    this.entries = loaded;
    applyFilter();
  }

  private AccessEntry toAccessEntry(ProjectCollaborator collaborator) {
    var userInfo = userInformationService.findById(collaborator.userId()).orElseThrow();
    return new AccessEntry(PrincipalType.USER, collaborator.userId(), userInfo.platformUserName(),
        userInfo.fullName(), userInfo.oidcId(), userInfo.oidcIssuer(), null, null, null,
        collaborator.projectRole());
  }

  private AccessEntry toAccessEntry(SharedProjectGroup group) {
    GroupType groupType = groupInformationService.findGroupById(group.groupId())
        .map(GroupInfo::type).orElse(null);
    return new AccessEntry(PrincipalType.GROUP, group.groupId(), null, null, null, null,
        group.groupName(), group.groupDescription(), groupType, group.projectRole());
  }

  private void applyFilter() {
    hideActionBar();
    String query = searchField.getValue() == null ? "" : searchField.getValue().trim().toLowerCase();
    AccessFilter filter = filterSelect.getValue() == null ? AccessFilter.ALL
        : filterSelect.getValue();
    List<AccessEntry> filtered = entries.stream()
        .filter(entry -> matchesFilter(entry, filter))
        .filter(entry -> matchesQuery(entry, query))
        .toList();
    grid.setItems(filtered);
    grid.deselectAll();
    updateActionButtons();
  }

  private static boolean matchesFilter(AccessEntry entry, AccessFilter filter) {
    return switch (filter) {
      case ALL -> true;
      case PEOPLE -> entry.isUser();
      case GROUPS -> !entry.isUser();
    };
  }

  private static boolean matchesQuery(AccessEntry entry, String query) {
    if (query.isEmpty()) {
      return true;
    }
    if (entry.isUser()) {
      return contains(entry.userName(), query)
          || contains(entry.fullName(), query)
          || contains(entry.oidc(), query);
    }
    return contains(entry.groupName(), query) || contains(entry.groupDescription(), query);
  }

  private static boolean contains(String value, String query) {
    return value != null && value.toLowerCase().contains(query);
  }

  private void updateActionButtons() {
    int selectedCount = grid.getSelectedItems().size();
    boolean hasSelection = canChangeAccess && selectedCount > 0;
    changeRoleButton.setEnabled(hasSelection);
    removeButton.setEnabled(hasSelection);
    selectionCount.setText(selectedCount == 1 ? "1 selected" : "%d selected".formatted(selectedCount));
    selectionCount.setVisible(selectedCount > 0);
  }

  private Component principalCell(AccessEntry entry) {
    Component principal = entry.isUser() ? userIdentity(entry) : groupIdentity(entry);
    Div cell = new Div(typeTag(entry.type()), principal);
    cell.addClassName("access-principal-cell");
    cell.getElement().setAttribute("title", tooltip(entry));
    return cell;
  }

  private Component userIdentity(AccessEntry entry) {
    UserAvatar avatar = new UserAvatar();
    avatar.setUserId(entry.id());
    avatar.setName(entry.userName());
    Span name = new Span();
    name.addClassName("access-name");
    Span userNameSpan = new Span(entry.userName());
    userNameSpan.addClassName("bold");
    name.add(userNameSpan);
    if (entry.fullName() != null && !entry.fullName().isBlank()) {
      Span fullNameSpan = new Span(entry.fullName());
      fullNameSpan.addClassName("access-full-name");
      name.add(fullNameSpan);
    }
    Div identity = new Div(avatar, name);
    identity.addClassName("access-identity");
    return identity;
  }

  private Component groupIdentity(AccessEntry entry) {
    Icon groupIcon = VaadinIcon.USERS.create();
    groupIcon.addClassName("access-group-icon");
    Span name = new Span(entry.groupName());
    name.addClassName("access-name");
    Div nameHeader = new Div(name);
    nameHeader.addClassName("access-name-header");
    if (entry.groupType() != null) {
      nameHeader.add(ProjectSharingComposer.groupTypeBadge(entry.groupType()));
    }
    Div nameBlock = new Div(nameHeader);
    nameBlock.addClassName("access-name-block");
    if (entry.groupDescription() != null && !entry.groupDescription().isBlank()) {
      Span description = new Span(entry.groupDescription());
      description.addClassName("access-description");
      nameBlock.add(description);
    }
    Div identity = new Div(groupIcon, nameBlock);
    identity.addClassName("access-identity");
    return identity;
  }

  private static String tooltip(AccessEntry entry) {
    if (entry.isUser()) {
      return entry.fullName() == null || entry.fullName().isBlank() ? entry.userName()
          : "%s (%s)".formatted(entry.userName(), entry.fullName());
    }
    return entry.groupDescription() == null || entry.groupDescription().isBlank()
        ? entry.groupName()
        : "%s — %s".formatted(entry.groupName(), entry.groupDescription());
  }

  private static Tag typeTag(PrincipalType type) {
    Tag tag = new Tag(type == PrincipalType.USER ? "User" : "Group");
    tag.setTagColor(type == PrincipalType.USER ? TagColor.CONTRAST : TagColor.TEAL);
    tag.addClassName("access-type-tag");
    return tag;
  }

  private static Tag roleBadge(ProjectRole role) {
    Tag tag = new Tag(ProjectSharingComposer.roleLabel(role));
    tag.setTagColor(ProjectSharingComposer.roleColor(role));
    tag.addClassName("access-role-badge");
    return tag;
  }

  private void applyRole(Set<AccessEntry> selected, ProjectRole projectRole) {
    if (!canChangeAccess) {
      displayError(INVALID_ROLE_EDIT, "You don't have permission to change this project role");
      hideActionBar();
      return;
    }
    ProjectId projectId = context.projectId().orElseThrow();
    for (AccessEntry entry : selected) {
      try {
        if (entry.isUser()) {
          projectAccessService.changeRole(projectId, entry.id(), projectRole);
        } else {
          projectAccessService.changeAuthorityAccess(projectId,
              GroupSidProvider.GROUP_SID_PREFIX + entry.id(), projectRole);
        }
      } catch (ApplicationException e) {
        displayError(INVALID_ROLE_EDIT, "You don't have permission to change this project role");
      }
    }
    loadAccess();
  }

  private void showRoleChooser() {
    Set<AccessEntry> selected = grid.getSelectedItems();
    if (selected.isEmpty()) {
      return;
    }
    actionBar.removeAll();
    Span question = new Span(selected.size() == 1
        ? "Set the role for 1 selected principal:"
        : "Set the role for %d selected principals:".formatted(selected.size()));
    question.addClassName("inline-confirm-question");
    Select<ProjectRole> roleSelect = new Select<>();
    roleSelect.addClassName("access-role-chooser");
    roleSelect.setItems(ProjectRole.READ, ProjectRole.WRITE, ProjectRole.ADMIN);
    roleSelect.setItemLabelGenerator(ProjectSharingComposer::roleLabel);
    roleSelect.setRenderer(new ComponentRenderer<>(role -> {
      Span roleName = new Span(ProjectSharingComposer.roleLabel(role));
      roleName.addClassName("project-role-label");
      Span roleDescription = new Span(ProjectRoleRecommendationRenderer.render(role));
      roleDescription.addClassName("project-role-description");
      Div roleItem = new Div(roleName, roleDescription);
      roleItem.addClassName("project-role-item");
      return roleItem;
    }));
    roleSelect.setValue(ProjectRole.READ);
    roleSelect.getElement().setAttribute("aria-label", "New role for the selected principals");
    Button apply = new Button("Apply", event -> applyRole(selected, roleSelect.getValue()));
    apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    apply.addClassName("inline-role-apply");
    Button cancel = new Button("Cancel", event -> hideActionBar());
    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    cancel.addClassName("inline-confirm-cancel");
    actionBar.add(question, roleSelect, apply, cancel);
    actionBar.setVisible(true);
  }

  private void showRemoveConfirm() {
    Set<AccessEntry> selected = grid.getSelectedItems();
    if (selected.isEmpty()) {
      return;
    }
    actionBar.removeAll();
    Span question = new Span(selected.size() == 1
        ? "Remove 1 principal from this project?"
        : "Remove %d principals from this project?".formatted(selected.size()));
    question.addClassName("inline-confirm-question");
    Button confirm = new Button("Remove", event -> removeSelected(selected));
    confirm.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
    confirm.addClassName("inline-confirm-remove");
    Button cancel = new Button("Cancel", event -> hideActionBar());
    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    cancel.addClassName("inline-confirm-cancel");
    actionBar.add(question, confirm, cancel);
    actionBar.setVisible(true);
  }

  private void hideActionBar() {
    actionBar.setVisible(false);
    actionBar.removeAll();
  }

  private void removeSelected(Set<AccessEntry> selected) {
    ProjectId projectId = context.projectId().orElseThrow();
    for (AccessEntry entry : selected) {
      try {
        if (entry.isUser()) {
          projectAccessService.removeCollaborator(projectId, entry.id());
        } else {
          projectAccessService.removeAuthorityAccess(projectId,
              GroupSidProvider.GROUP_SID_PREFIX + entry.id());
        }
      } catch (ApplicationException e) {
        displayError(INVALID_USER_REMOVAL, "You can't remove this principal from the project");
      }
    }
    loadAccess();
  }

  private boolean isActionable(AccessEntry entry) {
    if (!canChangeAccess || entry.projectRole() == ProjectRole.OWNER) {
      return false;
    }
    if (!entry.isUser()) {
      return true;
    }
    var currentUserId = authenticationToUserIdTranslator.translateToUserId(
        SecurityContextHolder.getContext().getAuthentication()).orElseThrow();
    return !Objects.equals(entry.id(), currentUserId);
  }

  private void onGrantRequested(GrantRequestedEvent event) {
    List<GrantRequest> requests = List.copyOf(event.requests());
    composer.setBusy(true);
    // Run the ACL write on the UI thread (via UiHandle/ui.access): the app uses a
    // VaadinAwareSecurityContextHolderStrategy whose ACL strategy resolves the authenticated
    // principal from the Vaadin session, which is not available on a raw pool thread. The busy
    // overlay is sent to the client with the current response before the scheduled task runs.
    CompletableFuture.runAsync(
        () -> uiHandle.onUiAndPush(() -> onGrantsApplied(applyGrants(requests))),
        taskExecutor);
  }

  private GrantOutcome applyGrants(List<GrantRequest> requests) {
    ProjectId projectId = context.projectId().orElseThrow();
    int granted = 0;
    List<String> problems = new ArrayList<>();
    for (GrantRequest request : requests) {
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
    return new GrantOutcome(granted, List.copyOf(problems));
  }

  private void onGrantsApplied(GrantOutcome outcome) {
    composer.setBusy(false);
    loadAccess();
    if (!outcome.problems().isEmpty()) {
      composer.showInlineError(outcome.granted() > 0
          ? "Access granted to %d, but some requests failed:".formatted(outcome.granted())
          : "Access could not be granted:", outcome.problems());
    } else if (outcome.granted() > 0) {
      composer.reset();
      composer.showInlineConfirmation(outcome.granted() == 1
          ? "Access granted to 1 principal. The list below is up to date."
          : "Access granted to %d principals. The list below is up to date."
              .formatted(outcome.granted()));
    }
  }

  private record GrantOutcome(int granted, List<String> problems) {

  }

  private static int roleRank(ProjectRole role) {
    return switch (role) {
      case OWNER -> 0;
      case ADMIN -> 1;
      case WRITE -> 2;
      case READ -> 3;
    };
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
   * One principal (user or group) with access to the project.
   */
  public record AccessEntry(PrincipalType type, String id, String userName, String fullName,
                            String oidc, String oidcIssuer, String groupName,
                            String groupDescription, GroupType groupType, ProjectRole projectRole) {

    public boolean isUser() {
      return type == PrincipalType.USER;
    }

    public String displayName() {
      return isUser() ? userName : groupName;
    }
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
