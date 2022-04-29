package life.qbic.views;


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
            registeredMainLayout.login.getUI().ifPresent(ui ->
                    ui.navigate("login"));

        });

        registeredMainLayout.register.addClickListener(event -> {
            registeredMainLayout.register.getUI().ifPresent(ui ->
                    ui.navigate("register"));
        });
    }


}
