package life.qbic.views.login;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginI18n.Form;

/**
 * <b>A login form that remembers the text it displays.</b>
 *
 * @since 1.0.0
 */
class ConfigurableLoginForm extends LoginForm {

  record ErrorMessage(String title, String message) {

  }

  private String errorMessage;
  private String errorTitle;
  // form fields
  private String usernameText;

  private void updateText() {
    LoginI18n loginI18n = LoginI18n.createDefault();
    if (errorTitle != null && !errorTitle.isBlank()
        || errorMessage != null && !errorMessage.isBlank()) {
      LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
      errorMessage.setMessage(this.errorMessage);
      errorMessage.setTitle(this.errorTitle);
      loginI18n.setErrorMessage(errorMessage);
    }
    Form form = loginI18n.getForm();
    if (usernameText != null && !usernameText.isBlank()) {
      form.setUsername(usernameText);
    }
    loginI18n.setForm(form);
    this.setI18n(loginI18n);
  }

  public void setError(ErrorMessage errorMessage) {
    this.errorTitle = errorMessage.title();
    this.errorMessage = errorMessage.message();
    updateText();
  }

  /**
   * Shows the error. Same as <code>setError(true)</code>
   *
   * @see #setError(boolean)
   */
  public void showError() {
    setError(true);
  }

  /**
   * Hide the error. Same as <code>setError(false)</code>
   *
   * @see #setError(boolean)
   */
  public void hideError() {
    setError(false);
  }

  public void setUsernameText(String usernameText) {
    this.usernameText = usernameText;
    updateText();
  }
}
