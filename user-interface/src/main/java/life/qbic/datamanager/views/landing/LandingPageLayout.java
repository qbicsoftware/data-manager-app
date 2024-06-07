package life.qbic.datamanager.views.landing;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLayout;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.LandingPageTitleAndLogo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The landing page that allows logging in for the user. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager")
public class LandingPageLayout extends DataManagerLayout implements RouterLayout {

  @Serial
  private static final long serialVersionUID = 8899881833038660866L;
  public Button register;
  public Button login;
  private final Div landingPageContent = new Div();
  private LandingPageTitleAndLogo landingPageTitleAndLogo = new LandingPageTitleAndLogo();

  public LandingPageLayout(@Autowired LandingPageHandlerInterface handlerInterface) {
    Objects.requireNonNull(handlerInterface);
    setId("landing-page-layout");
    //CSS class hosting the background image for all our landing pages
    landingPageContent.addClassName("landing-page-content");
    createNavBarContent();
    registerToHandler(handlerInterface);
  }

  private void registerToHandler(LandingPageHandlerInterface handler) {
    handler.handle(this);
  }

  private void createNavBarContent() {
    Span dataManagerTitle = new Span("Data Manager");
    dataManagerTitle.setClassName("navbar-title");
    addToNavbar(dataManagerTitle, createHeaderButtonLayout());
  }

  private HorizontalLayout createHeaderButtonLayout() {
    register = new Button("Register");
    login = new Button("Login");

    HorizontalLayout loggedOutButtonLayout = new HorizontalLayout(register, login);
    loggedOutButtonLayout.addClassName("button-layout-spacing");

    styleHeaderButtons();

    return loggedOutButtonLayout;
  }

  private void styleHeaderButtons() {
    login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

  /**
   * {@inheritDoc}
   *
   * @param content
   * @throws IllegalArgumentException if content is not a {@link Component}
   */
  @Override
  public void showRouterLayoutContent(HasElement content) {
    landingPageContent.removeAll();
    //Ensures that the data manager title und UT Logo is always present in this layout
    landingPageContent.getElement().appendChild(landingPageTitleAndLogo.getElement());
    landingPageContent.getElement().appendChild(content.getElement());
    super.showRouterLayoutContent(landingPageContent);
  }
}
