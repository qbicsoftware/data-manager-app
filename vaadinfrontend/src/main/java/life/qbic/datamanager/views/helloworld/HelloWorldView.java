package life.qbic.datamanager.views.helloworld;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.Serial;
import javax.annotation.security.PermitAll;
import life.qbic.authentication.domain.user.concept.FullName;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.datamanager.security.SecurityService;
import life.qbic.datamanager.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Hello World")
@Route(value = "test", layout = MainLayout.class)
@PermitAll
public class HelloWorldView extends VerticalLayout {

  @Serial
  private static final long serialVersionUID = 4170996960459734911L;
  private final TextField name;

  public HelloWorldView(@Autowired SecurityService securityService) {

    String username = securityService.get().map(User::fullName)
        .map(FullName::get)
        .orElse("Your name");
    name = new TextField(username);

    Button sayHello = new Button("Say hello");
    sayHello.addClickListener(
        e -> Notification.show("Hello " + name.getValue()));

    setMargin(true);
    setDefaultHorizontalComponentAlignment(Alignment.CENTER);

    H1 personalWelcomeMessage = new H1();
    personalWelcomeMessage.getElement().setText("Welcome " + username);
    add(personalWelcomeMessage, name, sayHello);

  }
}
