package life.qbic.views.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import life.qbic.usermanagement.persistence.UserJpaRepository;
import life.qbic.usermanagement.policies.PasswordPolicy;
import life.qbic.usermanagement.policies.PolicyCheckReport;
import life.qbic.usermanagement.policies.PolicyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles the {@link LoginLayout} components
 *
 * This class is responsible for enabling buttons or triggering other view relevant changes on the view class components
 */
@Component
public class LoginHandler {

    private final UserJpaRepository userRepository;
    private final LoginLayout loginLayout;

    @Autowired
    LoginHandler(LoginLayout loginLayout, UserJpaRepository repository){
        this.loginLayout = loginLayout;
        this.userRepository = repository;

        addListener();
    }

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

}
