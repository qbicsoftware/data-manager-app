package life.qbic.views.helloworld;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import javax.annotation.security.PermitAll;
import life.qbic.security.SecurityService;
import life.qbic.views.MainLayout;

@PageTitle("Hello World")
@Route(value = "hello", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PermitAll
public class HelloWorldView extends VerticalLayout {

    private TextField name;
    private Button sayHello;

    private H1 personalWelcomeMessage;

    private SecurityService securityService;

    public HelloWorldView(SecurityService securityService) {
        this.securityService = securityService;


        name = new TextField("Your name");
        name.setValue(securityService.get().get().getFullName());
        sayHello = new Button("Say hello");
        sayHello.addClickListener(e -> {
            Notification.show("Hello " + name.getValue());
        });

        setMargin(true);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        this.personalWelcomeMessage = new H1();
        this.personalWelcomeMessage.getElement().setText("Welcome " + securityService.get().get().getFullName());
        add(personalWelcomeMessage, name, sayHello);
    }

}
