package life.qbic.datamanager.views.groups;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import life.qbic.usergroups.api.GroupRole;
import life.qbic.usergroups.api.GroupType;
import life.qbic.usergroups.api.MyGroupMembership;

/**
 * <b>My Groups summary panel</b>
 * <p>
 * Compact dashboard panel for the project overview showing the caller's group memberships: each
 * group as a clickable card (name, type badge, role badge and member count) linking to its detail
 * page ({@code groups/:groupId}), plus a "Go to My Groups" shortcut to the groups hub
 * ({@code groups}).
 * <p>
 * The panel is hidden entirely while the caller belongs to no group, so the project overview looks
 * exactly as before for users without groups. Data access stays behind a {@link Supplier} seam so
 * the component is unit-testable without a Spring or Vaadin {@code UI} context, mirroring
 * {@code PinnedProjectsComponent}.
 *
 * @since 1.19.0
 */
public class MyGroupSummaryComponent extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = 6427986140212857484L;

  private final transient Supplier<List<MyGroupMembership>> membershipsSupplier;
  private final Div cards = new Div();

  /**
   * Creates a new my-groups summary panel.
   *
   * @param membershipsSupplier supplies the caller's current memberships; must not be {@code null}
   */
  public MyGroupSummaryComponent(Supplier<List<MyGroupMembership>> membershipsSupplier) {
    this.membershipsSupplier = Objects.requireNonNull(membershipsSupplier,
        "membershipsSupplier must not be null");
    addClassName("my-group-summary");
    setVisible(false);
    Span title = new Span("My Groups");
    title.addClassName("my-group-summary__title");
    RouterLink goToMyGroups = new RouterLink("Go to My Groups", MyGroupsMain.class);
    goToMyGroups.addClassName("my-group-summary__goto");
    cards.addClassName("my-group-summary__cards");
    Div header = new Div();
    header.addClassName("my-group-summary__header");
    header.add(title, goToMyGroups);
    add(header, cards);
    refresh();
  }

  /**
   * Re-runs the membership supplier and re-renders the panel. Hidden while the caller has no
   * groups, so the overview stays undisturbed for users without memberships.
   */
  public final void refresh() {
    List<MyGroupMembership> memberships = membershipsSupplier.get();
    cards.removeAll();
    memberships.forEach(this::addCard);
    setVisible(!memberships.isEmpty());
  }

  private void addCard(MyGroupMembership membership) {
    RouterLink card = new RouterLink("", GroupDetailMain.class,
        new RouteParameters(new RouteParam(GroupDetailMain.GROUP_ID_ROUTE_PARAMETER,
            membership.groupId())));
    card.addClassName("my-group-summary__card");
    card.add(buildCardBody(membership));
    cards.add(card);
  }

  private Div buildCardBody(MyGroupMembership membership) {
    Div body = new Div();
    body.addClassName("my-group-summary__card-body");
    Div line = new Div();
    line.addClassName("my-group-summary__card-line");
    Span name = new Span(membership.groupName());
    name.addClassName("my-group-summary__card-name");
    line.add(name, buildTypeBadge(membership.groupType()), buildRoleBadge(membership.myRole()),
        buildMemberCountBadge(membership.memberCount()));
    body.add(line);
    if (membership.groupDescription() != null && !membership.groupDescription().isBlank()) {
      Span description = new Span(membership.groupDescription());
      description.addClassName("my-group-summary__card-description");
      body.add(description);
    }
    return body;
  }

  private static Span buildTypeBadge(GroupType type) {
    Span badge = new Span(type == GroupType.ORG ? "Org" : "User Group");
    badge.addClassName("my-group-summary__badge");
    badge.addClassName(
        type == GroupType.ORG ? "my-group-summary__badge--org" : "my-group-summary__badge--adhoc");
    return badge;
  }

  private static Span buildRoleBadge(GroupRole role) {
    String label = role == GroupRole.OWNER ? "Owner"
        : role == GroupRole.MANAGER ? "Manager" : "Member";
    Span badge = new Span(label);
    badge.addClassName("my-group-summary__badge");
    badge.addClassName("my-group-summary__badge--role");
    return badge;
  }

  private static Span buildMemberCountBadge(int memberCount) {
    Span badge = new Span(memberCount + " member" + (memberCount == 1 ? "" : "s"));
    badge.addClassName("my-group-summary__badge");
    badge.addClassName("my-group-summary__badge--count");
    return badge;
  }
}