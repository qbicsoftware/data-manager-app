package life.qbic.views.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import life.qbic.usermanagement.persistence.UserJpaRepository;
import life.qbic.usermanagement.policies.PasswordPolicy;
import life.qbic.usermanagement.policies.PolicyCheckReport;
import life.qbic.usermanagement.policies.PolicyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class LoginHandler implements LoginHandlerInterface {

    private final UserJpaRepository userRepository;

    private LoginLayout registeredLoginView;

    LoginHandler(@Autowired UserJpaRepository repository){
        this.userRepository = repository;
    }

    @Override
    public boolean register(LoginLayout loginView) {
        if (registeredLoginView == null) {
            this.registeredLoginView = loginView;
            // orchestrate view
            this.registeredLoginView.loginButton.addClickListener( e -> System.out.println( "Worked!"));
            // then return
            return true;
        }
        return false;
    }
/*
    private void addListener(){
        loginLayout.password.addValueChangeListener(event -> {
            enableLoginButton();
            isPasswordValid();
        });
        loginLayout.email.addValueChangeListener(event -> {
            enableLoginButton();
        });

        loginLayout.loginButton.addClickListener(event -> {
            try{
                var users = userRepository.findUsersByEmail(loginLayout.email.getValue());
                users.get(0).checkPassword(loginLayout.password.getValue());
                //todo authorization: create route during runtime
                UI.getCurrent().navigate("hello"); //could be dashboard later
            }catch (RuntimeException r){
                //todo show error in ui
                Notification.show("Wrong email or password");
            }
        });
    }

    private void isPasswordValid() {
        PolicyCheckReport report = PasswordPolicy.create().validate(loginLayout.password.getValue());
        if(report.status().equals(PolicyStatus.PASSED)){
            loginLayout.password.setInvalid(false);
        }else{
            //todo show notification in UI
            Notification.show(report.reason());
            loginLayout.password.setInvalid(true);
        }
    }

    private void enableLoginButton(){
        boolean validEmail = !loginLayout.email.isInvalid();
        boolean validPassword = !loginLayout.password.isInvalid();
        boolean enableButton = validPassword && validEmail;

        loginLayout.loginButton.setEnabled(enableButton);
    }
*/
}
