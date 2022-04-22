package life.qbic.views.register;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.MainLayout;
import life.qbic.views.login.LoginLayout;

import java.util.stream.Stream;

@PageTitle("Register")
@Route(value = "register", layout = MainLayout.class)
@AnonymousAllowed
@CssImport("./styles/views/login/login-view.css")
public class RegisterLayout extends VerticalLayout {

    protected EmailField email;

    protected PasswordField password;

    protected Paragraph errorMessageField;

    protected Button registerButton;

    protected Span loginSpan;

    private VerticalLayout contentLayout;

    public RegisterLayout() {
        setId("register-view");
        contentLayout = new VerticalLayout();

        initLayout();
    }

    private void initLayout() {
        H3 title = new H3("Register");

        styleEmailField();
        styleRegisterButton();
        stylePasswordField();
        var passwordLayout = createPasswordLayout();
        createSpan();

        setRequiredIndicatorVisible(email, password);
        styleFormLayout(title, passwordLayout);

        add(contentLayout);
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    private void styleFormLayout(H3 title, VerticalLayout passwordLayout) {
        contentLayout.addClassNames("bg-base", "border", "border-contrast-30", "box-border", "flex", "flex-col", "w-full");
        contentLayout.add(title, email, passwordLayout,
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

    private VerticalLayout createPasswordLayout(){
        VerticalLayout compositionLayout = new VerticalLayout(password,errorMessageField);
        compositionLayout.setSpacing(false);
        compositionLayout.setPadding(false);
        compositionLayout.setWidthFull();

        return compositionLayout;
    }

    private void stylePasswordField() {
        password = new PasswordField("Password");
        password.setWidthFull();
        errorMessageField = new Paragraph("Must be at least 8 characters long");
        errorMessageField.addClassName("description-text");
    }

    private void styleEmailField() {
        email = new EmailField("Email");
        email.setWidthFull();
    }

    public PasswordField getPasswordField() { return password; }

    public Paragraph getErrorMessageField() { return errorMessageField; }

    public Button getRegisterButton() { return registerButton; }

    private void setRequiredIndicatorVisible(HasValueAndElement<?, ?>... components) {
        Stream.of(components).forEach(comp -> comp.setRequiredIndicatorVisible(true));
    }

}
