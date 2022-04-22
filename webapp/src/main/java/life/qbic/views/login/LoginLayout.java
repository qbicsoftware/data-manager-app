package life.qbic.views.login;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.MainLayout;
import life.qbic.views.register.RegisterLayout;

import java.util.stream.Stream;

/**
 * Describes the layout of the login
 */

@PageTitle("Login")
@Route(value = "login", layout = MainLayout.class)
@AnonymousAllowed
@CssImport("./styles/views/login/login-view.css")
public class LoginLayout extends Composite<VerticalLayout> {

    protected EmailField email;

    protected PasswordField password;

    protected Paragraph errorMessageField;

    protected Button loginButton;

    protected RouterLink registerLink;

    private final VerticalLayout contentLayout;

    public LoginLayout() {
        setId("login-view");
        contentLayout = new VerticalLayout();

        initLayout();
    }

    private void initLayout() {
        H3 title = new H3("Login");

        styleEmailField();
        styleLoginButton();
        VerticalLayout passwordLayout = createPasswordLayout();
        registerLink = new RouterLink("REGISTER", RegisterLayout.class);

        setRequiredIndicatorVisible(email, password);
        styleFormLayout(title, passwordLayout);


        VerticalLayout parentLayout = this.getContent();
        parentLayout.add(contentLayout);
        parentLayout.setSizeFull();
        parentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        parentLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    }

    private void styleFormLayout(H3 title, VerticalLayout passwordLayout) {
        contentLayout.addClassNames("bg-base", "border", "border-contrast-10", "box-border", "flex", "flex-col", "w-full");
        contentLayout.add(title, email, passwordLayout,
                loginButton, new Span(new Text("Need an account? "),registerLink));
    }

    private void styleLoginButton() {
        loginButton = new Button("Login");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setEnabled(false);
        loginButton.setWidthFull();
    }

    private void styleEmailField(){
        email = new EmailField("Email");
        email.setWidthFull();
    }

    private VerticalLayout createPasswordLayout(){
        password = new PasswordField("Password");
        password.setWidthFull();

        errorMessageField = new Paragraph("Must be at least 8 characters long");
        errorMessageField.addClassNames("font-medium", "text-s", "text-secondary");
        errorMessageField.setWidthFull();

        VerticalLayout compositionLayout = new VerticalLayout(password,errorMessageField);
        compositionLayout.setSpacing(false);
        compositionLayout.setPadding(false);
        compositionLayout.setWidthFull();

        return compositionLayout;
    }

    public PasswordField getPasswordField() { return password; }

    public Paragraph getErrorMessageField() { return errorMessageField; }

    public Button getLoginButton() { return loginButton; }

    public RouterLink getRegisterLink() { return registerLink; }

    public EmailField getEmail() { return email; }

    private void setRequiredIndicatorVisible(HasValueAndElement<?, ?>... components) {
        Stream.of(components).forEach(comp -> comp.setRequiredIndicatorVisible(true));
    }

}
