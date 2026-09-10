package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.server.VaadinService;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.OidcLinkController;
import life.qbic.datamanager.views.general.InlineEditableField;
import life.qbic.datamanager.views.general.InlineEditableField.CancelEvent;
import life.qbic.datamanager.views.general.InlineEditableField.SaveEvent;
import life.qbic.datamanager.views.general.oidc.OidcType;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException;
import life.qbic.logging.api.Logger;

/**
 * User Profile Component
 * <p>
 * Card-based profile view showing the user's personal information and linked accounts.
 * Username editing is inline via {@link InlineEditableField}. No dialog overhead for
 * a single-field change.
 */
public class UserProfileComponent extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = -65339437186530376L;
  private static final Logger log = logger(UserProfileComponent.class);
  private final transient IdentityService identityService;
  private final UserInfo userInfo;
  private final Location currentLocation;

  public UserProfileComponent(IdentityService identityService,
      UserInfo userInfo,
      Location currentLocation) {
    this.identityService = requireNonNull(identityService,
        "identity service cannot be null");
    this.userInfo = requireNonNull(userInfo, "userInfo must not be null");
    this.currentLocation = requireNonNull(currentLocation);
    addClassName("user-profile-component");
    render();
  }

  private void render() {
    add(buildProfileInfoCard(), buildLinkedAccountsCard());
  }

  // ── Profile Information Card ──────────────────────────────────

  private Div buildProfileInfoCard() {
    var card = new Div();
    card.addClassName("profile-info-card");

    // Header row: avatar + full name
    var header = new Div();
    header.addClassName("profile-info-card__header");

    UserAvatar userAvatar = new UserAvatar();
    userAvatar.setName(userInfo.platformUserName());
    userAvatar.setUserId(userInfo.id());
    userAvatar.addClassName("profile-info-card__avatar");

    Span fullName = new Span(userInfo.fullName());
    fullName.addClassName("profile-info-card__name");

    header.add(userAvatar, fullName);
    card.add(header);

    // Details: username (inline editable) + email (display-only)
    var details = new Div();
    details.addClassName("profile-info-card__details");
    details.add(buildUsernameField());
    details.add(buildEmailField());
    card.add(details);

    return card;
  }

  private InlineEditableField buildUsernameField() {
    var field = new InlineEditableField("Username", userInfo.platformUserName());

    field.addSaveListener(this::onUsernameSave);
    field.addCancelListener(e -> { /* nothing to do — component handles UI revert */ });

    return field;
  }

  private void onUsernameSave(SaveEvent event) {
    String newName = event.value();
    if (newName.isEmpty()) {
      event.getSource().setError("Please provide a non-empty username");
      return;
    }

    var response = identityService.requestUserNameChange(userInfo.id(), newName);
    if (response.isSuccess()) {
      event.getSource().setValue(newName);
      event.getSource().cancelEditing();
      // Reload the settings page to refresh the header and all username references
      UI.getCurrent().getPage().reload();
      return;
    }

    RuntimeException e = response.failures().stream().findFirst().orElseThrow();
    if (e instanceof UserNameNotAvailableException) {
      event.getSource().setError("Username \"" + newName + "\" is not available");
      return;
    }
    if (e instanceof EmptyUserNameException) {
      event.getSource().setError("Please provide a non-empty username");
      return;
    }
    throw ApplicationException.wrapping("Unexpected exception in username change.", e);
  }

  private InlineEditableField buildEmailField() {
    var field = new InlineEditableField("Email", userInfo.emailAddress());
    field.setEditable(false);
    return field;
  }

  // ─ Linked Accounts Card ───────────────────────────────────────

  private Div buildLinkedAccountsCard() {
    var card = new Div();
    card.addClassName("profile-info-card");
    card.addClassName("profile-info-card--linked-accounts");

    var header = new Div();
    header.addClassName("profile-info-card__subheader");
    header.add(new Span("Linked Accounts"));
    card.add(header);

    if (userInfo.oidcId() == null || userInfo.oidcIssuer() == null
        || userInfo.oidcIssuer().isEmpty() || userInfo.oidcId().isEmpty()) {
      card.add(linkWithOrcidCard());
    } else {
      Arrays.stream(OidcType.values())
          .filter(ot -> ot.getIssuer().equals(userInfo.oidcIssuer()))
          .findFirst()
          .ifPresentOrElse(
              oidcType -> card.add(generateLinkedAccountCard(userInfo)),
              () -> log.warn("No issuer was found for OIDC type " + userInfo.oidcIssuer()));
    }

    return card;
  }

  private Div linkWithOrcidCard() {
    var linkAccountCard = new Div();
    linkAccountCard.addClassName("profile-info-card__link-account");

    var contextPath = VaadinService.getCurrentRequest().getContextPath();
    var returnTo = URLEncoder.encode(currentLocation.getPath(), StandardCharsets.UTF_8);

    var linkAccount = new Anchor("", "Link ORCiD account");
    linkAccount.setTarget(AnchorTarget.SELF);
    linkAccount.setHref(contextPath + OidcLinkController.ENDPOINT_LINK_ORCID + "?return=" + returnTo);
    linkAccount.setRouterIgnore(true);
    linkAccountCard.add(linkAccount);
    return linkAccountCard;
  }

  private Div generateLinkedAccountCard(UserInfo userInfo) {
    return new AccountContentBox(userInfo.oidcId(), URI.create(generateOidCRecordURL(userInfo.oidcId())));
  }

  private String generateOidCRecordURL(String oidcId) {
    return String.format("https://orcid.org/%s", oidcId);
  }
}
