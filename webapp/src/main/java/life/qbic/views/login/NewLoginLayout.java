package life.qbic.views.login;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.MainLayout;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Route(value = "login", layout = MainLayout.class)
@AnonymousAllowed
public class NewLoginLayout extends VerticalLayout implements BeforeEnterObserver {

  public LoginForm loginForm;

  public NewLoginLayout() {
    this.setSizeFull();
    this.loginForm = new LoginForm();
    add(loginForm);
    loginForm.setAction("login");
    add(new Button("Sign up"));
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    if (beforeEnterEvent.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      loginForm.setError(true);
    }

  }
}
