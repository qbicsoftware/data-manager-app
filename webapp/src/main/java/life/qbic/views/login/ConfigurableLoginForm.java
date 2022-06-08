package life.qbic.views.login;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.login.AbstractLogin.ForgotPasswordEvent;
import com.vaadin.flow.component.login.AbstractLogin.LoginEvent;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import life.qbic.views.components.InformationMessage;

/**
 * <b>A login form that remembers the text it displays.</b>
 *
 * @since 1.0.0
 */
class ConfigurableLoginForm extends VerticalLayout {

  private final LoginForm loginForm;

  private final LoginI18n loginI18n;
  private final HorizontalLayout informationMessageLayout;

  record Message(String title, String message) {

  }


  public ConfigurableLoginForm() {
    super();
    setPadding(false);
    setMargin(false);
    setSpacing(false);
    loginForm = new LoginForm();
    this.loginI18n = LoginI18n.createDefault();
    loginI18n.getForm().setTitle("");
    loginForm.setI18n(loginI18n);

    H2 title = new H2("Log in");
    title.setClassName("form-title");

    informationMessageLayout = new HorizontalLayout();
    informationMessageLayout.setSizeFull();
    this.add(title, informationMessageLayout, loginForm);
  }

  private void updateText() {
    loginForm.setI18n(loginI18n);
  }

  /**
   * @see LoginForm#setAction(String) ´
   */
  public void setAction(String action) {
    loginForm.setAction(action);
  }

  public void setError(Message errorMessage) {
    LoginI18n.ErrorMessage error = new LoginI18n.ErrorMessage();
    error.setMessage(errorMessage.message());
    error.setTitle(errorMessage.title());
    loginI18n.setErrorMessage(error);
    updateText();
  }

  public void showInformation(Message message) {
    informationMessageLayout.add(new InformationMessage(message.title(), message.message()));
    informationMessageLayout.setSizeFull();
    informationMessageLayout.setVisible(true);
  }

  public void resetInformation() {
    informationMessageLayout.removeAll();
    informationMessageLayout.setVisible(false);
  }

  /**
   * Shows the error.
   */
  public void showError() {
    loginForm.setError(true);
  }

  /**
   * Hide the error.
   */
  public void hideError() {
    loginForm.setError(false);
  }

  public void setUsernameText(String usernameText) {
    loginI18n.getForm().setUsername(usernameText);
    updateText();
  }

  public void addLoginListener(ComponentEventListener<LoginEvent> loginListener) {
    loginForm.addLoginListener(loginListener);
  }

  public void addForgotPasswordListener(ComponentEventListener<ForgotPasswordEvent> listener) {
    loginForm.addForgotPasswordListener(listener);
  }
}
