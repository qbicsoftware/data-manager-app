package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.AddTokenEvent;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.DeleteTokenEvent;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.PersonalAccessTokenFrontendBean;
import life.qbic.datamanager.views.general.Main;
import life.qbic.identity.api.PersonalAccessToken;
import life.qbic.identity.api.PersonalAccessTokenService;
import life.qbic.identity.api.RawToken;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Personal Access Token Main
 * <p>
 * This component hosts the components necessary to show the {@link PersonalAccessToken} for the
 * current logged-in User via the {@link PersonalAccessTokenComponent}. Additionally, the user can
 * create and delete {@link PersonalAccessToken} via the provided UI elements
 */

@Route(value = "personal-access-token", layout = UserMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
public class PersonalAccessTokenMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = -7876265792987169498L;
  private static final Logger log = LoggerFactory.logger(PersonalAccessTokenMain.class);
  private final PersonalAccessTokenComponent personalAccessTokenComponent;
  private final PersonalAccessTokenService personalAccessTokenService;
  private final AuthenticationToUserIdTranslationService userIdTranslator;

  public PersonalAccessTokenMain(PersonalAccessTokenService personalAccessTokenService,
      PersonalAccessTokenComponent personalAccessTokenComponent,
      AuthenticationToUserIdTranslationService userIdTranslator) {
    this.personalAccessTokenService = requireNonNull(personalAccessTokenService,
        "personalAccessTokenService must not be null");
    this.personalAccessTokenComponent = requireNonNull(personalAccessTokenComponent,
        "personalAccessTokenComponent must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");

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
    AccessTokenDeletionConfirmationNotification tokenDeletionConfirmationNotification = new AccessTokenDeletionConfirmationNotification();
    tokenDeletionConfirmationNotification.open();
    tokenDeletionConfirmationNotification.addConfirmListener(event -> {
      var userId = userIdTranslator.translateToUserId(
              SecurityContextHolder.getContext().getAuthentication())
          .orElseThrow();
      personalAccessTokenService.delete(deleteTokenEvent.tokenId(), userId);
      loadGeneratedPersonalAccessTokens();
      tokenDeletionConfirmationNotification.close();
    });
    tokenDeletionConfirmationNotification.addCancelListener(
        event -> tokenDeletionConfirmationNotification.close());
  }

  private void onAddTokenClicked(AddTokenEvent addTokenEvent) {
    AddPersonalAccessTokenDialog addPersonalAccessTokenDialog = new AddPersonalAccessTokenDialog();
    addPersonalAccessTokenDialog.open();
    addPersonalAccessTokenDialog.addCancelListener(event -> event.getSource().close());
    /*Reload the tokens to ensure that if multiple tokens are generated the previously generated tokens are shown within the list*/
    addPersonalAccessTokenDialog.addConfirmListener(event -> loadGeneratedPersonalAccessTokens());
    addPersonalAccessTokenDialog.addConfirmListener(event -> {
      var userId = userIdTranslator.translateToUserId(
              SecurityContextHolder.getContext().getAuthentication())
          .orElseThrow();
      RawToken createdToken = personalAccessTokenService.create(userId,
          event.personalAccessTokenDTO()
              .tokenDescription(), event.personalAccessTokenDTO().expirationDate());
      personalAccessTokenComponent.showCreatedToken(createdToken);
      event.getSource().closeIgnoringListeners();
    });
  }

  /**
   * Upon initialization of the main Component, the {@link PersonalAccessTokenComponent} should be
   * provided with the list of personal access tokens associated with the user
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    loadGeneratedPersonalAccessTokens();
  }

  private void loadGeneratedPersonalAccessTokens() {
    var userId = userIdTranslator.translateToUserId(
            SecurityContextHolder.getContext().getAuthentication())
        .orElseThrow();
    Collection<PersonalAccessToken> personalAccessTokens = personalAccessTokenService.findAll(
        userId);
    List<PersonalAccessTokenFrontendBean> personalAccessTokenFrontendBeans = personalAccessTokens.stream()
        .map(PersonalAccessTokenFrontendBean::from)
        .collect(Collectors.toCollection(ArrayList::new));
    personalAccessTokenComponent.setTokens(personalAccessTokenFrontendBeans);
  }
}
