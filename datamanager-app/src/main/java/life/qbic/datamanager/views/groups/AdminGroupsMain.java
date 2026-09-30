package life.qbic.datamanager.views.groups;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.List;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.settings.SettingsSection;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupAdministrationPermission;
import life.qbic.usergroups.api.GroupInfo;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.api.GroupManagementService;
import life.qbic.usergroups.api.GroupType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Admin Groups directory</b>
 * <p>
 * Admin-only overview of all organisational groups (type {@code ORG}). Each group is shown as a
 * row with its name, description and type badge — read-only, membership is never exposed
 * (visibility policy, strategy §1a/§5.1). A "New Organisational Group" action opens the
 * org-group creation
 * form ({@link AdminGroupCreationMain}).
 * <p>
 * Access control is <b>defense in depth</b>: the route gate re-checks the caller is a QBiC
 * administrator on every {@code beforeEnter} and reroutes to the not-found page otherwise. The
 * authoritative gate remains the application boundary — all org-group mutations are gated in the
 * application layer; this view is read-only.
 * <p>
 * Lists the org groups via the public directory ({@link GroupInformationService#listPublicDirectory()})
 * filtered to {@link GroupType#ORG}: group names and descriptions are public by design
 * (discoverability), so no membership data can leak here.
 *
 * @since 1.21.0
 */
@Route(value = AppRoutes.GroupsRoutes.ADMIN_GROUPS, layout = GroupsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Groups · Admin")
public class AdminGroupsMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 7152438654172894561L;

  private final transient GroupInformationService groupInformationService;
  private final transient GroupManagementService groupManagementService;
  private final transient GroupAdministrationPermission groupAdministrationPermission;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;

  private transient SettingsSection section;
  private transient Div groupList;

  /**
   * Production constructor: wires the services, the admin gate and the user-id resolution.
   */
  public AdminGroupsMain(
      @Autowired GroupInformationService groupInformationService,
      @Autowired GroupManagementService groupManagementService,
      @Autowired GroupAdministrationPermission groupAdministrationPermission,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator) {
    this.groupInformationService = requireNonNull(groupInformationService,
        "groupInformationService must not be null");
    this.groupManagementService = requireNonNull(groupManagementService,
        "groupManagementService must not be null");
    this.groupAdministrationPermission = requireNonNull(groupAdministrationPermission,
        "groupAdministrationPermission must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");
    addClassName("admin-groups");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!isCurrentUserAdmin()) {
      // Defense in depth: hide the surface entirely. The service gate remains authoritative.
      event.rerouteToError(NotFoundException.class);
      return;
    }
    if (section != null) {
      remove(section);
      section = null;
      groupList = null;
    }
    section = new SettingsSection("Admin · Organisational Groups",
        "Organisational groups shared onto projects as one unit. Names and descriptions are "
            + "visible to all users.");
    groupList = new Div();
    groupList.addClassName("admin-groups-list");
    renderOrgGroups();
    section.addContent(groupList);
    Button newOrgGroupButton = new Button("New Organisational Group");
    newOrgGroupButton.addClassName("primary");
    newOrgGroupButton.addClickListener(click ->
        UI.getCurrent().navigate(AdminGroupCreationMain.class));
    section.addAction(newOrgGroupButton);
    add(section);
  }

  private boolean isCurrentUserAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return userIdTranslator.translateToUserId(authentication)
        .map(groupAdministrationPermission::isAdmin)
        .orElse(false);
  }

  private String currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return userIdTranslator.translateToUserId(authentication).orElseThrow();
  }

  private void renderOrgGroups() {
    List<GroupInfo> orgGroups = orgGroupsFromDirectory();
    if (orgGroups.isEmpty()) {
      Span emptyState = new Span("No organisational groups yet.");
      emptyState.addClassName("admin-groups-empty-state");
      groupList.add(emptyState);
      return;
    }
    // Resolve the acting admin once and build every row with its member count (the admin
    // gate is already enforced in beforeEnter; each orgGroupMemberCount call re-checks it).
    String actingUserId = currentUserId();
    orgGroups.forEach(groupInfo -> groupList.add(buildRow(groupInfo, actingUserId)));
  }

  /**
   * Returns the org groups from the public directory, filtered to {@link GroupType#ORG}.
   *
   * @return the org groups (may be empty)
   */
  List<GroupInfo> orgGroupsFromDirectory() {
    return groupInformationService.listPublicDirectory().stream()
        .filter(groupInfo -> groupInfo.type() == GroupType.ORG)
        .toList();
  }

  Component buildRow(GroupInfo groupInfo, String actingUserId) {
    Div row = new Div();
    // The org-group directory uses the exact same card anatomy and styling as the My Groups
    // rows (shared `my-groups-*` classes), so org group cards and ad-hoc user group cards are
    // visually identical — including their action buttons. The class prefix is historical: the
    // shared group-card styles are anchored in my-groups.css and already reused by
    // GroupMembersComponent.
    row.addClassName("my-groups-row");

    // Vertical scan pattern (mirrors My Groups): title + action on line 1, badges on line 2,
    // description below.
    Div identity = new Div();
    identity.addClassName("my-groups-row__identity");

    Div header = new Div();
    header.addClassName("my-groups-row__header");

    Div title = new Div();
    title.addClassName("my-groups-row__title");
    // The org-group name is the row's primary identity and a navigation target
    // ({@link AdminGroupManagersMain}). A {@link RouterLink} routes via Vaadin's router (real
    // route, no '#' hash) and gives native link behavior, matching the My Groups rows.
    RouterLink name = new RouterLink("", AdminGroupManagersMain.class,
        new RouteParameters(
            new RouteParam(AdminGroupManagersMain.GROUP_ID_ROUTE_PARAMETER, groupInfo.id())));
    name.setText(groupInfo.name());
    name.addClassName("my-groups-row__name");
    title.add(name);

    Div actions = new Div();
    actions.addClassName("my-groups-row__actions");
    actions.add(buildManageButton(groupInfo));
    header.add(title, actions);
    identity.add(header);

    Div badges = new Div();
    badges.addClassName("my-groups-row__badges");
    badges.add(buildTypeBadge());
    badges.add(buildMemberCountBadge(groupManagementService.orgGroupMemberCount(
        groupInfo.id(), actingUserId)));
    identity.add(badges);

    if (groupInfo.description() != null && !groupInfo.description().isBlank()) {
      Span description = new Span(groupInfo.description());
      description.addClassName("my-groups-row__description");
      identity.add(description);
    }

    row.add(identity);
    return row;
  }

  private static Span buildTypeBadge() {
    Span badge = new Span("organisational");
    badge.addClassName("my-groups-badge");
    badge.addClassName("my-groups-badge--type-org");
    return badge;
  }

  /**
   * Member-count badge for the org group card (admin oversight). The count is resolved by the
   * caller from the admin-gated {@link GroupManagementService#orgGroupMemberCount} seam — the
   * public directory never carries membership data.
   */
  private static Span buildMemberCountBadge(int memberCount) {
    Span badge = new Span(memberCount + " member" + (memberCount == 1 ? "" : "s"));
    badge.addClassName("my-groups-badge");
    badge.addClassName("my-groups-badge--member-count");
    return badge;
  }

  /**
   * The admin "Manage" action is styled exactly like the My Groups "Manage group" action — a
   * tertiary inline button (primary text colour, hover/focus tint, chevron) — so both group-card
   * surfaces offer visually identical action affordances.
   */
  private Button buildManageButton(GroupInfo groupInfo) {
    Button manageButton = new Button("Manage",
        new com.vaadin.flow.component.icon.Icon(
            com.vaadin.flow.component.icon.VaadinIcon.CHEVRON_RIGHT));
    manageButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    manageButton.addClassName("my-groups-action--manage");
    manageButton.addClickListener(click ->
        UI.getCurrent().navigate(AdminGroupManagersMain.class,
            new com.vaadin.flow.router.RouteParameters(
                AdminGroupManagersMain.GROUP_ID_ROUTE_PARAMETER, groupInfo.id())));
    return manageButton;
  }
}