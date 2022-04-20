package life.qbic.views.login;

import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.stream.Stream;

@PageTitle("Login")
@Route(value = "login")
@CssImport("./styles/views/login/login-view.css")
public class LoginView extends Div {
    private H3 title;

    private EmailField email;

    private PasswordField password;

    private Paragraph errorMessageField;

    private Button submitButton;

    private Span linkRegister;


    public LoginView() {
        setId("login-view");

        initLayout();
    }

    private void initLayout() {
        title = new H3("Login");
        email = new EmailField("Email");

        password = new PasswordField("Password");

        setRequiredIndicatorVisible(email, password);

        errorMessageField = new Paragraph("Must be at least 8 characters long");
        errorMessageField.addClassName("description-text");

        submitButton = new Button("Login");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(title, email, password, errorMessageField,
                submitButton);
    }

    public PasswordField getPasswordField() { return password; }

    public Paragraph getErrorMessageField() { return errorMessageField; }

    public Button getSubmitButton() { return submitButton; }

    private void setRequiredIndicatorVisible(HasValueAndElement<?, ?>... components) {
        Stream.of(components).forEach(comp -> comp.setRequiredIndicatorVisible(true));
    }

}
