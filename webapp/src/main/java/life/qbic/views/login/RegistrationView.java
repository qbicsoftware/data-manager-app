package life.qbic.views.login;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.MainLayout;

import java.util.stream.Stream;

@PageTitle("Register")
@Route(value = "register", layout = MainLayout.class)
@AnonymousAllowed
@CssImport("./styles/views/login/login-view.css")
public class RegistrationView extends VerticalLayout {

    private H3 title;

    private EmailField email;

    private PasswordField password;

    private Paragraph errorMessageField;

    private Button registerButton;

    private VerticalLayout contentLayout;

    public RegistrationView() {
        setId("login-view");

        initLayout();
    }

    private void initLayout() {
        contentLayout = new VerticalLayout();

        title = new H3("Register");
        email = new EmailField("Email");
        email.setWidthFull();

        password = new PasswordField("Password");
        password.setWidthFull();

        setRequiredIndicatorVisible(email, password);

        errorMessageField = new Paragraph("Must be at least 8 characters long");
        errorMessageField.addClassName("description-text");

        registerButton = new Button("Register");
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();

        contentLayout.add(title, email, password, errorMessageField,
                registerButton, new Span(new Text("Already have an account? "),new RouterLink("LOGIN", LoginView.class)));
        add(contentLayout);
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    public PasswordField getPasswordField() { return password; }

    public Paragraph getErrorMessageField() { return errorMessageField; }

    public Button getRegisterButton() { return registerButton; }

    private void setRequiredIndicatorVisible(HasValueAndElement<?, ?>... components) {
        Stream.of(components).forEach(comp -> comp.setRequiredIndicatorVisible(true));
    }

}
