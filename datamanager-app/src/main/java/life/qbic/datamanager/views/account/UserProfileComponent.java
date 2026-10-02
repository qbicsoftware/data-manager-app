package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.server.VaadinService;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.profilepicture.ProfilePictureDialog;
import life.qbic.datamanager.profilepicture.ProfilePictureMessages;
import life.qbic.datamanager.profilepicture.ProfilePictureOwnerType;
import life.qbic.datamanager.profilepicture.ProfilePictureService;
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
 * Flat, group-based profile view showing the user's personal information and linked accounts.
 * Semantic groups ("Personal information", "Linked accounts") are separated by subheadings and
 * whitespace only — no card chrome. Username editing is inline via {@link InlineEditableField}.
 */
public class UserProfileComponent extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = -65339437186530376L;
  private static final Logger log = logger(UserProfileComponent.class);
  private final transient IdentityService identityService;
  private final transient ProfilePictureService profilePictureService;
  private final UserInfo userInfo;
  private final Location currentLocation;
  private final transient Consumer<String> usernameChangedListener;
  private UserAvatar profileAvatar;
  private Button removePictureButton;

  /**
   * @param usernameChangedListener invoked with the new username after a successful change, so
   *                                the surrounding layout can refresh dependent UI (e.g. the
   *                                account overview header) without a page reload
   */
  public UserProfileComponent(IdentityService identityService,
      ProfilePictureService profilePictureService,
      UserInfo userInfo,
      Location currentLocation,
      Consumer<String> usernameChangedListener) {
    this.identityService = requireNonNull(identityService,
        "identity service cannot be null");
    this.profilePictureService = requireNonNull(profilePictureService,
        "profile picture service cannot be null");
    this.userInfo = requireNonNull(userInfo, "userInfo must not be null");
    this.currentLocation = requireNonNull(currentLocation);
    this.usernameChangedListener = requireNonNull(usernameChangedListener,
        "usernameChangedListener must not be null");
    addClassName("user-profile-component");
    render();
  }

  private void render() {
    add(buildPersonalInformationGroup(), buildLinkedAccountsGroup());
  }

  // ── Personal information group ────────────────────────────────

  private Div buildPersonalInformationGroup() {
    var group = settingsGroup("Personal information");

    var fields = new Div();
    fields.addClassName("personal-information__fields");
    fields.add(buildUsernameField());
    fields.add(buildEmailRow());

    var layout = new Div(buildProfilePictureBlock(), fields);
    layout.addClassName("personal-information__layout");

    group.add(layout);
    return group;
  }

  private Div buildProfilePictureBlock() {
    var block = new Div();
    block.addClassName("profile-picture-block");

    var label = new Span("Profile picture");
    label.addClassName("profile-picture-block__label");

    profileAvatar = new UserAvatar();
    profileAvatar.setUserId(userInfo.id());
    profileAvatar.addClassName("profile-picture-block__avatar");

    var changeButton = new Button("Change picture");
    changeButton.addClassName("tertiary");
    changeButton.addClickListener(event -> openPictureDialog());

    removePictureButton = new Button("Remove picture");
    removePictureButton.addClassNames("tertiary", "profile-picture-row__remove");
    removePictureButton.setVisible(hasProfilePicture());
    removePictureButton.addClickListener(event -> removeProfilePicture());

    var actions = new Div(changeButton, removePictureButton);
    actions.addClassName("profile-picture-block__actions");

    block.add(label, profileAvatar, actions);
    return block;
  }

  private boolean hasProfilePicture() {
    return profilePictureService.findContentHash(ProfilePictureOwnerType.USER, userInfo.id())
        .isPresent();
  }

  private void openPictureDialog() {
    var dialog = new ProfilePictureDialog();
    dialog.addPictureSelectedListener(png -> saveProfilePicture(png, dialog));
    dialog.open();
  }

  private void saveProfilePicture(byte[] png, ProfilePictureDialog dialog) {
    try {
      var result = profilePictureService.setUserPicture(userInfo.id(), png);
      if (result.isError()) {
        dialog.showError(ProfilePictureMessages.userMessage(result.getError()));
        return;
      }
    } catch (RuntimeException e) {
      log.warn("Saving profile picture failed for user " + userInfo.id() + ": " + e.getMessage());
      dialog.showError("The picture could not be saved. Please try again.");
      return;
    }
    dialog.close();
    refreshProfileAvatar();
  }

  private void removeProfilePicture() {
    var result = profilePictureService.removeUserPicture(userInfo.id());
    if (result.isError()) {
      log.warn("Could not remove profile picture for user " + userInfo.id() + ": "
          + result.getError().getMessage());
      return;
    }
    refreshProfileAvatar();
  }

  private void refreshProfileAvatar() {
    profileAvatar.refresh(ProfilePictureOwnerType.USER, userInfo.id());
    removePictureButton.setVisible(hasProfilePicture());
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
      // Refresh the account overview header and other username references in place
      usernameChangedListener.accept(newName);
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

  /**
   * The email address is rendered as plain text: it is not editable, so it must not carry any
   * edit affordance (no underlined input look). The label column reuses the inline-editable-field
   * styles so both rows align.
   */
  private Div buildEmailRow() {
    var row = new Div();
    row.addClassName("inline-editable-field");

    var label = new Span("Email:");
    label.addClassName("inline-editable-field__label");

    var value = new Span(userInfo.emailAddress());
    value.addClassName("inline-editable-field__value");

    row.add(label, value);
    return row;
  }

  // ── Linked accounts group ─────────────────────────────────────

  private Div buildLinkedAccountsGroup() {
    var group = settingsGroup("Linked accounts");

    if (userInfo.oidcId() == null || userInfo.oidcIssuer() == null
        || userInfo.oidcIssuer().isEmpty() || userInfo.oidcId().isEmpty()) {
      group.add(buildLinkWithOrcidBlock());
    } else {
      Arrays.stream(OidcType.values())
          .filter(ot -> ot.getIssuer().equals(userInfo.oidcIssuer()))
          .findFirst()
          .ifPresentOrElse(
              oidcType -> group.add(generateLinkedAccountBox(userInfo)),
              () -> log.warn("No issuer was found for OIDC type " + userInfo.oidcIssuer()));
    }

    return group;
  }

  private Div buildLinkWithOrcidBlock() {
    var block = new Div();
    block.addClassName("link-account-block");

    var explanation = new Paragraph(
        "Link your ORCiD account to sign in with ORCiD and connect your public researcher record "
            + "with this account.");
    explanation.addClassName("link-account-block__explanation");

    var contextPath = VaadinService.getCurrentRequest().getContextPath();
    var returnTo = URLEncoder.encode(currentLocation.getPath(), StandardCharsets.UTF_8);

    var linkAccount = new Anchor(
        contextPath + OidcLinkController.ENDPOINT_LINK_ORCID + "?return=" + returnTo,
        "Link ORCiD account");
    linkAccount.setTarget(AnchorTarget.SELF);
    linkAccount.setRouterIgnore(true);
    // Render the anchor as a primary button so the call to action is discoverable
    linkAccount.getElement().setAttribute("theme", "button primary");

    block.add(explanation, linkAccount);
    return block;
  }

  private Div generateLinkedAccountBox(UserInfo userInfo) {
    return new AccountContentBox(userInfo.oidcId(),
        URI.create(generateOidCRecordURL(userInfo.oidcId())));
  }

  private String generateOidCRecordURL(String oidcId) {
    return String.format("https://orcid.org/%s", oidcId);
  }

  // ── Shared group scaffolding ──────────────────────────────────

  private static Div settingsGroup(String title) {
    var group = new Div();
    group.addClassName("settings-group");
    var heading = new H3(title);
    heading.addClassName("settings-group__title");
    group.add(heading);
    return group;
  }
}
