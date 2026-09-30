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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
  private final Button grantButton = new Button("Grant access");
  /** Centered spinner overlay shown while a grant request is being processed. */
  private final Div loadingOverlay = new Div();
  /** Inline success/error confirmation that stays visible while the drawer remains open. */
  private final Div inlineMessage = new Div();
  private boolean busy = false;

  private final Set<String> alreadyGrantedUserIds = new HashSet<>();
  private final Set<String> alreadyGrantedGroupIds = new HashSet<>();
  private final Set<String> stagedUserIds = new HashSet<>();
  private final Set<String> stagedGroupIds = new HashSet<>();

  public ProjectSharingComposer(UserInformationService userInformationService,
      GroupInformationService groupInformationService) {
    this.userInformationService = requireNonNull(userInformationService);
    this.groupInformationService = requireNonNull(groupInformationService);
    addClassName("project-sharing-composer");
    layout();
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
    return userInfoComponent;
  }

  private void layout() {
    Span title = new Span("Share this project");
    title.addClassName("section-title");
    Span description = new Span(
        "Add one or several people and groups. Everyone you select gains the role chosen next to "
            + "their name. People and groups that already have access are not shown again.");
    description.addClassName("secondary");

    configurePersonPicker();
    configureGroupPicker();
    stagedGrants.addClassName("staged-grants");

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
    inlineMessage.getStyle().set("display", "none");

    Div pickers = new Div(personPicker, groupPicker);
    pickers.addClassName("sharing-pickers");

    add(title, description, pickers, stagedGrants, grantButton, inlineMessage, loadingOverlay);
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
   * Shows an inline success confirmation that stays visible (the drawer stays open).
   *
   * @param message the confirmation text
   */
  public void showInlineConfirmation(String message) {
    setInlineMessage(message, List.of(), false);
  }

  /**
   * Shows an inline error that names the principals that failed and why. Stays visible until the
   * next staging action.
   *
   * @param title    a short headline, e.g. "Access could not be granted:"
   * @param problems one line per failed principal
   */
  public void showInlineError(String title, List<String> problems) {
    setInlineMessage(title, problems, true);
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
   * Colour token for a project role: elevated roles share the primary accent, read stays neutral.
   *
   * @param role the project role
   * @return the tag colour for the role
   */
  static TagColor roleColor(ProjectRole role) {
    return switch (role) {
      case READ -> TagColor.CONTRAST;
      case WRITE, ADMIN, OWNER -> TagColor.PRIMARY;
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

  private void setInlineMessage(String title, List<String> lines, boolean error) {
    inlineMessage.removeAll();
    inlineMessage.removeClassName("inline-message-success");
    inlineMessage.removeClassName("inline-message-error");
    inlineMessage.addClassName(error ? "inline-message-error" : "inline-message-success");
    Span titleSpan = new Span(title);
    titleSpan.addClassName("inline-message-title");
    inlineMessage.add(titleSpan);
    lines.forEach(line -> {
      Span lineSpan = new Span(line);
      lineSpan.addClassName("inline-message-line");
      inlineMessage.add(lineSpan);
    });
    inlineMessage.getStyle().remove("display");
  }

  private void clearInlineMessage() {
    inlineMessage.removeAll();
    inlineMessage.getStyle().set("display", "none");
  }

  private void configurePersonPicker() {
    personPicker.setLabel("Add a person");
    personPicker.setPlaceholder("Search by username, name or ORCID…");
    personPicker.setItemLabelGenerator(UserInfo::platformUserName);
    personPicker.setRenderer(new ComponentRenderer<>(ProjectSharingComposer::renderUser));
    personPicker.addClassName("person-selection");
    personPicker.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream()
          .map(it -> new SortOrder(it.getSorted(),
              it.getDirection().equals(SortDirection.DESCENDING)))
          .collect(Collectors.toCollection(ArrayList::new));
      sortOrders.add(SortOrder.of("userName").descending());
      return userInformationService.queryActiveUsersWithFilter(query.getFilter().orElse(null),
              query.getOffset(), query.getLimit(), List.copyOf(sortOrders)).stream()
          .filter(userInfo -> !alreadyGrantedUserIds.contains(userInfo.id()))
          .filter(userInfo -> !stagedUserIds.contains(userInfo.id()));
    });
    personPicker.addValueChangeListener(event -> {
      if (event.getValue() != null) {
        stageUser(event.getValue());
        personPicker.setValue(null);
      }
    });
  }

  private void configureGroupPicker() {
    groupPicker.setLabel("Add a group");
    groupPicker.setPlaceholder("Search groups…");
    groupPicker.setItemLabelGenerator(GroupInfo::name);
    groupPicker.setRenderer(new ComponentRenderer<>(ProjectSharingComposer::renderGroupOption));
    groupPicker.addClassName("group-selection");
    groupPicker.setItems(query -> {
      String filter = query.getFilter().orElse("").toLowerCase();
      return groupInformationService.listPublicDirectory().stream()
          .filter(groupInfo -> !alreadyGrantedGroupIds.contains(groupInfo.id()))
          .filter(groupInfo -> !stagedGroupIds.contains(groupInfo.id()))
          .filter(groupInfo -> filter.isEmpty()
              || groupInfo.name().toLowerCase().contains(filter)
              || (groupInfo.description() != null
              && groupInfo.description().toLowerCase().contains(filter)))
          .skip(query.getOffset())
          .limit(query.getLimit());
    });
    groupPicker.addValueChangeListener(event -> {
      if (event.getValue() != null) {
        stageGroup(event.getValue());
        groupPicker.setValue(null);
      }
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
    Span name = new Span(groupInfo.name());
    name.addClassName("bold");
    if (groupInfo.description() == null || groupInfo.description().isBlank()) {
      return name;
    }
    Span description = new Span(groupInfo.description());
    description.addClassName("tertiary");
    Div identity = new Div(name, description);
    identity.addClassName("group-identity");
    return identity;
  }

  /**
   * Renders a group as a search result: the name plus its type badge on the first line and the
   * description (if any) muted on a second line. Keeps the picker scannable without overloading it.
   */
  private static Component renderGroupOption(GroupInfo groupInfo) {
    Div option = new Div();
    option.addClassName("group-option");
    // Three stable lines: type badge, name, description. Keeping the badge on its own line stops
    // it from being pushed out of the dropdown when the name/description is long.
    option.add(groupTypeBadge(groupInfo.type()));
    Span name = new Span(groupInfo.name());
    name.addClassName("bold");
    name.addClassName("group-option-name");
    name.getElement().setAttribute("title", groupInfo.name());
    option.add(name);
    if (groupInfo.description() != null && !groupInfo.description().isBlank()) {
      Span description = new Span(groupInfo.description());
      description.addClassName("tertiary");
      description.addClassName("group-option-description");
      description.getElement().setAttribute("title", groupInfo.description());
      option.add(description);
    }
    return option;
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

  private void addStagedGrant(PrincipalType type, String id, String displayName,
      Component identity) {
    stagedGrants.add(new StagedGrant(type, id, displayName, identity));
  }

  private void removeStagedGrant(StagedGrant stagedGrant) {
    if (stagedGrant.type() == PrincipalType.USER) {
      stagedUserIds.remove(stagedGrant.id());
      personPicker.getDataProvider().refreshAll();
    } else {
      stagedGroupIds.remove(stagedGrant.id());
      groupPicker.getDataProvider().refreshAll();
    }
    stagedGrants.remove(stagedGrant);
    updateGrantButtonState();
  }

  private void updateGrantButtonState() {
    grantButton.setEnabled(!busy && stagedGrants.getChildren().findAny().isPresent());
  }

  private void fireGrantRequest() {
    List<GrantRequest> requests = stagedGrants.getChildren()
        .filter(StagedGrant.class::isInstance)
        .map(StagedGrant.class::cast)
        .map(staged -> new GrantRequest(staged.type(), staged.id(), staged.role(),
            staged.displayName()))
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
    alreadyGrantedUserIds.clear();
    collaborators.stream().map(ProjectCollaborator::userId).forEach(alreadyGrantedUserIds::add);
    alreadyGrantedGroupIds.clear();
    sharedGroups.stream().map(SharedProjectGroup::groupId)
        .forEach(alreadyGrantedGroupIds::add);
    clearStagedGrants();
    personPicker.getDataProvider().refreshAll();
    groupPicker.getDataProvider().refreshAll();
  }

  /**
   * Clears the staging area. Called after a successful grant or when the composer is dismissed.
   */
  public void reset() {
    clearStagedGrants();
  }

  private void clearStagedGrants() {
    stagedGrants.removeAll();
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
      Div identityWrapper = new Div(typeTag(type), identity);
      identityWrapper.addClassName("staged-grant-identity");
      add(identityWrapper, roleSelect, remove);
    }

    private static Tag typeTag(PrincipalType type) {
      Tag tag = new Tag(type == PrincipalType.USER ? "User" : "Group");
      tag.setTagColor(type == PrincipalType.USER ? TagColor.CONTRAST : TagColor.TEAL);
      tag.addClassName("staged-grant-type");
      return tag;
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
