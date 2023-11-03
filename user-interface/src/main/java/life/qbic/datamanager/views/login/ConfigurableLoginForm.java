package life.qbic.datamanager.views.login;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginI18n.ErrorMessage;

/**
 * <b>A login form that remembers the text it displays.</b>
 * <p>This login form does not show a title.
 *
 * @since 1.0.0
 */
class ConfigurableLoginForm extends LoginForm {

  private final LoginI18n loginI18n;


  public ConfigurableLoginForm() {
    super();
    this.loginI18n = LoginI18n.createDefault();
    removeTitle();
    removeErrorMessage();
    this.setI18n(loginI18n);
    this.setId("login-form");
  }

  private void removeErrorMessage() {
    loginI18n.setErrorMessage(new ErrorMessage());
    updateText();
  }

  private void removeTitle() {
    loginI18n.getForm().setTitle("");
  }

  private void updateText() {
    this.setI18n(loginI18n);
  }

  public void setUsernameText(String usernameText) {
    loginI18n.getForm().setUsername(usernameText);
    updateText();
  }
}
