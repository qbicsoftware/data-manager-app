package life.qbic.views.login.resetPassword;

import org.springframework.stereotype.Component;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Component
public class LinkSendHandler implements LinkSendHandlerInterface{

    private LinkSendLayout registeredLayout;

    @Override
    public void handle(LinkSendLayout layout) {
        if(registeredLayout != layout){
            registeredLayout = layout;

            addClicklisteners();
        }
    }

    private void addClicklisteners() {
        registeredLayout.loginButton.addClickListener(buttonClickEvent ->
                registeredLayout.getUI().ifPresent(ui -> ui.navigate("login")));
        registeredLayout.resendButton.addClickListener(buttonClickEvent ->{
                    //todo trigger resend
                });
    }
}
