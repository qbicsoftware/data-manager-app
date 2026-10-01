package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
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
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.Toast;
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

  /**
   * Default order of the roster, applied on load and by the sortable Principal column:
   * role (owner → admin → write → read), then type (users before groups), then name
   * (case-insensitive).
   */
  private static final Comparator<AccessEntry> ROSTER_ORDER = Comparator
      .comparingInt((AccessEntry entry) -> roleRank(entry.projectRole()))
      .thenComparingInt((AccessEntry entry) -> typeRank(entry.type()))
      .thenComparing(AccessEntry::displayName, String.CASE_INSENSITIVE_ORDER);
  public static final String INVALID_USER_REMOVAL = "Invalid user removal";
  public static final String INVALID_ROLE_EDIT = "Invalid role edit";

  private final transient ProjectAccessService projectAccessService;
  private final transient UserInformationService userInformationService;
  private final transient GroupInformationService groupInformationService;
  private final transient UserPermissions userPermissions;
  private final transient AuthenticationToUserIdTranslator authenticationToUserIdTranslator;
  private final transient Executor taskExecutor;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final UiHandle uiHandle = new UiHandle();

  private final Div header;
  private final ProjectSharingComposer composer;
  private final TextField searchField;
  private final Select<AccessFilter> filterSelect;
  private final Div statsOverview;
  private final Map<ProjectRole, Div> roleStatChips = new LinkedHashMap<>();
  private Div allStatChip;
  private final Button changeRoleButton;
  private final Button removeButton;
  private final Div actionBar;
  private final Tag selectionCount;
  private final Grid<AccessEntry> grid;

  private Context context;
  private boolean canChangeAccess = false;
  private List<AccessEntry> entries = List.of();
  /**
   * Records the role change of a principal that was just applied, keyed by the principal id, so
   * the affected roster row can show a transient direction indicator (up = increased grant,
   * down = decreased grant) before the next data reload or page leave.
   */
  private final Map<String, RoleChange> recentRoleChanges = new HashMap<>();
  /**
   * The current roster state mirrored into the URL query parameters.
   */
  private AccessRosterState rosterState = AccessRosterState.defaults();
  /**
   * Set while applying an externally provided state (initial load / back-forward / shared link)
   * so the applied state is not mirrored back into the URL.
   */
  private boolean suppressUrlWrite = false;
  /**
   * Set while applying an externally provided state so the search/filter value-change listeners
   * do not re-enter {@link #rosterState} mutation and re-render mid-application.
   */
  private boolean applyingExternalState = false;

  protected ProjectAccessComponent(
      @Autowired ProjectAccessService projectAccessService,
      @Autowired UserInformationService userInformationService,
      @Autowired GroupInformationService groupInformationService,
      UserPermissions userPermissions,
      AuthenticationToUserIdTranslator authenticationToUserIdTranslator,
      @Qualifier("taskExecutor") Executor taskExecutor,
      @Autowired MessageSourceNotificationFactory notificationFactory) {
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
    this.notificationFactory = requireNonNull(notificationFactory,
        "notificationFactory must not be null");
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
    statsOverview = new Div();
    changeRoleButton = new Button("Change role");
    removeButton = new Button("Remove", VaadinIcon.TRASH.create());
    actionBar = new Div();
    selectionCount = new Tag("");
    grid = createGrid();
    configureToolbar();
    configureStatsOverview();

    Div roster = new Div(statsOverview, toolbar(), actionBar, grid);
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
    searchField.addValueChangeListener(event -> {
      if (!applyingExternalState) {
        rosterState = rosterState.withSearch(event.getValue() == null ? "" : event.getValue());
        writeUrl(false);
        applyFilter();
      }
    });

    filterSelect.setItems(AccessFilter.values());
    filterSelect.setItemLabelGenerator(AccessFilter::label);
    filterSelect.setValue(AccessFilter.ALL);
    filterSelect.addClassName("access-filter");
    filterSelect.getElement().setAttribute("aria-label", "Filter by principal type");
    filterSelect.addValueChangeListener(event -> {
      if (!applyingExternalState) {
        rosterState = rosterState.withFilter(
            event.getValue() == null ? AccessFilter.ALL : event.getValue());
        writeUrl(false);
        applyFilter();
      }
    });

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
        // Not auto-width: a long group description would otherwise widen this column without
        // bound and push the Role column out of view. Flex + a minimum width keeps it bounded and
        // lets the cell content ellipsize.
        .setAutoWidth(false)
        .setFlexGrow(1)
        .setSortable(true)
        .setComparator(ROSTER_ORDER);
    accessGrid.addColumn(new ComponentRenderer<>(this::roleCell))
        .setKey("role")
        .setHeader("Role")
        .setAutoWidth(true)
        .setFlexGrow(0);
    accessGrid.asMultiSelect().addSelectionListener(event -> updateActionButtons());
    return accessGrid;
  }

  public void setContext(Context context) {
    setContext(context, AccessRosterState.defaults());
  }

  /**
   * Initialises the component with the project context and an externally provided roster state
   * (e.g. restored from the URL on direct load / reload / shared links).
   */
  public void setContext(Context context, AccessRosterState urlState) {
    if (context.projectId().isEmpty()) {
      throw new ApplicationException("no project id in context " + context);
    }
    this.context = context;
    // The component is @UIScope: role-change indicators must not leak from a previously shown
    // project (their ids are not globally unique across projects).
    recentRoleChanges.clear();
    this.canChangeAccess = userPermissions.changeProjectAccess(context.projectId().orElseThrow());
    composer.setVisible(canChangeAccess);
    uiHandle.bind(UI.getCurrent());
    removeButton.setVisible(canChangeAccess);
    changeRoleButton.setVisible(canChangeAccess);
    grid.setSelectionMode(canChangeAccess ? SelectionMode.MULTI : SelectionMode.NONE);
    suppressUrlWrite = true;
    try {
      applyExternalState(urlState);
    } finally {
      suppressUrlWrite = false;
    }
    loadAccess();
  }

  /**
   * Applies an externally provided roster state (initial load, URL back/forward, shared links)
   * to the search field, type filter and role filter without writing back to the URL.
   */
  public void applyExternalState(AccessRosterState state) {
    this.rosterState = state;
    suppressUrlWrite = true;
    applyingExternalState = true;
    try {
      searchField.setValue(state.search());
      filterSelect.setValue(state.filter());
    } finally {
      suppressUrlWrite = false;
      applyingExternalState = false;
    }
    applyFilter();
  }

  private void loadAccess() {
    ProjectId projectId = context.projectId().orElseThrow();
    List<ProjectCollaborator> collaborators = projectAccessService.listCollaborators(projectId);
    List<SharedProjectGroup> sharedGroups = projectAccessService.listSharedGroups(projectId);
    composer.setAlreadyGranted(collaborators, sharedGroups);
    List<AccessEntry> loaded = new ArrayList<>();
    collaborators.forEach(collaborator -> loaded.add(toAccessEntry(collaborator)));
    sharedGroups.forEach(group -> loaded.add(toAccessEntry(group)));
    loaded.sort(ROSTER_ORDER);
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
    Optional<ProjectRole> role = rosterState.role();
    List<AccessEntry> filtered = entries.stream()
        .filter(entry -> matchesFilter(entry, filter))
        .filter(entry -> role.isEmpty() || entry.projectRole() == role.get())
        .filter(entry -> matchesQuery(entry, query))
        .toList();
    grid.setItems(filtered);
    grid.deselectAll();
    updateActionButtons();
    updateStatsOverview();
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

  /**
   * Builds the summary overview row: a clickable chip with the total number of shared principals,
   * plus one chip per project role with the number of principals holding it. Clicking a chip
   * filters the roster directly to that role (clicking the total clears the role filter).
   */
  private void configureStatsOverview() {
    statsOverview.addClassName("access-stats-overview");
    allStatChip = statChip("Shared total", null);
    statsOverview.add(allStatChip);
    for (ProjectRole role : roleOrder()) {
      Div roleChip = statChip(ProjectSharingComposer.roleLabel(role), role);
      roleStatChips.put(role, roleChip);
      statsOverview.add(roleChip);
    }
  }

  private Div statChip(String label, ProjectRole role) {
    Div chip = new Div();
    chip.addClassName("access-stat-chip");
    if (role != null) {
      chip.addClassName("access-stat-chip-" + role.name().toLowerCase());
    }
    chip.getElement().setAttribute("role", "button");
    chip.getElement().setAttribute("tabindex", "0");
    Span name = new Span(label);
    name.addClassName("access-stat-name");
    Span count = new Span("0");
    count.addClassName("access-stat-count");
    chip.add(name, count);
    chip.addClickListener(event -> selectRoleFilter(role));
    // Keyboard accessibility: the chip behaves like a button (role="button", tabindex="0"),
    // so activate it with Enter or Space as well.
    chip.getElement().addEventListener("keydown", event -> selectRoleFilter(role))
        .setFilter("event.key === 'Enter' || event.key === ' '");
    return chip;
  }

  private void selectRoleFilter(ProjectRole role) {
    rosterState = rosterState.withRole(role == null ? Optional.empty() : Optional.of(role));
    writeUrl(false);
    applyFilter();
  }

  /**
   * Updates the role counts on the overview chips and highlights the active one. Counts always
   * reflect the full roster, independent of the current search/type/role filter, so the manager
   * gets a stable overview of what is shared.
   */
  private void updateStatsOverview() {
    int total = entries.size();
    setStatCount(allStatChip, total);
    allStatChip.setEnabled(total > 0);
    // The active chip always reflects the role filter: “Shared total” is active whenever no
    // role filter is set, independently of the orthogonal search/type filters.
    boolean allActive = rosterState.role().isEmpty();
    allStatChip.getElement().getClassList().set("access-stat-chip-active", allActive);
    for (Map.Entry<ProjectRole, Div> entry : roleStatChips.entrySet()) {
      long count = entries.stream().filter(e -> e.projectRole() == entry.getKey()).count();
      setStatCount(entry.getValue(), (int) count);
      boolean active = rosterState.role().map(role -> role == entry.getKey()).orElse(false);
      entry.getValue().setEnabled(total > 0 && count > 0);
      entry.getValue().getElement().getClassList().set("access-stat-chip-active", active);
    }
  }

  private static void setStatCount(Div chip, int count) {
    chip.getChildren().filter(c -> c instanceof Span)
        .filter(c -> c.getElement().getClassList().contains("access-stat-count"))
        .forEach(c -> ((Span) c).setText(String.valueOf(count)));
  }

  /**
   * Mirrors the current roster state into the URL query parameters so search and filter settings
   * are preserved during natural browser navigation. All roster-state changes (search, type
   * filter, role filter) replace the current history entry — the roster is a single, always
   * in-sync view, so pushing a new entry per change would spam the browser history.
   */
  private void writeUrl(boolean push) {
    if (suppressUrlWrite || context == null || context.projectId().isEmpty()) {
      return;
    }
    UI ui = UI.getCurrent();
    // Defensive null checks: without a current UI or page (e.g. unit tests) there is nothing
    // to navigate, and the state is simply not mirrored.
    if (ui == null || ui.getPage() == null) {
      return;
    }
    Location location =
        new Location(currentAccessPath(), AccessRosterStateCodec.toQueryParameters(rosterState));
    if (push) {
      ui.getPage().getHistory().pushState(null, location);
    } else {
      ui.getPage().getHistory().replaceState(null, location);
    }
  }

  private String currentAccessPath() {
    return String.format(ProjectRoutes.ACCESS, context.projectId().orElseThrow().value());
  }

  private static List<ProjectRole> roleOrder() {
    return List.of(ProjectRole.OWNER, ProjectRole.ADMIN, ProjectRole.WRITE, ProjectRole.READ);
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

  /**
   * Renders the Role cell: a fixed-width indicator slot followed by the role badge. The slot is
   * always present so the badge keeps its normal position whether or not a fresh role change
   * indicator is shown (the indicator never pushes the badge). Green double-up marks an increased
   * grant, red double-down a decreased grant.
   */
  private Component roleCell(AccessEntry entry) {
    Span slot = new Span();
    slot.addClassName("role-change-slot");
    RoleChange change = recentRoleChanges.get(entry.id());
    if (change != null) {
      boolean increased = change.increased();
      Icon icon = new Icon(
          increased ? VaadinIcon.ANGLE_DOUBLE_UP : VaadinIcon.ANGLE_DOUBLE_DOWN);
      icon.addClassName(increased ? "role-change-up" : "role-change-down");
      icon.getElement().setAttribute("aria-label",
          increased ? "Role increased" : "Role decreased");
      icon.getElement().setAttribute("title",
          increased ? "Grant increased (previous role: %s)".formatted(
              ProjectSharingComposer.roleLabel(change.previous()))
              : "Grant decreased (previous role: %s)".formatted(
                  ProjectSharingComposer.roleLabel(change.previous())));
      slot.add(icon);
    }
    Div cell = new Div(slot, roleBadge(entry.projectRole()));
    cell.addClassName("access-role-cell");
    return cell;
  }

  /**
   * A just-applied role change of a principal (previous → updated), used to render a transient
   * direction indicator on the roster row.
   */
  private record RoleChange(ProjectRole previous, ProjectRole updated) {

    private boolean increased() {
      return roleRank(updated) < roleRank(previous);
    }
  }

  private void applyRole(Set<AccessEntry> selected, ProjectRole projectRole) {
    if (!canChangeAccess) {
      displayError(INVALID_ROLE_EDIT, "You don't have permission to change this project role");
      hideActionBar();
      return;
    }
    ProjectId projectId = context.projectId().orElseThrow();
    int updated = 0;
    for (AccessEntry entry : selected) {
      try {
        ProjectRole previousRole = entry.projectRole();
        if (entry.isUser()) {
          projectAccessService.changeRole(projectId, entry.id(), projectRole);
        } else {
          projectAccessService.changeAuthorityAccess(projectId,
              GroupSidProvider.GROUP_SID_PREFIX + entry.id(), projectRole);
        }
        updated++;
        recordRoleChange(entry.id(), previousRole, projectRole);
      } catch (ApplicationException e) {
        displayError(INVALID_ROLE_EDIT, "You don't have permission to change this project role");
      }
    }
    if (updated > 0) {
      showRoleUpdatedConfirmation(updated, projectRole);
    }
    loadAccess();
  }

  /**
   * Records a just-applied role change so the affected roster row can show a transient direction
   * indicator (increase/decrease of the grant). Older indicators are dropped once a handful of
   * further changes have been recorded, so the map stays small.
   */
  private void recordRoleChange(String principalId, ProjectRole previous, ProjectRole updated) {
    if (recentRoleChanges.size() >= 20) {
      recentRoleChanges.clear();
    }
    recentRoleChanges.put(principalId, new RoleChange(previous, updated));
  }

  /**
   * Shows a success toast after the roles of one or more selected principals were changed.
   */
  private void showRoleUpdatedConfirmation(int updated, ProjectRole projectRole) {
    UI currentUi = UI.getCurrent();
    Locale locale = currentUi == null || currentUi.getLocale() == null
        ? Locale.getDefault()
        : currentUi.getLocale();
    String roleLabel = ProjectSharingComposer.roleLabel(projectRole);
    String messageKey = updated == 1
        ? "project-access.role.updated.success"
        : "project-access.role.updated.success.plural";
    Toast toast = notificationFactory.toast(messageKey,
        updated == 1 ? new Object[]{roleLabel}
            : new Object[]{updated, roleLabel},
        locale);
    toast.open();
  }

  private void showRoleChooser() {
    Set<AccessEntry> selected = grid.getSelectedItems();
    if (selected.isEmpty()) {
      return;
    }
    actionBar.removeAll();
    actionBar.removeClassName("access-inline-confirm-danger");
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
    actionBar.addClassName("access-inline-confirm-danger");
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
    if (!canChangeAccess) {
      displayError(INVALID_USER_REMOVAL, "You can't remove this principal from the project");
      hideActionBar();
      return;
    }
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

  private static int typeRank(PrincipalType type) {
    return switch (type) {
      case USER -> 0;
      case GROUP -> 1;
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
      Span userNameSpan = new Span(userName);
      userNameSpan.addClassName("bold");
      userNameSpan.addClassName("user-name");
      Div nameBlock = new Div(userNameSpan);
      nameBlock.addClassName("name-block");
      if (fullName != null && !fullName.isBlank()) {
        Span fullNameSpan = new Span(fullName);
        fullNameSpan.addClassName("user-full-name");
        nameBlock.add(fullNameSpan);
      }
      userInfoContent.add(nameBlock);
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
      OidcLogo oidcLogo = new OidcLogo(oidcType);
      oidcLogo.addClassNames("oidc-logo", "clickable");
      oidcLogo.getElement().setAttribute("title",
          "View %s profile of %s (opens in a new tab)".formatted(oidcType.getName(), oidc));
      Anchor oidcLink = new Anchor(oidcUrl, oidcLogo);
      oidcLink.setTarget(AnchorTarget.BLANK);
      oidcLink.setClassName("oidc-link");
      oidcLink.getElement().setAttribute("aria-label",
          "Open %s profile of %s in a new tab".formatted(oidcType.getName(), oidc));
      userInfoContent.add(oidcLink);
    }
  }
}
