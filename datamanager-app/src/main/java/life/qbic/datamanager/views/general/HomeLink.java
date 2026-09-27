package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouterLink;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;

/**
 * Home navigation affordance for the application's top navigation bar.
 * <p>
 * Renders a {@link RouterLink} to the logged-in user's home page,
 * {@link ProjectOverviewMain} (route {@code projects/list}), displayed as a home icon.
 * <p>
 * The link is placed at the left side of the navbar, next to the brand title, so users can
 * always return to the project overview with a single, predictable gesture. It is a real
 * {@code <a>} element rendered by {@link RouterLink}: keyboard focusable, announced as
 * "Go to home page" via {@code aria-label}, and highlighted by Vaadin's router-link active
 * state when the user is already on the overview page.
 */
public class HomeLink extends RouterLink {

  private static final String HOME_LINK_ARIA_LABEL = "Go to home page";

  public HomeLink() {
    super(ProjectOverviewMain.class);
    add(new Icon(VaadinIcon.HOME));
    addClassName("home-link");
    getElement().setAttribute("aria-label", HOME_LINK_ARIA_LABEL);
  }
}