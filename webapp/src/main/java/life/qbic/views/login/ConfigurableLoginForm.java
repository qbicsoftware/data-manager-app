package life.qbic.views.login;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>A login form that remembers the text it displays.</b>
 * <p>This login form does not show a title.
 *
 * @since 1.0.0
 */
class ConfigurableLoginForm extends LoginForm {

  @FunctionalInterface
  public interface ErrorListener {

    void onError();
  }

  private final List<ErrorListener> errorListeners;

  private final LoginI18n loginI18n;


  public ConfigurableLoginForm() {
    super();
    this.loginI18n = LoginI18n.createDefault();
    removeTitle();
    this.setI18n(loginI18n);
    errorListeners = new ArrayList<>();
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

  private void fireErrorEvent() {
    errorListeners.forEach(ErrorListener::onError);
  }

  @Override
  public void setError(boolean error) {
    super.setError(error);
    if (error) {
      fireErrorEvent();
    }
  }

  @Override
  public boolean isError() {
    return super.isError();
  }

  public void addErrorListener(ErrorListener listener) {
    errorListeners.add(listener);
  }
}
