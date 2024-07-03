package life.qbic.datamanager.views.register;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
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
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.MainPage;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.register.UserRegistrationOrcIdComponent.UserRegistrationOrcIdInformation;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserExistsException;
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route(value = AppRoutes.REGISTER_OIDC, layout = LandingPageLayout.class)
@PermitAll
public class RegistrationOrcIdMain extends Main implements BeforeEnterObserver,
    BeforeLeaveObserver {

  private static final Logger log = logger(RegistrationOrcIdMain.class);
  private final transient IdentityService identityService;
  private final UserRegistrationOrcIdComponent userRegistrationOrcIdComponent;

  public RegistrationOrcIdMain(
      @Qualifier("userRegistrationService") IdentityService identityService,
      @Autowired UserInformationService userInformationService) {
    this.identityService = requireNonNull(identityService, "identityService must not be null");
    userRegistrationOrcIdComponent = new UserRegistrationOrcIdComponent(userInformationService);
    userRegistrationOrcIdComponent.addRegistrationListener(
        event -> registerOidcUser(event.userRegistrationOrcIdInformation()));
    add(userRegistrationOrcIdComponent);
    addClassName("user-registration");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        userRegistrationOrcIdComponent.getClass().getSimpleName(),
        System.identityHashCode(userRegistrationOrcIdComponent)));
  }

  private static String buildFullName(String givenName, String middleName, String familyName) {
    return "%s%s%s".formatted(
        isNull(givenName) ? "" : givenName,
        isNull(middleName) ? "" : " " + middleName,
        isNull(familyName) ? "" : " " + familyName
    );
  }

  private void registerOidcUser(UserRegistrationOrcIdInformation information) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof QbicOidcUser) {
      return; //nothing to do
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      ApplicationResponse registrationResponse = identityService.registerOpenIdUser(
          information.fullName(),
          information.userName(), information.email(), oidcUser.getIssuer().toString(),
          oidcUser.getName());
      registrationResponse.ifSuccessOrElse(
          response -> UI.getCurrent().navigate(EmailConfirmationMain.class),
          response -> handleRegistrationFailure(response.failures())
      );
      return;
    }
    UI.getCurrent().navigate(
        Projects.PROJECTS); // no user id loaded as authentication principal is not overwritten.
  }

  private void handleRegistrationFailure(List<RuntimeException> exceptionList) {
    /*These Cases should not happen anymore since we validate before we send the event,
    however they can still be used as a failsafe*/
    if (exceptionList.isEmpty()) {
      return;
    }
    if (exceptionList.contains(UserExistsException.class)) {
      userRegistrationOrcIdComponent.showError("Email address already in use",
          "If you have difficulties with your password you can reset it.");
    } else if (exceptionList.contains(UserNameNotAvailableException.class)) {
      userRegistrationOrcIdComponent.showError("Username already in use",
          "Please try another username");
    } else if (exceptionList.contains(EmptyUserNameException.class)) {
      userRegistrationOrcIdComponent.showError("Username must not be empty",
          "Please try another username");
    } else {
      userRegistrationOrcIdComponent.showError("Registration failed", "Please try again.");
    }
    String allErrorMessages = exceptionList.stream().map(Throwable::getMessage)
        .collect(Collectors.joining("\n"));
    log.error(allErrorMessages);
    exceptionList.forEach(e -> log.debug(e.getMessage(), e));
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
      userRegistrationOrcIdComponent.setFullName(
          buildFullName(oidcUser.getGivenName(), oidcUser.getMiddleName(),
              oidcUser.getFamilyName()));
      Optional.ofNullable(oidcUser.getEmail()).ifPresent(userRegistrationOrcIdComponent::setEmail);
      Optional.ofNullable(oidcUser.getPreferredUsername()).ifPresent(
          userRegistrationOrcIdComponent::setUsername);
    }
  }

  @Override
  public void beforeLeave(BeforeLeaveEvent event) {
    SecurityContextHolder.getContext().setAuthentication(null); // remove authentication
  }
}
