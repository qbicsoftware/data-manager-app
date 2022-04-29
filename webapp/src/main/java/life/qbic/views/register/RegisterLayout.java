package life.qbic.views.register;

import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.ErrorMessage;
import life.qbic.views.MainLayout;
import life.qbic.views.login.LoginLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

@PageTitle("Register")
@Route(value = "register", layout = MainLayout.class)
@AnonymousAllowed
@CssImport("./styles/views/login/login-view.css")
public class RegisterLayout extends VerticalLayout {

    public EmailField email;

    public PasswordField password;

    public TextField fullName;

    public Button registerButton;

    public Span loginSpan;

    public ErrorMessage alreadyUsedEmailMessage;
    public ErrorMessage passwordTooShortMessage;
    public ErrorMessage errorMessage;

    private final VerticalLayout contentLayout;

    public RegisterLayout(@Autowired RegisterHandlerInterface registerHandler) {
        setId("register-view");
        contentLayout = new VerticalLayout();

        initLayout();
        registerToHandler(registerHandler);
    }

    private void registerToHandler(RegisterHandlerInterface registerHandler) {
        if (registerHandler.register(this)) {
            System.out.println("Registered RegisterHandler");
        } else {
            System.out.println("Already registered RegisterHandler");
        }
    }

    private void initLayout() {
        H3 title = new H3("Register");

        createErrorMessage();
        styleEmailField();
        styleNameField();
        stylePasswordField();
        styleRegisterButton();
        createSpan();

        setRequiredIndicatorVisible(fullName, email, password);
        styleFormLayout(title);

        add(contentLayout);
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    }

    private void createErrorMessage(){
        alreadyUsedEmailMessage = new ErrorMessage("Email already in use",
                "If you have difficulties with your password you can reset it.");
        alreadyUsedEmailMessage.setVisible(false);

        passwordTooShortMessage = new ErrorMessage("Password too short",
                "Your password must be at least 8 characters long.");
        passwordTooShortMessage.setVisible(false);

        errorMessage = new ErrorMessage("Registration failed",
                "Please try again.");
        errorMessage.setVisible(false);
    }

    private void styleNameField() {
        fullName = new TextField("Full Name");
        fullName.setWidthFull();
    }

    private void styleFormLayout(H3 title) {
        contentLayout.addClassNames("bg-base", "border", "border-contrast-30", "box-border", "flex", "flex-col", "w-full");
        contentLayout.add(title, errorMessage, fullName, email, password,
                registerButton, loginSpan);
    }

    private void createSpan(){
        RouterLink link = new RouterLink("LOGIN", LoginLayout.class);
        loginSpan = new Span(new Text("Already have an account? "),link);
    }
    private void styleRegisterButton() {
        registerButton = new Button("Register");
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();
    }

    private void stylePasswordField() {
        password = new PasswordField("Password");
        password.setHelperText("A password must be at least 8 characters");
        password.setPattern(".{8,}");
        password.setErrorMessage("Not a valid password");
        password.setWidthFull();
    }

    private void styleEmailField() {
        email = new EmailField("Email");
        email.setWidthFull();
    }

    private void setRequiredIndicatorVisible(HasValueAndElement<?, ?>... components) {
        Stream.of(components).forEach(comp -> comp.setRequiredIndicatorVisible(true));
    }

}
