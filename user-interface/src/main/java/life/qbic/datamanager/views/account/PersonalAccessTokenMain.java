package life.qbic.datamanager.views.account;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.AddTokenEvent;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.DeleteTokenEvent;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.PersonalAccessTokenDTO;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.projects.project.samples.BatchDeletionConfirmationNotification;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.DeleteBatchEvent;
import life.qbic.identity.api.PersonalAccessToken;
import life.qbic.identity.api.PersonalAccessTokenService;
import life.qbic.identity.api.RawToken;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
    AccessTokenDeletionConfirmationNotification tokenDeletionConfirmationNotification = new AccessTokenDeletionConfirmationNotification();
    tokenDeletionConfirmationNotification.open();
    tokenDeletionConfirmationNotification.addConfirmListener(event -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      QbicUserDetails details = (QbicUserDetails) authentication.getPrincipal();
      personalAccessTokenService.delete(deleteTokenEvent.tokenId(), details.getUserId());
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
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      QbicUserDetails details = (QbicUserDetails) authentication.getPrincipal();
      RawToken createdToken = personalAccessTokenService.create(details.getUserId(),
          event.personalAccessTokenDTO()
              .tokenDescription(), event.personalAccessTokenDTO().expirationDate());
      personalAccessTokenComponent.showCreatedToken(createdToken);
      event.getSource().close();
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
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    QbicUserDetails details = (QbicUserDetails) authentication.getPrincipal();
    List<PersonalAccessTokenDTO> personalAccessTokenDTOs = new ArrayList<>();
    Collection<PersonalAccessToken> personalAccessTokens = personalAccessTokenService.find(
        details.getUserId());
    personalAccessTokens.forEach(token -> personalAccessTokenDTOs.add(
        new PersonalAccessTokenDTO(token.tokenId(), token.description(), Duration.between(
            Instant.now(), token.expiration()), token.expired())));
    personalAccessTokenComponent.setTokens(personalAccessTokenDTOs);
  }
}
