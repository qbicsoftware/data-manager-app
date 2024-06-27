package life.qbic.datamanager.views.register;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationResponse;
import life.qbic.datamanager.exceptionhandling.ErrorMessageTranslationService;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.MainPage;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route("register/oidc")
@PermitAll
public class RegisterOpenIdConnect extends AppLayout implements BeforeEnterObserver,
    BeforeLeaveObserver {

  private final TextField username;
  private final TextField fullName;
  private final TextField email;
  private final Div errorArea;
  
  private static final Logger log = logger(RegisterOpenIdConnect.class);
  private final transient IdentityService identityService;
  private final transient ErrorMessageTranslationService errorMessageTranslationService;


  public RegisterOpenIdConnect(
      @Qualifier("userRegistrationService") IdentityService identityService,
      @Autowired ErrorMessageTranslationService errorMessageTranslationService) {
    requireNonNull(errorMessageTranslationService,
        "errorMessageTranslationService must not be null");
    this.identityService = requireNonNull(identityService, "identityService must not be null");
    this.errorMessageTranslationService = errorMessageTranslationService;
    Div content = new Div();
    fullName = new TextField("Full Name");
    fullName.setRequired(true);
    username = new TextField("Username");
    username.setRequired(true);
    email = new TextField("Email");
    email.setRequired(true);
    Button submit = new Button("Submit");
    submit.addClickListener(
        clickedEvent -> registerOidcUser(fullName.getValue(), username.getValue(),
            email.getValue()));
    errorArea = new Div();
    errorArea.setVisible(false);
    content.add(new FormLayout(fullName, username, email, submit), errorArea);
    setContent(content);
  }

  private void registerOidcUser(String fullName, String username, String email) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof QbicOidcUser) {
      return; //nothing to do
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      ApplicationResponse registrationResponse = identityService.registerOpenIdUser(fullName,
          username, email, oidcUser.getIssuer().toString(),
          oidcUser.getName());
      resetErrors();
      registrationResponse.ifSuccessOrElse(
          this::onSuccessfulOidcUserRegistration,
          this::onFailedOidcUserRegistration
      );
      return;
    }
    UI.getCurrent().navigate(
        Projects.PROJECTS); // no user id loaded as authentication principal is not overwritten.
  }

  private void onFailedOidcUserRegistration(ApplicationResponse failure) {
    String allErrorMessages = failure.failures().stream().map(Throwable::getMessage)
        .collect(Collectors.joining("\n"));
    List<Component> errorMessages = failure.failures().stream()
        .map(errorMessageTranslationService::translate)
        .map(userFriendlyErrorMessage -> {
          Div errorDiv = new Div();
          errorDiv.add(new Span(userFriendlyErrorMessage.title() + ": "),
              new Span(userFriendlyErrorMessage.message()));
          return (Component) errorDiv;
        }).toList();
    errorArea.add(errorMessages);
    errorArea.setVisible(true);
    log.error(allErrorMessages);
    failure.failures().forEach(e -> log.debug(e.getMessage(), e));
  }

  private void resetErrors() {
    errorArea.setVisible(false);
    errorArea.removeAll();
  }

  private void onSuccessfulOidcUserRegistration(ApplicationResponse success) {
    errorArea.removeAll();
    errorArea.setVisible(false);
    UI.getCurrent().navigate(PleaseConfirmEmailPage.class);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof QbicOidcUser qbicOidcUser) {
      log.warn("User %s tried to enter registration although exists.".formatted(
          qbicOidcUser.getQbicUserId()));
      event.rerouteTo(MainPage.class);
      return;
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      //note: for ORCiD, only given name and family name are supported https://orcid.org/.well-known/openid-configuration.
      this.fullName.setValue(buildFullName(oidcUser.getGivenName(), oidcUser.getMiddleName(),
          oidcUser.getFamilyName()));
      Optional.ofNullable(oidcUser.getEmail()).ifPresent(this.email::setValue);
      Optional.ofNullable(oidcUser.getPreferredUsername()).ifPresent(this.username::setValue);
    }
  }

  private static String buildFullName(String givenName, String middleName, String familyName) {
    return "%s%s%s".formatted(
        isNull(givenName) ? "" : givenName,
        isNull(middleName) ? "" : " " + middleName,
        isNull(familyName) ? "" : " " + familyName
    );
  }

  @Override
  public void beforeLeave(BeforeLeaveEvent event) {
    SecurityContextHolder.getContext().setAuthentication(null); // remove authentication
  }
}
