package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.AddTokenEvent;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.DeleteTokenEvent;
import life.qbic.datamanager.views.account.PersonalAccessTokenComponent.PersonalAccessTokenFrontendBean;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.settings.SettingsMainLayout;
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
 * create and delete {@link PersonalAccessToken} via the provided UI elements.
 */
@Route(value = "settings/api-tokens", layout = SettingsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Settings · API Tokens")
public class PersonalAccessTokenMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = -7876265792987169498L;
  private static final Logger log = LoggerFactory.logger(PersonalAccessTokenMain.class);
  private final PersonalAccessTokenComponent personalAccessTokenComponent;
  private final transient PersonalAccessTokenService personalAccessTokenService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final transient MessageSourceNotificationFactory messageSourceNotificationFactory;

  public PersonalAccessTokenMain(PersonalAccessTokenService personalAccessTokenService,
      PersonalAccessTokenComponent personalAccessTokenComponent,
      AuthenticationToUserIdTranslationService userIdTranslator,
      MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.personalAccessTokenService = requireNonNull(personalAccessTokenService,
        "personalAccessTokenService must not be null");
    this.personalAccessTokenComponent = requireNonNull(personalAccessTokenComponent,
        "personalAccessTokenComponent must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");
    this.messageSourceNotificationFactory = requireNonNull(messageSourceNotificationFactory,
        "messageSourceToastFactory must not be null");

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
    AlertDialog.danger(this,
        "Personal Access Token will be revoked",
        "Revoking this token will make it unusable for API access. "
            + "Any applications using this token will stop working immediately.",
        "Revoke token",
        "Keep token",
        () -> {
          var userId = userIdTranslator.translateToUserId(
                  SecurityContextHolder.getContext().getAuthentication())
              .orElseThrow();
          personalAccessTokenService.delete(deleteTokenEvent.tokenId(), userId);
          loadGeneratedPersonalAccessTokens();
        }).open();
  }

  private void onAddTokenClicked(AddTokenEvent addTokenEvent) {
    var dialog = new AddPersonalAccessTokenDialog();
    dialog.addCancelListener(event -> dialog.close());
    dialog.addConfirmListener(event -> loadGeneratedPersonalAccessTokens());
    dialog.addConfirmListener(event -> {
      var userId = userIdTranslator.translateToUserId(
              SecurityContextHolder.getContext().getAuthentication())
          .orElseThrow();
      RawToken createdToken = personalAccessTokenService.create(userId,
          event.personalAccessTokenDTO().tokenDescription(),
          event.personalAccessTokenDTO().expirationDuration());
      personalAccessTokenComponent.showCreatedToken(createdToken);
      dialog.close();
      Toast toast = messageSourceNotificationFactory.toast(
          "personal-access-token.created.success",
          new Object[]{event.personalAccessTokenDTO().tokenDescription()},
          getLocale());
      toast.open();
    });
    dialog.open();
  }

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
