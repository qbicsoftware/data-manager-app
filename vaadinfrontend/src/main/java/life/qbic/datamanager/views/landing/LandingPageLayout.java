package life.qbic.datamanager.views.landing;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.components.OfferSearchDialog;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The landing page that allows logging in for the user. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager")
@Route(value = "landing")
public class LandingPageLayout extends DataManagerLayout {

  @Serial
  private static final long serialVersionUID = 8899881833038660866L;

  public Button register;
  public Button login;
  private final OfferLookupService offerLookupService;

  public LandingPageLayout(@Autowired LandingPageHandlerInterface handlerInterface,
      @Autowired OfferLookupService offerLookupService) {
    System.out.println("Initialized landing page");
    Objects.requireNonNull(handlerInterface);
    Objects.requireNonNull(offerLookupService);
    this.offerLookupService = offerLookupService;

    createNavBarContent();
    registerToHandler(handlerInterface);

  }

  private void registerToHandler(LandingPageHandlerInterface handler) {
    handler.handle(this);
  }

  private void createNavBarContent() {

    addToNavbar(createHeaderButtonLayout());
  }

  private HorizontalLayout createHeaderButtonLayout() {
    register = new Button("Register");
    login = new Button("Login");
    //todo remove
    OfferSearchDialog dialog = new OfferSearchDialog(offerLookupService);
    dialog.open();

    HorizontalLayout loggedOutButtonLayout = new HorizontalLayout(register, login);
    loggedOutButtonLayout.addClassName("button-layout-spacing");

    styleHeaderButtons();

    return loggedOutButtonLayout;
  }

  private void styleHeaderButtons() {
    login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }
}
