package life.qbic.datamanager.views.landing;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.components.SearchDialog;
import life.qbic.finance.persistence.SimpleOfferSearchService;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <b> The landing page that allows logging in for the user. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager")
@Route(value = "landing")
public class LandingPageLayout extends DataManagerLayout {

  public Button register;
  public Button login;
  private final OfferLookupService offerLookupService;

  public LandingPageLayout(@Autowired LandingPageHandlerInterface handlerInterface, @Autowired OfferLookupService offerLookupService) {
    createNavBarContent();
    registerToHandler(handlerInterface);
    this.offerLookupService = offerLookupService;
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
    SearchDialog dialog = new SearchDialog(offerLookupService);
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
