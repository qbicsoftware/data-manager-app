package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User Profile Component
 * <p>
 * This {@link PageArea} allows the user to manage his profile. The user is able to view a tokens
 * expiration date and description. Additionally,he is able to delete and create personal access
 * tokens. Only after a personal access token is created its raw text is shown to the user with the
 * ability to copy it to the clipboard
 */

@SpringComponent
@UIScope
public class UserProfileComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = -65339437186530376L;
  private static final String TITLE = "My Profile";
  private final transient UserInformationService userInformationService;
  private final transient IdentityService identityService;
  private final UserDetailsCard userDetailsCard;
  private transient UserInfo userInfo;

  @Autowired
  public UserProfileComponent(IdentityService identityService,
      UserInformationService userInformationService) {
    this.userInformationService = Objects.requireNonNull(userInformationService,
        "user information service cannot be null");
    this.identityService = Objects.requireNonNull(identityService,
        "identity service cannot be null");
    Span title = new Span(TITLE);
    addComponentAsFirst(title);
    title.addClassName("title");
    userDetailsCard = new UserDetailsCard();
    add(userDetailsCard);
    addClassName("user-profile-component");
  }

  public void setUserDetails(String userId) {
    this.userInfo = userInformationService.findById(userId).orElseThrow();
    userDetailsCard.setUserInfo(userInfo);
  }

  private void setupChangeUserDialog() {
    ChangeUserDetailsDialog dialog = new ChangeUserDetailsDialog();
    dialog.setCurrentUserName(userInfo.platformUserName());
    dialog.addConfirmListener(event -> {
      var response = identityService.requestUserNameChange(userInfo.id(), event.userName());
      if (!response.failures().isEmpty()) {
        for (RuntimeException e : response.failures()) {
          if (e instanceof UserNameNotAvailableException) {
            dialog.setUserNameNotAvailable();
            return;
          }
          if (e instanceof EmptyUserNameException) {
            dialog.setUserNameEmpty();
            return;
          } else {
            throw new ApplicationException("Unexpected exception occurred, Please try again");
          }
        }
      }
      if (response.isSuccess()) {
        event.getSource().close();
        // Trigger reload of UI reloading the username displayed in the datamanager menu
        // and within this component
        UI.getCurrent().getPage().reload();
      }
    });
    dialog.addCancelListener(event -> event.getSource().close());
    dialog.open();
  }

  private static class UserDetail extends Div {

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

    public ChangeUserDetailsDialog() {
      super();
      setHeaderTitle("Change username");
      add(platformUserNameField);
      setConfirmButtonLabel("Save");
      addClassName("change-user-details-dialog");
      platformUserNameField.addClassName("change-user-name");
    }

    public void setCurrentUserName(String userName) {
      platformUserNameField.setValue(userName);
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

    private final Span platformUserName = new Span();
    private final UserAvatar userAvatar = new UserAvatar();
    private final Span userFullName = new Span();
    private final Span userEmail = new Span();

    public UserDetailsCard() {
      Div avatarWithName = new Div(userAvatar, userFullName);
      userAvatar.addClassName("avatar");
      userFullName.addClassName("bold");
      avatarWithName.addClassName("avatar-with-name");
      Span changePlatformUserName = new Span("Change Username");
      changePlatformUserName.addClickListener(event -> setupChangeUserDialog());
      changePlatformUserName.addClassName("change-name");
      UserDetail userNameDetail = new UserDetail("Username: ", platformUserName,
          changePlatformUserName);
      UserDetail userEmailDetail = new UserDetail("Email: ", userEmail);
      Div userDetails = new Div(userNameDetail, userEmailDetail);
      userDetails.addClassName("details");
      add(avatarWithName, userDetails);
      addClassName("user-details-card");
    }

    private void setUserInfo(UserInfo userInfo) {
      platformUserName.setText(userInfo.platformUserName());
      userEmail.setText(userInfo.emailAddress());
      userFullName.setText(userInfo.fullName());
      userAvatar.setName(userInfo.id());
    }
  }
}
