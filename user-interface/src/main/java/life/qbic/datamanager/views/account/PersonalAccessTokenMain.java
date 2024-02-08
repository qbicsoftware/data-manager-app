package life.qbic.datamanager.views.account;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.DeleteTokenEvent;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.PersonalAccessTokenDTO;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.addTokenEvent;
import life.qbic.datamanager.views.general.Main;
import life.qbic.identity.api.PersonalAccessToken;
import life.qbic.identity.api.PersonalAccessTokenService;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Personal Access Token Main
 * <p>
 * This component hosts the components necessary to show the {@link PersonalAccessToken} for the
 * current logged-in User via the {@link PersonalAccessTokenComponent}. Additionally, the user can
 * create and delete {@link PersonalAccessToken} via the provided UI elements
 */

@Route(value = "personal-access-token", layout = MainLayout.class)
@SpringComponent
@UIScope
@PermitAll
public class PersonalAccessTokenMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = -7876265792987169498L;
  private static final Logger log = LoggerFactory.logger(PersonalAccessTokenMain.class);
  private final PersonalAccessTokenComponent personalAccessTokenComponent;
  private final PersonalAccessTokenService personalAccessTokenService;

  public PersonalAccessTokenMain(PersonalAccessTokenService personalAccessTokenService,
      @Autowired PersonalAccessTokenComponent personalAccessTokenComponent) {
    Objects.requireNonNull(personalAccessTokenService);
    Objects.requireNonNull(personalAccessTokenComponent);
    this.personalAccessTokenService = personalAccessTokenService;
    this.personalAccessTokenComponent = personalAccessTokenComponent;
    addClassName("personal-access-token");
    add(personalAccessTokenComponent);
    personalAccessTokenComponent.addTokenListener(this::onAddTokenClicked);
    personalAccessTokenComponent.addDeleteTokenListener(this::onDeleteTokenClicked);
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        personalAccessTokenComponent.getClass().getSimpleName(),
        System.identityHashCode(personalAccessTokenComponent)));
  }

  private void onDeleteTokenClicked(DeleteTokenEvent deleteTokenEvent) {
    //Todo Delete Token Logic with Service
  }

  private void onAddTokenClicked(addTokenEvent addTokenEvent) {
    AddPersonalAccessTokenDialog addPersonalAccessTokenDialog = new AddPersonalAccessTokenDialog();
    addPersonalAccessTokenDialog.open();
    addPersonalAccessTokenDialog.addCancelListener(event -> event.getSource().close());
    addPersonalAccessTokenDialog.addConfirmListener(event -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      QbicUserDetails details = (QbicUserDetails) authentication.getPrincipal();
      //ToDo add expiration date and description?
      personalAccessTokenService.create(details.getUserId());
      event.getSource().close();
    });
  }


  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    loadGeneratedPersonalAccessTokens();
  }

  private void loadGeneratedPersonalAccessTokens() {

    //ToDo add this once backend is ready
    /*
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    QbicUserDetails details = (QbicUserDetails) authentication.getPrincipal();
    List<PersonalAccessTokenDTO> personalAccessTokenDTOs = new ArrayList<>;
    List<PersonalAccessToken> personalAccessTokens = personalAccessTokenService.find(details.getUserId()));
    personalAccessTokens.forEach(token -> personalAccessTokenDTOs.add(new PersonalAccessTokenDTO(token.description(),
            LocalDate.ofInstant(token.expiration(), ZoneId.systemDefault()))));
    personalAccessTokenComponent.setTokens(personalAccessTokenDTOs);
    */
    personalAccessTokenComponent.setTokens(MockPersonalAccessTokenService.getTokensForUser());
  }


  //Todo Replace with real service
  @Service
  public static class MockPersonalAccessTokenService {

    public MockPersonalAccessTokenService() {
    }


    public static List<PersonalAccessTokenDTO> getTokensForUser() {
      return List.of(new PersonalAccessTokenDTO("ABC", LocalDate.now().plusDays(1)),
          new PersonalAccessTokenDTO("DEF", LocalDate.now().plusDays(2)),
          new PersonalAccessTokenDTO("GHIJ", LocalDate.now().plusDays(3)),
          new PersonalAccessTokenDTO("KLMN", LocalDate.now().plusDays(4)),
          new PersonalAccessTokenDTO("OPQR", LocalDate.now().plusDays(5)));
    }
  }

}
