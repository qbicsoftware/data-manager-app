package life.qbic.views.login;

import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.ErrorMessage;
import life.qbic.views.MainLayout;
import life.qbic.views.register.RegisterLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

/**
 * <b>Defines the layout and look of the login view. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Login")
@Route(value = "login", layout = MainLayout.class)
@AnonymousAllowed
@CssImport("./styles/views/login/login-view.css")
public class LoginLayout extends VerticalLayout {

    public EmailField email;

    public PasswordField password;

    public Button loginButton;

    public Span registerSpan;

    public ErrorMessage errorMessage;

    private final VerticalLayout contentLayout;

    public LoginLayout(@Autowired LoginHandlerInterface loginHandlerInterface) {
        setId("login-view");
        contentLayout = new VerticalLayout();

        initLayout();
        registerToHandler(loginHandlerInterface);
    }

    private void registerToHandler(LoginHandlerInterface loginHandler) {
        if (loginHandler.register(this)) {
            System.out.println("Registered login handler");
        } else {
            System.out.println("Already registered login handler");
        }
    }

    private void initLayout() {
        H3 title = new H3("Login");
        styleErrorDiv();

        styleEmailField();
        stylePasswordField();
        styleLoginButton();
        createSpan();

        setRequiredIndicatorVisible(email, password);
        styleFormLayout(title);

        add(contentLayout);
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    }

    private void styleErrorDiv(){
        errorMessage = new ErrorMessage("Incorrect email or password",
                "Check that you have used the correct email and password and try again");
        errorMessage.setVisible(false);
    }

    private void styleFormLayout(H3 title) {
        contentLayout.addClassNames("bg-base", "border", "border-contrast-30", "box-border", "flex", "flex-col", "w-full");
        contentLayout.add(title, errorMessage, email, password,
                loginButton,registerSpan);
    }

    private void createSpan(){
        RouterLink routerLink = new RouterLink("REGISTER", RegisterLayout.class);
        registerSpan = new Span(new Text("Need an account? "),routerLink);
    }

    private void styleLoginButton() {
        loginButton = new Button("Login");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setEnabled(true);
        loginButton.setWidthFull();
    }

    private void styleEmailField(){
        email = new EmailField("Email");
        email.setWidthFull();
    }

    private void stylePasswordField(){
        password = new PasswordField("Password");
        password.setErrorMessage("Wrong password");
        password.setWidthFull();
    }

    private void setRequiredIndicatorVisible(HasValueAndElement<?, ?>... components) {
        Stream.of(components).forEach(comp -> comp.setRequiredIndicatorVisible(true));
    }

}
