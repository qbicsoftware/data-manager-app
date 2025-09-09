package life.qbic.datamanager.views.account;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.OidcLinkController;
import life.qbic.datamanager.views.account.UserProfileComponent.ChangeUserDetailsDialog.ConfirmEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;
import life.qbic.datamanager.views.projects.project.access.UserAvatarWithNameComponent;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException;
import life.qbic.logging.api.Logger;

/**
 * User Profile Component
 * <p>
 * This {@link PageArea} allows the user to manage his profile. The user is able to view a tokens
 * expiration date and description. Additionally,he is able to delete and create personal access
 * tokens. Only after a personal access token is created its raw text is shown to the user with the
 * ability to copy it to the clipboard
 */

public class UserProfileComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = -65339437186530376L;
  private static final String TITLE = "My Profile";
  private static final Logger log = logger(UserProfileComponent.class);
  private final transient IdentityService identityService;
  private final Location currentLocation;
  private UserDetailsCard userDetailsCard;

  public UserProfileComponent(IdentityService identityService,
      UserInfo userInfo,
      Location currentLocation) {
    this.identityService = requireNonNull(identityService,
        "identity service cannot be null");
    this.currentLocation = requireNonNull(currentLocation);
    Span title = new Span(TITLE);
    addComponentAsFirst(title);
    title.addClassName("title");
    addClassName("user-profile-component");
    this.setVisible(false);
    this.showForUser(userInfo);
  }

  private void showForUser(UserInfo userInfo) {
    requireNonNull(userInfo, "userInfo must not be null");
    if (nonNull(userDetailsCard)) {
      remove(userDetailsCard);
    }
    userDetailsCard = new UserDetailsCard(userInfo, OidcLinkController.ENDPOINT_LINK_ORCID);
    add(userDetailsCard);
    this.setVisible(true);
  }

  static class UserDetail extends Div {

    public UserDetail(String title, Component... components) {
      Span titleSpan = new Span(title);
      titleSpan.addClassName("bold");
      addClassName("detail");
      addComponentAsFirst(titleSpan);
      add(components);
    }
  }

  public static class ChangeUserDetailsDialog extends DialogWindow {

    private final TextField platformUserNameField = new TextField("New username");

    public ChangeUserDetailsDialog(String currentUserName) {
      super();

      setHeaderTitle("Change username");
      add(platformUserNameField);
      setConfirmButtonLabel("Save");
      addClassName("change-user-details-dialog");
      platformUserNameField.addClassName("change-user-name");
      platformUserNameField.setValue(currentUserName);
    }

    public void setUserNameNotAvailable() {
      platformUserNameField.setInvalid(true);
      platformUserNameField.setErrorMessage(
          String.format("Username %s is not available", platformUserNameField.getValue()));
    }

    public void setUserNameEmpty() {
      platformUserNameField.setInvalid(true);
      platformUserNameField.setErrorMessage("Please provide a non empty username");
    }

    /**
     * Overwrite to change what happens on confirm button clicked
     *
     * @param clickEvent
     */
    @Override
    protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
      if (!platformUserNameField.isEmpty()) {
        fireEvent(
            new ConfirmEvent(this, clickEvent.isFromClient(), platformUserNameField.getValue()));
      } else {
        setUserNameEmpty();
      }
    }

    /**
     * Overwrite to change what happens on cancel button clicked.
     *
     * @param clickEvent
     */
    @Override
    protected void onCancelClicked(ClickEvent<Button> clickEvent) {
      fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
    }

    public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
      return addListener(CancelEvent.class, listener);
    }

    public Registration addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
      return addListener(ConfirmEvent.class, listener);
    }

    public static class ConfirmEvent extends ComponentEvent<ChangeUserDetailsDialog> {

      private final String platformUserName;

      /**
       * Creates a new event using the given source and indicator whether the event originated from
       * the client side or the server side.
       *
       * @param source           the source component
       * @param fromClient       <code>true</code> if the event originated from the client
       *                         side, <code>false</code> otherwise
       * @param platformUserName The valid new platform username to be associated with the user
       */
      public ConfirmEvent(ChangeUserDetailsDialog source, boolean fromClient,
          String platformUserName) {
        super(source, fromClient);
        requireNonNull(platformUserName, "new user display name must not be null");
        this.platformUserName = platformUserName;
      }

      public String userName() {
        return platformUserName;
      }
    }

    public static class CancelEvent extends ComponentEvent<ChangeUserDetailsDialog> {

      /**
       * Creates a new event using the given source and indicator whether the event originated from
       * the client side or the server side.
       *
       * @param source     the source component
       * @param fromClient <code>true</code> if the event originated from the client
       *                   side, <code>false</code> otherwise
       */
      public CancelEvent(ChangeUserDetailsDialog source, boolean fromClient) {
        super(source, fromClient);
      }
    }
  }

  private class UserDetailsCard extends Div {

    private final UserInfo userInfo;

    private final String orcidLinkingEndpoint;

    public UserDetailsCard(UserInfo userInfo, String orcidLinkingEndpoint) {
      this.orcidLinkingEndpoint = orcidLinkingEndpoint;
      UserAvatar userAvatar = new UserAvatar();
      userAvatar.setName(userInfo.platformUserName());
      userAvatar.setUserId(userInfo.id());
      UserAvatarWithNameComponent avatarWithName = new UserAvatarWithNameComponent(userAvatar,
          userInfo.fullName());
      avatarWithName.getUserNameComponent().addClassName("bold");

      Span changePlatformUserName = new Span("Change Username");
      changePlatformUserName.addClickListener(this::onChangePlatformUserNameClicked);
      changePlatformUserName.addClassName("change-name");
      Span platformUserName = new Span();
      UserDetail userNameDetail = new UserDetail("Username: ", platformUserName,
          changePlatformUserName);
      Span userEmail = new Span();
      UserDetail userEmailDetail = new UserDetail("Email: ", userEmail);
      Div userDetails = new Div();
      userDetails.add(userNameDetail, userEmailDetail);
      userDetails.addClassName("details");
      add(avatarWithName, userDetails);
      addClassName("user-details-card");
      this.userInfo = requireNonNull(userInfo, "userInfo must not be null");
      platformUserName.setText(userInfo.platformUserName());
      userEmail.setText(this.userInfo.emailAddress());
      userAvatar.setName(this.userInfo.platformUserName());
      userAvatar.setUserId(this.userInfo.id());
      setLinkedAccounts(userDetails);
    }

    private void onChangePlatformUserNameClicked(ClickEvent<Span> event) {
      userDetailsCard.openChangeUserDialog();
    }

    private void openChangeUserDialog() {
      requireNonNull(userInfo, "userInfo must not be null");
      ChangeUserDetailsDialog dialog = new ChangeUserDetailsDialog(userInfo.platformUserName());
      dialog.addConfirmListener(this::onChangeUserDetailsDialogConfirmed);
      dialog.addCancelListener(event -> event.getSource().close());
      dialog.open();
    }

    private void onChangeUserDetailsDialogConfirmed(ConfirmEvent event) {
      var response = identityService.requestUserNameChange(userInfo.id(), event.userName());
      if (response.isSuccess()) {
        event.getSource().close();
        // Trigger reload of UI reloading the username displayed in the datamanager menu
        // and within this component
        UI.getCurrent().getPage().reload();
        return;
      }

      RuntimeException e = response.failures().stream().findFirst().orElseThrow();
      if (e instanceof UserNameNotAvailableException) {
        event.getSource().setUserNameNotAvailable();
        return;
      }
      if (e instanceof EmptyUserNameException) {
        event.getSource().setUserNameEmpty();
        return;
      }
      throw ApplicationException.wrapping("Unexpected exception in username change.", e);
    }

    private void setLinkedAccounts(Div userDetails) {
      if (userInfo.oidcId() == null || userInfo.oidcIssuer() == null) {
        userDetails.add(new UserDetail("Linked Accounts", linkWithOrcidCard()));
        return;
      }
      if (userInfo.oidcIssuer().isEmpty() || userInfo.oidcId().isEmpty()) {
        userDetails.add(new UserDetail("Linked Accounts", linkWithOrcidCard()));
        return;
      }
      Arrays.stream(OidcType.values())
          .filter(ot -> ot.getIssuer().equals(userInfo.oidcIssuer()))
          .findFirst()
          .ifPresentOrElse(oidcType -> userDetails.add(
                  new UserDetail("Linked Accounts", generateLinkedAccountCard(userInfo))),
              () -> log.warn("No issuer was found for OIDC type " + userInfo.oidcIssuer()));
    }

    private Div linkWithOrcidCard() {
      var linkAccountCard = new Div();
      var contextPath = VaadinService.getCurrentRequest().getContextPath();

      // Since we have an internal app flow only, the relative path is enough to send submit
      var returnTo = URLEncoder.encode(currentLocation.getPath(), StandardCharsets.UTF_8);

      var linkAccount = new Anchor("", "Link ORCiD account");
      linkAccount.setTarget(AnchorTarget.SELF);
      // Will call the endpoint that starts the OIDC link workflow
      linkAccount.setHref(contextPath + orcidLinkingEndpoint + "?return=" + returnTo);
      // Important to exclude the Vaadin router and call the Spring controller
      linkAccount.setRouterIgnore(true);
      linkAccountCard.add(linkAccount);
      return linkAccountCard;
    }

    private Div generateLinkedAccountCard(UserInfo userInfo) {
      return new AccountContentBox(userInfo.oidcId(), URI.create(generateOidCRecordURL(
          userInfo.oidcId())));
    }

    private String generateOidCRecordURL(String oidcId) {
      return String.format("https://orcid.org/%s", oidcId);
    }
  }

}
