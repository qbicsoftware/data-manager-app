package life.qbic.views.helloworld;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import javax.annotation.security.PermitAll;
import life.qbic.security.SecurityService;
import life.qbic.domain.usermanagement.User;
import life.qbic.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Hello World")
@Route(value = "hello", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PermitAll
public class HelloWorldView extends VerticalLayout {

    private final TextField name;
    private final Button sayHello;

    private final H1 personalWelcomeMessage;

    private final SecurityService securityService;

    public HelloWorldView(@Autowired SecurityService securityService) {
        this.securityService = securityService;

        String username = securityService.get().map(User::getFullName).orElse("Your name");
        name = new TextField(username);

        sayHello = new Button("Say hello");
        sayHello.addClickListener(e -> {
            Notification.show("Hello " + name.getValue());
        });

        setMargin(true);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        this.personalWelcomeMessage = new H1();
        this.personalWelcomeMessage.getElement().setText("Welcome " + username);
        add(personalWelcomeMessage, name, sayHello);
    }

}
