package life.qbic.views;

import com.vaadin.flow.component.UI;

/**
 * <b> Handles the view elements of the {@link MainLayout}. </b>
 *
 * @since 1.0.0
 */
public class MainHandler implements MainHandlerInterface{

    private MainLayout registeredMainLayout;

    @Override
    public boolean register(MainLayout layout) {
        if (registeredMainLayout != layout) {
            this.registeredMainLayout = layout;
            // orchestrate view
            addClickListeners();
            // then return
            return true;
        }

        return false;
    }

    private void addClickListeners() {
        registeredMainLayout.login.addClickListener(event -> {
            UI.getCurrent().navigate("login");

        });

        registeredMainLayout.register.addClickListener(event -> {
            UI.getCurrent().navigate("register");
        });
    }


}
