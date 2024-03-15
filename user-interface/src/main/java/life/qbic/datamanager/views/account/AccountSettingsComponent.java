package life.qbic.datamanager.views.account;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.Objects;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;

/**
 * Contains controls to adapt settings referring to the user's account.
 */
@UIScope
@SpringComponent
public class AccountSettingsComponent extends PageArea {

  private final TextField userNameField;
  private final UserInformationService userInformationService;
  private final Button save;
  private final Button cancel;
  private String userId;
  private Registration userNameChangeListenerRegistration;

  public AccountSettingsComponent(UserInformationService userInformationService) {
    this.userInformationService = userInformationService;
    save = new Button("Save");
    save.setThemeName("primary");
    cancel = new Button("Cancel");
    userNameField = new TextField("Change your Username");
    userNameField.setValueChangeMode(ValueChangeMode.EAGER);
    // must contain at least one non-whitespace character and no leading/tailing whitespace.
    userNameField.setPattern("^\\S+(.*\\S)*$");
    save.addClickListener(this::onSaveClicked);
    cancel.addClickListener(this::onCancelClicked);
    add(userNameField, cancel, save);
  }

  private void onCancelClicked(ClickEvent<Button> buttonClickEvent) {
    requireNonNull(userId, "userId must not be null");
    UserInfo userInfo = userInformationService.findById(userId).orElseThrow();
    userNameField.setValue(userInfo.userName());
  }

  private void onSaveClicked(ClickEvent<Button> buttonClickEvent) {
    requireNonNull(userId, "userId must not be null");
    UserInfo userInfo = userInformationService.findById(userId).orElseThrow();
    String modifiedUserName = userNameField.getValue();
    UserInfo modifiedInfo = new UserInfo(userInfo.id(),
        userInfo.fullName(),
        userInfo.emailAddress(),
        modifiedUserName,
        userInfo.encryptedPassword(),
        userInfo.isActive());
    userInformationService.updateUserInformation(userId, modifiedInfo);
    setSavedUserName(modifiedUserName);
  }

  public void setUserId(String userId) {
    UserInfo userInfo = userInformationService.findById(userId).orElseThrow();
    this.userId = userId;
    setSavedUserName(userInfo.userName());
  }

  private void setSavedUserName(String userName) {
    overwriteUserNameChangeListener(userName);
    userNameField.setValue(userName);
    setUserNameChanged(false);
  }

  private Registration addHasChangedListener(String userName, TextField userNameField) {
    return userNameField.addValueChangeListener(valueChanged -> setUserNameChanged(
        !Objects.equals(userName, valueChanged.getValue())));
  }

  private void overwriteUserNameChangeListener(String userName) {
    if (nonNull(userNameChangeListenerRegistration)) {
      userNameChangeListenerRegistration.remove();
    }
    userNameChangeListenerRegistration = addHasChangedListener(userName, this.userNameField);
  }

  private void setUserNameChanged(boolean hasChanged) {
    if (hasChanged) {
      save.setEnabled(true);
      cancel.setEnabled(true);
      return;
    }
    //we have the current value again
    save.setEnabled(false);
    cancel.setEnabled(false);
  }
}
