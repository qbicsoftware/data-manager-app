package life.qbic.views.login;

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

import java.util.stream.Stream;

@PageTitle("Login")
@Route(value = "login", layout = MainLayout.class)
@AnonymousAllowed
@CssImport("./styles/views/login/login-view.css")
public class LoginView extends VerticalLayout {
    private H3 title;

    private EmailField email;

    private PasswordField password;

    private Paragraph errorMessageField;

    private Button loginButton;

    private VerticalLayout contentLayout;

    public LoginView() {
        setId("login-view");

        initLayout();
    }

    private void initLayout() {
        contentLayout = new VerticalLayout();

        title = new H3("Login");
        email = new EmailField("Email");
        email.setWidthFull();

        password = new PasswordField("Password");
        password.setWidthFull();

        setRequiredIndicatorVisible(email, password);

        errorMessageField = new Paragraph("Must be at least 8 characters long");
        errorMessageField.addClassName("description-text");
        errorMessageField.setWidthFull();

        loginButton = new Button("Login");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();

        contentLayout.add(title, email, password, errorMessageField,
                loginButton, new Span(new Text("Need an account? "),new RouterLink("REGISTER", RegistrationView.class)));

        add(contentLayout);
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

    }

    public PasswordField getPasswordField() { return password; }

    public Paragraph getErrorMessageField() { return errorMessageField; }

    public Button getLoginButton() { return loginButton; }

    private void setRequiredIndicatorVisible(HasValueAndElement<?, ?>... components) {
        Stream.of(components).forEach(comp -> comp.setRequiredIndicatorVisible(true));
    }

}
