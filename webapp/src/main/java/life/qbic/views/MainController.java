package life.qbic.views;

public class MainController {

    private final MainLayout mainLayout;

    MainController(MainLayout layout){
        this.mainLayout = layout;

        addClickListeners();
    }

    private void addClickListeners() {
        mainLayout.login.addClickListener(event -> {
            mainLayout.login.getUI().ifPresent(ui ->
                    ui.navigate("login"));

        });

        mainLayout.register.addClickListener(event -> {
            mainLayout.register.getUI().ifPresent(ui ->
                    ui.navigate("register"));
        });
    }
}
