package life.qbic.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;


/**
 * The main view is a top-level placeholder for other views.
 */
@PageTitle("Data Manager ")
@Route(value = "data")
public class MainLayout extends AppLayout {

    public Button register;
    public Button login;

    public MainLayout() {
        createHeaderContent();
    }

    private Component createHeaderContent() {
        var header = createHeaderLayout();
        var buttons = createHeaderButtonLayout();

        addToNavbar(header, buttons);

        return header;
    }

    private HorizontalLayout createHeaderLayout() {
        H1 appName = styleHeaderTitle();

        var header = new HorizontalLayout(appName);

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");

        return header;
    }

    private H1 styleHeaderTitle() {
        H1 appName = new H1("Data Manager");
        appName.addClassNames("text-l", "m-m");
        return appName;
    }

    private HorizontalLayout createHeaderButtonLayout() {
        styleHeaderButtons();

        HorizontalLayout buttons = new HorizontalLayout(register,login);
        buttons.addClassName("button-layout-spacing");
        return buttons;
    }

    private void styleHeaderButtons() {
        register = new Button("Register");

        login = new Button("Login");
        login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }

}
