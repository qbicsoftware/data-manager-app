package life.qbic.datamanager.views.projects.overview.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer;
import life.qbic.projectmanagement.application.ProjectOverview.UserInfo;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.SharedProjectGroup;

/**
 * <b>Shared-with summary</b>
 *
 * <p>A compact, collapsible summary of who a project is shared with, rendered inside the project
 * overview card ({@code FEAT-USER-GROUPS-12}, requirement {@code GROUP-R-11}).</p>
 *
 * <p>The collapsed line answers the story's core question at a glance without the previous avatar
 * stack: a fixed "Shared with" label, up to two group name chips, an overflow count and — when the
 * project also has direct collaborators — a clearly labelled people count. Groups are always
 * rendered as names, never as avatars: a user or group without a custom picture resolves to a
 * deterministic identicon, which carries no information at this size.</p>
 *
 * <p>The expanded body lists the groups and the people with their granted project role. It never
 * exposes group members or member counts (visibility policy, strategy §5.1): only the group
 * identity and the direct project grants are shown.</p>
 *
 * <p>When the project has neither group grants nor collaborators, nothing is rendered at all —
 * matching the connected-dataset footer, which deliberately avoids advertising an empty state on
 * the listing.</p>
 *
 * @since 1.19.0
 */
public class SharedWithSummary extends Div {

  @Serial
  private static final long serialVersionUID = -3162090104538975427L;

  /**
   * Group chips shown in the collapsed line. Two fits comfortably next to the fixed label at the
   * card's narrowest usable width; everything beyond it is summarised by the overflow count.
   */
  private static final int MAXIMUM_NUMBER_OF_SHOWN_GROUP_CHIPS = 2;

  /** HTML attribute carrying the native hover tooltip. */
  private static final String TITLE_ATTRIBUTE = "title";

  /**
   * Creates the access summary for one project.
   *
   * @param sharedGroups       the group grants of the project, already ordered by the service
   * @param collaborators      the users with a direct grant on the project
   * @param collaboratorRoles  the granted project role per collaborator user id; may be incomplete,
   *                           in which case the affected person is listed without a role tag
   * @param expanded           whether the section starts expanded (kept across re-renders)
   * @param toggleHandler      notified when the user expands or collapses the section; may be
   *                           {@code null}
   */
  public SharedWithSummary(List<SharedProjectGroup> sharedGroups, Collection<UserInfo> collaborators,
      Map<String, ProjectRole> collaboratorRoles, boolean expanded,
      ProjectCollectionComponent.AccessSummaryToggleHandler toggleHandler) {
    Objects.requireNonNull(sharedGroups);
    Objects.requireNonNull(collaborators);
    Objects.requireNonNull(collaboratorRoles);
    addClassName("project-card-access-summary");
    if (sharedGroups.isEmpty() && collaborators.isEmpty()) {
      setVisible(false);
      return;
    }
    var details = new Details();
    details.addClassName("project-card-access-details");
    details.setSummary(buildSummaryLine(sharedGroups, collaborators));
    details.add(buildExpandedContent(sharedGroups, collaborators, collaboratorRoles));
    details.setOpened(expanded);
    if (toggleHandler != null) {
      details.addOpenedChangeListener(event -> toggleHandler.onToggle(event.isOpened()));
    }
    add(details);
  }

  /**
   * Builds the always-visible line: {@code Shared with <chips> +N · M people}. The full list is
   * additionally available as a native tooltip so mouse users can read the names hidden behind the
   * overflow without expanding the section.
   */
  private static Component buildSummaryLine(List<SharedProjectGroup> groups,
      Collection<UserInfo> collaborators) {
    var line = new Span();
    line.addClassName("project-card-access-line");

    var icon = VaadinIcon.USERS.create();
    icon.addClassName("project-card-access-icon");
    line.add(icon);

    var label = new Span("Shared with");
    label.addClassName("project-card-access-label");
    line.add(label);

    if (!groups.isEmpty()) {
      var chips = new Span();
      chips.addClassName("project-card-access-chips");
      groups.stream().limit(MAXIMUM_NUMBER_OF_SHOWN_GROUP_CHIPS).forEach(group -> {
        var chip = new Span(group.groupName());
        chip.addClassName("project-card-access-chip");
        chip.setTitle(group.groupName());
        chips.add(chip);
      });
      int overflow = groups.size() - MAXIMUM_NUMBER_OF_SHOWN_GROUP_CHIPS;
      if (overflow > 0) {
        var more = new Span("+%d".formatted(overflow));
        more.addClassName("project-card-access-more");
        more.setTitle(groups.stream().map(SharedProjectGroup::groupName)
            .collect(Collectors.joining(", ")));
        chips.add(more);
      }
      line.add(chips);
    }

    if (!collaborators.isEmpty()) {
      if (!groups.isEmpty()) {
        line.add(separator());
      }
      // The word "people" is load-bearing: a bare count next to a group chip would read as a
      // group member count, which the visibility policy forbids exposing to non-members.
      var people = new Span("%d %s".formatted(collaborators.size(),
          collaborators.size() == 1 ? "person" : "people"));
      people.addClassName("project-card-access-people");
      line.add(people);
    }

    line.getElement().setAttribute(TITLE_ATTRIBUTE, buildTooltip(groups, collaborators));
    return line;
  }

  /**
   * Builds the expanded roster. It deliberately reuses the same {@code psd-*} structure as the
   * read-only summary in the sharing drawer, so the card, the drawer and the access page all render
   * a principal the same way (type tag, avatar, name, role). The shared styles are declared for both
   * scopes in {@code project-sharing-drawer.css}.
   */
  private static Component buildExpandedContent(List<SharedProjectGroup> groups,
      Collection<UserInfo> collaborators, Map<String, ProjectRole> collaboratorRoles) {
    var content = new Div();
    content.addClassName("project-card-access-content");
    content.addClassName("psd-summary");
    if (!groups.isEmpty()) {
      content.add(rosterSection("Groups with access",
          groups.stream().map(SharedWithSummary::groupRow).toList(), true));
    }
    if (!collaborators.isEmpty()) {
      List<UserInfo> ordered = orderedCollaborators(collaborators, collaboratorRoles);
      boolean anyRoleKnown = ordered.stream()
          .anyMatch(collaborator -> collaboratorRoles.get(collaborator.userId()) != null);
      content.add(rosterSection("People with access",
          ordered.stream().map(collaborator -> personRow(collaborator,
              collaboratorRoles.get(collaborator.userId()))).toList(), anyRoleKnown));
    }
    return content;
  }

  /**
   * Roster order for people: highest privilege first, then by name. A person whose role could not be
   * resolved sorts last rather than being hidden.
   */
  private static List<UserInfo> orderedCollaborators(Collection<UserInfo> collaborators,
      Map<String, ProjectRole> roles) {
    return collaborators.stream()
        .sorted(Comparator
            .comparingInt((UserInfo collaborator) -> roleRank(roles.get(collaborator.userId())))
            .thenComparing(UserInfo::userName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private static int roleRank(ProjectRole role) {
    if (role == null) {
      return 4;
    }
    return switch (role) {
      case OWNER -> 0;
      case ADMIN -> 1;
      case WRITE -> 2;
      case READ -> 3;
    };
  }

  private static Component rosterSection(String title, List<Component> rows,
      boolean showRoleHeader) {
    var section = new Div();
    section.addClassName("psd-section");
    var sectionTitle = new Span(title);
    sectionTitle.addClassName("psd-section-title");
    var headerRow = new Div(sectionTitle);
    headerRow.addClassName("psd-section-header");
    if (showRoleHeader) {
      var roleHeader = new Span("Role");
      roleHeader.addClassName("psd-role-header");
      headerRow.add(roleHeader);
    }
    section.add(headerRow);
    rows.forEach(section::add);
    return section;
  }

  private static Component groupRow(SharedProjectGroup group) {
    var avatar = new UserAvatar();
    avatar.setGroupId(group.groupId());
    var name = new Span(group.groupName());
    name.addClassName("psd-name");
    var identity = new Div(avatar, name);
    identity.addClassName("psd-group-identity");
    if (group.groupName() != null && !group.groupName().isBlank()) {
      identity.getElement().setAttribute(TITLE_ATTRIBUTE, group.groupName());
    }
    return rosterRow("Group", TagColor.TEAL, identity, group.projectRole());
  }

  private static Component personRow(UserInfo collaborator, ProjectRole role) {
    var avatar = new UserAvatar();
    avatar.setUserId(collaborator.userId());
    avatar.setName(collaborator.userName());
    var name = new Span(collaborator.userName());
    name.addClassName("psd-name");
    name.addClassName("bold");
    var identity = new Div(avatar, name);
    identity.addClassName("psd-user-identity");
    identity.getElement().setAttribute(TITLE_ATTRIBUTE, collaborator.userName());
    return rosterRow("User", TagColor.CONTRAST, identity, role);
  }

  /**
   * One roster row, mirroring the sharing drawer's read-only summary: a principal-type tag, the
   * identity (avatar + name) and — when known — the granted project role on the right.
   */
  private static Component rosterRow(String typeLabel, TagColor typeColor, Component identity,
      ProjectRole role) {
    var typeTag = new Tag(typeLabel);
    typeTag.setTagColor(typeColor);
    typeTag.addClassName("psd-type-tag");
    var identityWrapper = new Div(typeTag, identity);
    identityWrapper.addClassName("psd-identity");
    var row = new Div(identityWrapper);
    if (role != null) {
      var roleTag = new Tag(ProjectSharingComposer.roleLabel(role));
      roleTag.setTagColor(ProjectSharingComposer.roleColor(role));
      roleTag.addClassName("psd-role-tag");
      row.add(roleTag);
    }
    row.addClassName("psd-summary-row");
    return row;
  }

  private static Span separator() {
    var separator = new Span("·");
    separator.addClassName("project-card-access-separator");
    return separator;
  }

  private static String buildTooltip(List<SharedProjectGroup> groups,
      Collection<UserInfo> collaborators) {
    var parts = new java.util.ArrayList<String>();
    if (!groups.isEmpty()) {
      parts.add("Groups: " + groups.stream().map(SharedProjectGroup::groupName)
          .collect(Collectors.joining(", ")));
    }
    if (!collaborators.isEmpty()) {
      parts.add("People: " + collaborators.stream().map(UserInfo::userName)
          .collect(Collectors.joining(", ")));
    }
    return String.join("\n", parts);
  }
}
