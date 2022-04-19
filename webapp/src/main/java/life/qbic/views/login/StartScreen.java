package life.qbic.views.login;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Start")
@Route(value = "start")
public class StartScreen extends Div {

    private final Button register;

    //todo implement nicer, just for proof-of-concept/navigation
    StartScreen(){
        register = new Button("Login");
        register.addThemeVariants(ButtonVariant.MATERIAL_OUTLINED);

        Dialog dialog = new Dialog(new LoginView());
        register.addClickListener(event -> {dialog.open();  });

        add(register, dialog);

    dialog.add(new Span("Need an account? SIGN UP"));
    }
}
