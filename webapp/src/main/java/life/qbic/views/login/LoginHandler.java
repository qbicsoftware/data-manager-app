package life.qbic.views.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import life.qbic.usermanagement.persistence.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginHandler implements LoginHandlerInterface {

    private final UserJpaRepository userRepository;

    private LoginLayout registeredLoginView;

    LoginHandler(@Autowired UserJpaRepository repository){
        this.userRepository = repository;
    }

    @Override
    public boolean register(LoginLayout loginView) {
        if (registeredLoginView != loginView) {
            this.registeredLoginView = loginView;
            // orchestrate view
            addListener();
            // then return
            return true;
        }

        return false;
    }

    private void addListener(){
        registeredLoginView.loginButton.addClickListener(event -> {
            try{
                var users = userRepository.findUsersByEmail(registeredLoginView.email.getValue());
                users.get(0).checkPassword(registeredLoginView.password.getValue());
                //todo authorization: show the correct route now --> security context
                UI.getCurrent().navigate("about"); //could be dashboard later
                Notification.show("It worked");
            }catch (RuntimeException r){
                //todo show error in ui
                Notification.show("Wrong email or password");
            }
        });
    }

}
