package life.qbic.datamanager.views.projects.project.access;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.UserInfoComponent;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRoleRecommendationRenderer;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.SharedProjectGroup;
import life.qbic.usergroups.api.GroupInfo;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.api.GroupType;

/**
 * <b>Project Sharing Composer</b>
 *
 * <p>An inline, dialog-free surface to grant project access to one or several principals — people
 * and user groups — at once. Selecting a principal stages a removable chip with its own
 * {@link ProjectRole} control; {@code Grant access} applies all staged grants in a single request.
 * Staging a single principal is the per-item workflow; staging several is the batch workflow.</p>
 *
 * <p>The composer never uses a modal dialog and never requests group membership data. Already
 * granted principals are filtered out of the pickers.</p>
 *
 * @since 1.20.0
 */
public class ProjectSharingComposer extends Div {

  @Serial
  private static final long serialVersionUID = 7139122224671283411L;
  private static final List<ProjectRole> ASSIGNABLE_ROLES = List.of(ProjectRole.READ,
      ProjectRole.WRITE, ProjectRole.ADMIN);

  private final transient UserInformationService userInformationService;
  private final transient GroupInformationService groupInformationService;
  private final ComboBox<UserInfo> personPicker = new ComboBox<>();
  private final ComboBox<GroupInfo> groupPicker = new ComboBox<>();
  private final Div stagedGrants = new Div();
  private final Div stagedUsers = new Div();
  private final Div stagedGroups = new Div();
  private final Div usersSection = new Div();
  private final Div groupsSection = new Div();
  private final Button grantButton = new Button("Grant access");
  /** Centered spinner overlay shown while a grant request is being processed. */
  private final Div loadingOverlay = new Div();
  /** Inline success/error confirmation that stays visible while the drawer remains open. */
  private final Div inlineMessage = new Div();
  /**
   * A permanently attached, visually hidden ARIA live region. The visible {@link #inlineMessage}
   * cannot serve as the live region itself: it is hidden while empty, and a region that becomes
   * visible together with its content in a single update is frequently not announced. Keeping a
   * separate region that is always rendered (and only made visually imperceptible) makes the
   * asynchronously delivered grant result reliably announced.
   */
  private final Div liveRegion = new Div();
  private boolean busy = false;

  /**
   * Principals that already have access, mapped to the role they hold. They stay visible in the
   * pickers (marked as already granted) instead of being hidden, so a search never dead-ends in a
   * silent "no results" and the user learns the principal already has access.
   */
  private final Map<String, ProjectRole> alreadyGrantedUserRoles = new HashMap<>();
  private final Map<String, ProjectRole> alreadyGrantedGroupRoles = new HashMap<>();
  private final Set<String> stagedUserIds = new HashSet<>();
  private final Set<String> stagedGroupIds = new HashSet<>();

  public ProjectSharingComposer(UserInformationService userInformationService,
      GroupInformationService groupInformationService) {
    this.userInformationService = requireNonNull(userInformationService);
    this.groupInformationService = requireNonNull(groupInformationService);
    addClassName("project-sharing-composer");
    layout();
  }

  /**
   * Renders a person in the picker, marking people that already have access instead of hiding
   * them. Selecting such an entry explains that the role must be changed instead (see
   * {@link #configurePersonPicker()}).
   */
  private Component renderPickerUser(UserInfo userInfo) {
    Component identity = renderUser(userInfo);
    ProjectRole grantedRole = alreadyGrantedUserRoles.get(userInfo.id());
    if (grantedRole == null) {
      // Plain option: still gets the scrollbar reserve (see the theme), so a long name cannot run
      // under the overlay scrollbar.
      identity.addClassName("picker-option-plain");
      return identity;
    }
    // The marker trails the identity in both pickers, so the same status is always in the same
    // place. The principal's name keeps its normal contrast: de-emphasising the identity made the
    // list harder to scan and pushed the name below the AA contrast minimum on the hovered row.
    Div option = new Div(identity, accessState(grantedRole));
    option.addClassName("picker-option");
    option.addClassName("picker-option-granted");
    return option;
  }

  private static Component renderUser(UserInfo userInfo) {
    UserAvatar userAvatar = new UserAvatar();
    userAvatar.setUserId(userInfo.id());
    userAvatar.setName(userInfo.platformUserName());
    UserInfoComponent userInfoComponent = new UserInfoComponent(userAvatar,
        userInfo.platformUserName(), userInfo.fullName());
    if (userInfo.oidcId() != null && userInfo.oidcIssuer() != null) {
      userInfoComponent.setOidc(userInfo.oidcIssuer(), userInfo.oidcId());
    }
    userInfoComponent.getElement().setAttribute("title",
        userInfo.fullName() == null || userInfo.fullName().isBlank()
            ? userInfo.platformUserName()
            : "%s (%s)".formatted(userInfo.platformUserName(), userInfo.fullName()));
    return userInfoComponent;
  }

  private void layout() {
    Span title = new Span("Share this project");
    title.addClassName("section-title");
    Span description = new Span(
        "Add one or several people and groups. Everyone you select gains the role chosen next "
            + "to their name.");
    description.addClassName("secondary");

    configurePersonPicker();
    configureGroupPicker();
    stagedUsers.addClassName("staged-grants-list");
    stagedGroups.addClassName("staged-grants-list");
    usersSection.addClassName("staged-grants-section");
    groupsSection.addClassName("staged-grants-section");
    usersSection.add(sectionTitle("People to add"), stagedUsers);
    groupsSection.add(sectionTitle("Groups to add"), stagedGroups);
    usersSection.setVisible(false);
    groupsSection.setVisible(false);
    stagedGrants.addClassName("staged-grants");
    stagedGrants.add(usersSection, groupsSection);

    grantButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    grantButton.addClassName("grant-access-button");
    grantButton.setEnabled(false);
    grantButton.addClickListener(event -> fireGrantRequest());

    Div spinner = new Div();
    spinner.addClassName("psc-spinner");
    Span loadingMessage = new Span("Granting access…");
    loadingMessage.addClassName("project-sharing-composer-loading-message");
    loadingOverlay.addClassName("project-sharing-composer-overlay");
    loadingOverlay.add(spinner, loadingMessage);
    loadingOverlay.getStyle().set("display", "none");

    inlineMessage.addClassName("inline-message");

    // Always-rendered live region for assistive technology; the visible message below only
    // duplicates it visually. Errors interrupt, confirmations and notes wait for a pause.
    liveRegion.addClassName("composer-live-region");
    liveRegion.getElement().setAttribute("role", "status");
    liveRegion.getElement().setAttribute("aria-live", "polite");
    liveRegion.getElement().setAttribute("aria-atomic", "true");

    Div pickers = new Div(personPicker, groupPicker);
    pickers.addClassName("sharing-pickers");

    add(title, description, pickers, stagedGrants, grantButton, inlineMessage, liveRegion,
        loadingOverlay);
  }

  /**
   * Shows or hides the busy overlay while a grant request is processed. Disables the inputs so the
   * same batch cannot be submitted twice.
   *
   * @param busy {@code true} while the grant request is in flight
   */
  public void setBusy(boolean busy) {
    this.busy = busy;
    loadingOverlay.getStyle().set("display", busy ? "flex" : "none");
    personPicker.setEnabled(!busy);
    groupPicker.setEnabled(!busy);
    updateGrantButtonState();
    if (busy) {
      clearInlineMessage();
    }
  }

  /**
   * Shows an inline success confirmation that stays visible until it is dismissed or another grant
   * is staged.
   *
   * @param title the confirmation headline
   * @param lines one line per granted principal, naming the principal and the role it received
   */
  public void showInlineConfirmation(String title, List<String> lines) {
    showInlineConfirmation(title, lines, null, null);
  }

  /**
   * Shows an inline success confirmation with an optional follow-up action, e.g. revealing the
   * newly granted principals in the roster.
   *
   * @param title       the confirmation headline
   * @param lines       one line per granted principal, naming the principal and the role it got
   * @param actionLabel the label of the follow-up action, or {@code null} for no action
   * @param action      the follow-up action, or {@code null} for no action
   */
  public void showInlineConfirmation(String title, List<String> lines, String actionLabel,
      Runnable action) {
    InlineAction followUp = actionLabel == null || action == null
        ? null : new InlineAction(actionLabel, action);
    setInlineMessage(InlineMessageLevel.SUCCESS, title, lines, followUp);
  }

  /**
   * Shows an inline error that names the principals that failed and why. Stays visible until the
   * next staging action.
   *
   * @param title    a short headline, e.g. "Access could not be granted:"
   * @param problems one line per failed principal
   */
  public void showInlineError(String title, List<String> problems) {
    setInlineMessage(InlineMessageLevel.ERROR, title, problems, null);
  }

  /**
   * Shows an inline informational note that stays visible until the next staging action, e.g. when
   * a selected principal already has access to the project.
   *
   * @param title a short headline
   * @param lines one line per note, e.g. the next step the user can take
   */
  public void showInlineInfo(String title, List<String> lines) {
    setInlineMessage(InlineMessageLevel.INFO, title, lines, null);
  }

  /**
   * Builds a user-readable description of a failed grant: the principal, its kind and the reason.
   *
   * @param request the grant that failed
   * @param cause   the exception the service raised
   * @return a line such as {@code "NGS Lab (group): already has access to this project."}
   */
  static String describeFailure(GrantRequest request, RuntimeException cause) {
    String reason = cause.getMessage();
    if (reason == null || reason.isBlank()) {
      reason = "access could not be granted.";
    } else if (reason.contains("already collaborates")) {
      reason = "already has access to this project. Change the role instead.";
    }
    return "%s (%s): %s".formatted(request.displayName(),
        request.type() == PrincipalType.USER ? "user" : "group", reason);
  }

  /**
   * A user-readable one-liner naming a principal that was granted access and the role it received.
   * The success confirmation lists these, so a grant is never confirmed with an anonymous count.
   *
   * @param request the grant that was applied
   * @return a line such as {@code "NGS Lab (group) · editor"}
   */
  static String describeGrant(GrantRequest request) {
    String kind = request.type() == PrincipalType.USER ? "" : " (group)";
    return "%s%s · %s".formatted(request.displayName(), kind, roleLabel(request.role()));
  }

  /**
   * Colour token for a project role: elevated roles share the primary accent, read stays neutral.
   *
   * @param role the project role
   * @return the tag colour for the role
   */
  static TagColor roleColor(ProjectRole role) {
    return switch (role) {
      case READ -> TagColor.CONTRAST;
      case WRITE, ADMIN -> TagColor.PRIMARY;
      // The owner is unique and always present, so it gets its own colour instead of
      // blending in with the other elevated roles.
      case OWNER -> TagColor.GOLD;
    };
  }

  /**
   * Display label for a project role in the sharing UI.
   *
   * @param role the project role
   * @return the display label (member / editor / manager / owner)
   */
  static String roleLabel(ProjectRole role) {
    return switch (role) {
      case READ -> "member";
      case WRITE -> "editor";
      case ADMIN -> "manager";
      case OWNER -> "owner";
    };
  }

  private void setInlineMessage(InlineMessageLevel level, String title, List<String> lines,
      InlineAction action) {
    inlineMessage.removeAll();
    inlineMessage.removeClassNames("inline-message-success", "inline-message-error",
        "inline-message-info");
    inlineMessage.addClassName(switch (level) {
      case SUCCESS -> "inline-message-success";
      case ERROR -> "inline-message-error";
      case INFO -> "inline-message-info";
    });
    // Errors interrupt the screen reader; confirmations and notes wait for a pause.
    liveRegion.getElement().setAttribute("role",
        level == InlineMessageLevel.ERROR ? "alert" : "status");
    liveRegion.getElement().setAttribute("aria-live",
        level == InlineMessageLevel.ERROR ? "assertive" : "polite");
    liveRegion.setText(announcement(title, lines));

    Span titleSpan = new Span(title);
    titleSpan.addClassName("inline-message-title");
    inlineMessage.add(titleSpan);
    lines.forEach(line -> {
      Span lineSpan = new Span(line);
      lineSpan.addClassName("inline-message-line");
      inlineMessage.add(lineSpan);
    });
    if (action != null) {
      Button actionButton = new Button(action.label());
      actionButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
      actionButton.addClassName("inline-message-action");
      actionButton.addClickListener(event -> action.action().run());
      inlineMessage.add(actionButton);
    }
    Button dismiss = new Button(VaadinIcon.CLOSE_SMALL.create());
    dismiss.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
    dismiss.addClassName("inline-message-dismiss");
    dismiss.getElement().setAttribute("aria-label", "Dismiss notification");
    dismiss.addClickListener(event -> clearInlineMessage());
    inlineMessage.add(dismiss);
  }

  private void clearInlineMessage() {
    inlineMessage.removeAll();
    liveRegion.setText("");
  }

  /**
   * Flattens a title and its bullet lines into one plain-text sentence for the live region, so the
   * screen reader does not read the visual bullet characters. Lines that already end a sentence do
   * not get a second terminator.
   */
  private static String announcement(String title, List<String> lines) {
    String headline = terminate(title);
    if (lines.isEmpty()) {
      return headline;
    }
    String body = lines.stream().map(ProjectSharingComposer::terminate)
        .collect(Collectors.joining(" "));
    return "%s %s".formatted(headline, body);
  }

  /**
   * Ensures a fragment ends the sentence exactly once, so combining fragments never produces
   * ".." or a missing terminator.
   */
  private static String terminate(String fragment) {
    String trimmed = fragment.strip();
    while (trimmed.endsWith(".") || trimmed.endsWith(":")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1).strip();
    }
    return trimmed + ".";
  }

  private void configurePersonPicker() {
    personPicker.setLabel("Add a person");
    personPicker.setPlaceholder("Search by username, name or ORCID…");
    personPicker.setItemLabelGenerator(UserInfo::platformUserName);
    personPicker.setRenderer(new ComponentRenderer<>(this::renderPickerUser));
    personPicker.addClassName("person-selection");
    personPicker.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream()
          .map(it -> new SortOrder(it.getSorted(),
              it.getDirection().equals(SortDirection.DESCENDING)))
          .collect(Collectors.toCollection(ArrayList::new));
      sortOrders.add(SortOrder.of("userName").descending());
      return userInformationService.queryActiveUsersWithFilter(query.getFilter().orElse(null),
              query.getOffset(), query.getLimit(), List.copyOf(sortOrders)).stream()
          .filter(userInfo -> !stagedUserIds.contains(userInfo.id()))
          // Selectable people first, already-granted ones after. Note: this picker pages
          // server-side, so the ordering is applied within the fetched page. In the common case the
          // user has narrowed to a page worth of matches with the search box, so this is the same
          // as a global order; it never drops or duplicates an item.
          .sorted(Comparator.comparingInt(
              userInfo -> alreadyGrantedUserRoles.containsKey(userInfo.id()) ? 1 : 0));
    });
    personPicker.addValueChangeListener(event -> {
      if (event.getValue() == null) {
        return;
      }
      UserInfo userInfo = event.getValue();
      personPicker.setValue(null);
      ProjectRole grantedRole = alreadyGrantedUserRoles.get(userInfo.id());
      if (grantedRole != null) {
        showAlreadyGranted(userInfo.platformUserName(), grantedRole);
        return;
      }
      stageUser(userInfo);
    });
  }

  private void configureGroupPicker() {
    groupPicker.setLabel("Add a group");
    groupPicker.setPlaceholder("Search groups…");
    groupPicker.setItemLabelGenerator(GroupInfo::name);
    groupPicker.setRenderer(new ComponentRenderer<>(this::renderGroupOption));
    groupPicker.addClassName("group-selection");
    groupPicker.setItems(query -> {
      String filter = query.getFilter().orElse("").toLowerCase();
      return groupInformationService.listPublicDirectory().stream()
          .filter(groupInfo -> !stagedGroupIds.contains(groupInfo.id()))
          .filter(groupInfo -> filter.isEmpty()
              || groupInfo.name().toLowerCase().contains(filter)
              || (groupInfo.description() != null
              && groupInfo.description().toLowerCase().contains(filter)))
          // Selectable groups first, already-granted ones after: the actionable candidates cluster
          // at the top instead of being interleaved with rows that cannot be selected.
          .sorted(Comparator.comparingInt(
              groupInfo -> alreadyGrantedGroupRoles.containsKey(groupInfo.id()) ? 1 : 0))
          .skip(query.getOffset())
          .limit(query.getLimit());
    });
    groupPicker.addValueChangeListener(event -> {
      if (event.getValue() == null) {
        return;
      }
      GroupInfo groupInfo = event.getValue();
      groupPicker.setValue(null);
      ProjectRole grantedRole = alreadyGrantedGroupRoles.get(groupInfo.id());
      if (grantedRole != null) {
        showAlreadyGranted(groupInfo.name(), grantedRole);
        return;
      }
      stageGroup(groupInfo);
    });
  }

  private void stageUser(UserInfo userInfo) {
    if (!stagedUserIds.add(userInfo.id())) {
      return;
    }
    clearInlineMessage();
    addStagedGrant(PrincipalType.USER, userInfo.id(), userInfo.platformUserName(),
        renderUser(userInfo));
    personPicker.getDataProvider().refreshAll();
    updateGrantButtonState();
  }

  private void stageGroup(GroupInfo groupInfo) {
    if (!stagedGroupIds.add(groupInfo.id())) {
      return;
    }
    clearInlineMessage();
    addStagedGrant(PrincipalType.GROUP, groupInfo.id(), groupInfo.name(), renderGroup(groupInfo));
    groupPicker.getDataProvider().refreshAll();
    updateGrantButtonState();
  }

  private static Component renderGroup(GroupInfo groupInfo) {
    UserAvatar avatar = new UserAvatar();
    avatar.setGroupId(groupInfo.id());
    avatar.addClassName("staged-group-avatar");
    Span name = new Span(groupInfo.name());
    name.addClassName("bold");
    Div text = new Div(name);
    text.addClassName("group-identity-text");
    if (groupInfo.description() != null && !groupInfo.description().isBlank()) {
      Span description = new Span(groupInfo.description());
      description.addClassName("tertiary");
      text.add(description);
    }
    Div identity = new Div(avatar, text);
    identity.addClassName("group-identity");
    String tooltip = groupInfo.description() == null || groupInfo.description().isBlank()
        ? groupInfo.name()
        : "%s — %s".formatted(groupInfo.name(), groupInfo.description());
    identity.getElement().setAttribute("title", tooltip);
    return identity;
  }

  /**
   * Renders a group as a search result: the name plus its type badge on the first line and the
   * description (if any) muted on a second line. Keeps the picker scannable without overloading it.
   */
  private Component renderGroupOption(GroupInfo groupInfo) {
    Div option = new Div();
    option.addClassName("group-option");
    // Three stable lines: type badge, name (with avatar), description. Keeping the badge on its
    // own line stops it from being pushed out of the dropdown when the name/description is long.
    option.add(groupTypeBadge(groupInfo.type()));
    UserAvatar avatar = new UserAvatar();
    avatar.setGroupId(groupInfo.id());
    avatar.addClassName("group-option-avatar");
    Span name = new Span(groupInfo.name());
    name.addClassName("bold");
    name.addClassName("group-option-name");
    name.getElement().setAttribute("title", groupInfo.name());
    Div nameRow = new Div(avatar, name);
    nameRow.addClassName("group-option-name-row");
    option.add(nameRow);
    if (groupInfo.description() != null && !groupInfo.description().isBlank()) {
      Span description = new Span(groupInfo.description());
      description.addClassName("tertiary");
      description.addClassName("group-option-description");
      description.getElement().setAttribute("title", groupInfo.description());
      option.add(description);
    }
    if (alreadyGrantedGroupRoles.containsKey(groupInfo.id())) {
      // Same placement as the person picker: trailing the name row, not appended as an extra
      // stacked line (which read as a body line of the option rather than a state).
      nameRow.add(accessState(alreadyGrantedGroupRoles.get(groupInfo.id())));
      option.addClassName("picker-option-granted");
    }
    return option;
  }

  /**
   * The state marker for a principal that already has access: a check icon, the state and the role
   * the principal currently holds.
   *
   * <p>It is deliberately <em>not</em> a neutral pill. The group dropdown already carries a neutral
   * type badge ({@code organisational} / {@code User Group}); a second pill of the same shape read
   * as another attribute instead of a state. The success tint plus the check icon separates "what
   * kind of group is this" from "does it already have access", and naming the role answers the
   * question the user actually has. The tooltip carries the next step.</p>
   *
   * @param role the role the principal currently holds
   * @return the rendered state marker
   */
  private static Tag accessState(ProjectRole role) {
    Tag tag = new Tag("Has access · " + roleLabel(role));
    tag.setTagColor(TagColor.SUCCESS);
    tag.addClassName("picker-access-state");
    Icon icon = VaadinIcon.CHECK.create();
    icon.addClassName("picker-access-state-icon");
    tag.addComponentAsFirst(icon);
    tag.setTitle(
        "Already has access as %s. Change the role in the access roster instead."
            .formatted(roleLabel(role)));
    return tag;
  }

  /**
   * Explains that the selected principal already has access and how to change it, instead of
   * staging a grant that the service would reject as a duplicate.
   */
  private void showAlreadyGranted(String displayName, ProjectRole grantedRole) {
    showInlineInfo("%s already has access as %s.".formatted(displayName, roleLabel(grantedRole)),
        List.of("Change the role from the access roster instead of granting it again."));
  }

  /**
   * A small badge naming the group type, matching the groups hub convention
   * ({@code organisational} / {@code User Group}).
   *
   * @param type the group type
   * @return the rendered badge
   */
  static Span groupTypeBadge(GroupType type) {
    Span badge = new Span(type == GroupType.ORG ? "organisational" : "User Group");
    badge.addClassName("my-groups-badge");
    badge.addClassName(type == GroupType.ORG
        ? "my-groups-badge--type-org" : "my-groups-badge--type-adhoc");
    return badge;
  }

  private static Span sectionTitle(String text) {
    Span title = new Span(text);
    title.addClassName("staged-grants-section-title");
    return title;
  }

  private void addStagedGrant(PrincipalType type, String id, String displayName,
      Component identity) {
    StagedGrant staged = new StagedGrant(type, id, displayName, identity);
    if (type == PrincipalType.USER) {
      stagedUsers.add(staged);
      usersSection.setVisible(true);
    } else {
      stagedGroups.add(staged);
      groupsSection.setVisible(true);
    }
  }

  private void removeStagedGrant(StagedGrant stagedGrant) {
    if (stagedGrant.type() == PrincipalType.USER) {
      stagedUserIds.remove(stagedGrant.id());
      stagedUsers.remove(stagedGrant);
      usersSection.setVisible(stagedUsers.getComponentCount() > 0);
      personPicker.getDataProvider().refreshAll();
    } else {
      stagedGroupIds.remove(stagedGrant.id());
      stagedGroups.remove(stagedGrant);
      groupsSection.setVisible(stagedGroups.getComponentCount() > 0);
      groupPicker.getDataProvider().refreshAll();
    }
    updateGrantButtonState();
  }

  private void updateGrantButtonState() {
    grantButton.setEnabled(!busy && stagedGrantCount() > 0);
  }

  private void fireGrantRequest() {
    List<StagedGrant> staged = new ArrayList<>();
    stagedUsers.getChildren().filter(StagedGrant.class::isInstance)
        .map(StagedGrant.class::cast).forEach(staged::add);
    stagedGroups.getChildren().filter(StagedGrant.class::isInstance)
        .map(StagedGrant.class::cast).forEach(staged::add);
    List<GrantRequest> requests = staged.stream()
        .map(stagedGrant -> new GrantRequest(stagedGrant.type(), stagedGrant.id(),
            stagedGrant.role(), stagedGrant.displayName()))
        .toList();
    if (requests.isEmpty()) {
      return;
    }
    fireEvent(new GrantRequestedEvent(this, true, requests));
  }

  /**
   * Provides the currently granted principals so the pickers can filter them out.
   *
   * @param collaborators the direct collaborators of the project
   * @param sharedGroups  the groups currently shared onto the project
   */
  public void setAlreadyGranted(List<ProjectCollaborator> collaborators,
      List<SharedProjectGroup> sharedGroups) {
    alreadyGrantedUserRoles.clear();
    collaborators.forEach(
        collaborator -> alreadyGrantedUserRoles.put(collaborator.userId(),
            collaborator.projectRole()));
    alreadyGrantedGroupRoles.clear();
    sharedGroups.forEach(
        group -> alreadyGrantedGroupRoles.put(group.groupId(), group.projectRole()));
    clearStagedGrants();
    personPicker.getDataProvider().refreshAll();
    groupPicker.getDataProvider().refreshAll();
  }

  /**
   * Clears the staging area and any inline confirmation message. Called after a successful grant
   * or when the composer is dismissed.
   */
  public void reset() {
    clearStagedGrants();
    clearInlineMessage();
  }

  private int stagedGrantCount() {
    return stagedUsers.getComponentCount() + stagedGroups.getComponentCount();
  }

  private void clearStagedGrants() {
    stagedUsers.removeAll();
    stagedGroups.removeAll();
    usersSection.setVisible(false);
    groupsSection.setVisible(false);
    stagedUserIds.clear();
    stagedGroupIds.clear();
    updateGrantButtonState();
    personPicker.getDataProvider().refreshAll();
    groupPicker.getDataProvider().refreshAll();
  }

  public Registration addGrantListener(
      ComponentEventListener<GrantRequestedEvent> listener) {
    return addListener(GrantRequestedEvent.class, listener);
  }

  /**
   * The kind of principal a grant refers to.
   */
  public enum PrincipalType {
    USER,
    GROUP
  }

  /**
   * The severity of an inline message. It drives the colour, the icon-less emphasis and how
   * assistive technology announces the message.
   */
  private enum InlineMessageLevel {
    SUCCESS,
    ERROR,
    INFO
  }

  /**
   * An optional follow-up action rendered inside an inline confirmation.
   */
  private record InlineAction(String label, Runnable action) {

  }

  /**
   * A single grant staged in the composer: a principal and the project role it should receive.
   */
  public record GrantRequest(PrincipalType type, String id, ProjectRole role, String displayName) {

  }

  /**
   * A staged principal chip with its own role selection and a remove control.
   */
  private final class StagedGrant extends Div {

    @Serial
    private static final long serialVersionUID = 8907312431231123345L;
    private final PrincipalType type;
    private final String id;
    private final String displayName;
    private final Select<ProjectRole> roleSelect = new Select<>();

    private StagedGrant(PrincipalType type, String id, String displayName, Component identity) {
      this.type = type;
      this.id = id;
      this.displayName = displayName;
      addClassName("staged-grant");
      addClassName(type == PrincipalType.USER ? "staged-grant-user" : "staged-grant-group");
      configureRoleSelect();
      Button remove = new Button(VaadinIcon.CLOSE_SMALL.create());
      remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
      remove.addClassName("remove-staged-grant");
      remove.getElement().setAttribute("aria-label",
          "Remove %s from the share list".formatted(displayName));
      remove.addClickListener(event -> removeStagedGrant(this));
      Div identityWrapper = new Div(identity);
      identityWrapper.addClassName("staged-grant-identity");
      add(identityWrapper, roleSelect, remove);
    }

    private void configureRoleSelect() {
      roleSelect.addClassName("project-role-select");
      roleSelect.setItemLabelGenerator(ProjectSharingComposer::roleLabel);
      roleSelect.setItems(ASSIGNABLE_ROLES);
      roleSelect.setRenderer(new ComponentRenderer<>(role -> {
        Span roleName = new Span(ProjectSharingComposer.roleLabel(role));
        roleName.addClassName("project-role-label");
        Span roleDescription = new Span(ProjectRoleRecommendationRenderer.render(role));
        roleDescription.addClassName("project-role-description");
        Div item = new Div(roleName, roleDescription);
        item.addClassName("project-role-item");
        return item;
      }));
      roleSelect.setValue(ProjectRole.READ);
      roleSelect.getElement().setAttribute("aria-label", "Project role for the selected principal");
    }

    private PrincipalType type() {
      return type;
    }

    private String id() {
      return id;
    }

    private String displayName() {
      return displayName;
    }

    private ProjectRole role() {
      return roleSelect.getValue();
    }
  }

  /**
   * Fired when the user asks to grant access to all staged principals.
   */
  public static class GrantRequestedEvent extends ComponentEvent<ProjectSharingComposer> {

    @Serial
    private static final long serialVersionUID = 5571239981233112391L;
    private final transient List<GrantRequest> requests;

    /**
     * Creates a new grant request event.
     *
     * @param source     the composer that fired the event
     * @param fromClient whether the event originated client-side
     * @param requests   the staged grants to apply
     */
    public GrantRequestedEvent(ProjectSharingComposer source, boolean fromClient,
        List<GrantRequest> requests) {
      super(source, fromClient);
      this.requests = List.copyOf(requests);
    }

    public List<GrantRequest> requests() {
      return requests;
    }
  }
}
