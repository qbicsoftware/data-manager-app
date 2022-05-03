package life.qbic.views.login;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import life.qbic.views.MainLayout;
import life.qbic.views.register.UserRegistrationLayout;

/**
 * <b>Defines the layout and look of the login view. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Login")
@Route(value = "login", layout = MainLayout.class)
@AnonymousAllowed
@CssImport("./styles/views/login/login-view.css")
public class NewLoginLayout extends VerticalLayout implements BeforeEnterObserver {


  private VerticalLayout contentLayout;
  public LoginForm loginForm;
  public Span registerSpan;

  public NewLoginLayout() {
    this.addClassName("grid");

    initLayout();
    styleLayout();
  }

  private void initLayout() {
    contentLayout = new VerticalLayout();
    this.loginForm = new LoginForm();
    loginForm.setAction("login");

    createSpan();

    contentLayout.add(loginForm, registerSpan);

    add(contentLayout);
  }

  private void styleLayout() {
    styleFormLayout();

    LoginI18n i18n = LoginI18n.createDefault();
    i18n.getForm().setUsername("Email");
    loginForm.setI18n(i18n);

    setSizeFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }


  private void styleFormLayout() {
    registerSpan.addClassName("p-l");
    contentLayout.setWidthFull();
    contentLayout.setPadding(false);
    contentLayout.getElement().getClassList().addAll(
            List.of("bg-base", "border", "border-contrast-10", "rounded-m", "box-border", "flex", "flex-col", "w-full", "text-s", "shadow-l", "min-width-300px", "max-width-15vw"));
  }

  private void createSpan() {
    RouterLink routerLink = new RouterLink("REGISTER", UserRegistrationLayout.class);
    registerSpan = new Span(new Text("Need an account? "), routerLink);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    if (beforeEnterEvent.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      loginForm.setError(true);
    }
  }
}
