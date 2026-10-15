package life.qbic.datamanager.views.groups;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b>Groups Overview Header</b>
 * <p>
 * The identity block at the top of the Groups hub: a group-coloured icon, the area title
 * ("Groups") and a live count of the caller's memberships ("3 groups · 1 owned"). It heads the
 * whole hub in its own full-width row, above the navigation and the routed section, mirroring the
 * settings hub's account overview header — the header is what anchors the page instead of the
 * content starting flush at the top of the shell.
 * <p>
 * The count is informational and reflects the current user's memberships only (the numbers come
 * from {@code GroupInformationService.listMyGroups}). Membership is never exposed for other users.
 *
 * @since 1.19.0
 */
public class GroupsOverviewHeader extends Div {

  private final Span titleElement;
  private final Span countElement;

  /**
   * Creates the groups overview header.
   *
   * @param totalGroups the number of groups the caller belongs to, must not be negative
   * @param ownedGroups the number of groups the caller owns (ad-hoc groups only), must not be
   *                    negative and must not exceed {@code totalGroups}
   */
  public GroupsOverviewHeader(int totalGroups, int ownedGroups) {
    validateCounts(totalGroups, ownedGroups);
    addClassName("groups-overview-header");
    // One labelled unit for assistive technology, so the identity and its count are announced
    // together and stay out of the heading outline (the routed section carries the heading).
    getElement().setAttribute("role", "group");
    getElement().setAttribute("aria-label", "Groups");

    Div iconWrapper = new Div(new Icon(VaadinIcon.USERS));
    iconWrapper.addClassName("groups-overview-header__icon");
    // Decorative: the title and count carry the meaning; the icon is not announced separately.
    iconWrapper.getElement().setAttribute("aria-hidden", "true");

    titleElement = new Span("Groups");
    titleElement.addClassName("groups-overview-header__title");
    countElement = new Span();
    countElement.addClassName("groups-overview-header__count");

    Div texts = new Div(titleElement, countElement);
    texts.addClassName("groups-overview-header__texts");

    add(iconWrapper, texts);
    setCounts(totalGroups, ownedGroups);
  }

  /**
   * Updates the live count in place (e.g. after a group is created, joined or left).
   *
   * @param totalGroups the number of groups the caller belongs to, must not be negative
   * @param ownedGroups the number of groups the caller owns, must not be negative and must not
   *                    exceed {@code totalGroups}
   */
  public void setCounts(int totalGroups, int ownedGroups) {
    validateCounts(totalGroups, ownedGroups);
    countElement.setText(countText(totalGroups, ownedGroups));
  }

  private static void validateCounts(int totalGroups, int ownedGroups) {
    if (totalGroups < 0 || ownedGroups < 0) {
      throw new IllegalArgumentException("group counts must not be negative");
    }
    if (ownedGroups > totalGroups) {
      throw new IllegalArgumentException("owned groups cannot exceed total groups");
    }
  }

  /**
   * Renders the secondary count line for the given membership numbers.
   *
   * @param totalGroups the number of groups the caller belongs to
   * @param ownedGroups the number of groups the caller owns
   * @return a human-readable count, e.g. {@code "3 groups · 1 owned"} or {@code "No groups"}
   */
  static String countText(int totalGroups, int ownedGroups) {
    if (totalGroups == 0) {
      return "No groups";
    }
    String total = totalGroups + (totalGroups == 1 ? " group" : " groups");
    if (ownedGroups == 0) {
      return total;
    }
    return total + " · " + ownedGroups + " owned";
  }

  /**
   * The title element, for unit tests.
   */
  Span titleForTest() {
    return titleElement;
  }

  /**
   * The count element, for unit tests.
   */
  Span countForTest() {
    return countElement;
  }
}