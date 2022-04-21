package life.qbic.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import life.qbic.data.entity.TestUser;
import life.qbic.security.AuthenticatedUser;

import java.util.Optional;

/**
 * The main view is a top-level placeholder for other views.
 */
@PageTitle("Data Manager ")
@Route(value = "data")
public class MainLayout extends AppLayout {

    protected Button register;
    protected Button login;

    private AuthenticatedUser authenticatedUser;
    private AccessAnnotationChecker accessChecker;

    public MainLayout(AuthenticatedUser authenticatedUser, AccessAnnotationChecker accessChecker) {
        this.authenticatedUser = authenticatedUser;
        this.accessChecker = accessChecker;

        createHeaderContent();
        addListeners();
    }

    private Component createHeaderContent() {
        H1 appName = new H1("Data Manager");
        appName.addClassNames("text-l", "m-m");


        HorizontalLayout header = new HorizontalLayout(
                //new DrawerToggle(),
                appName
        );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");

        register = new Button("Register");
        login = new Button("Login");
        login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout buttons = new HorizontalLayout(register,login);
        buttons.addClassName("button-layout-spacing");

        addToNavbar(header, buttons);

        Optional<TestUser> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            TestUser testUser = maybeUser.get();

            Avatar avatar = new Avatar(testUser.getName(), testUser.getProfilePictureUrl());
            avatar.addClassNames("me-xs");

            ContextMenu userMenu = new ContextMenu(avatar);
            userMenu.setOpenOnClick(true);
            userMenu.addItem("Logout", e -> {
                authenticatedUser.logout();
            });

            Span name = new Span(testUser.getName());
            name.addClassNames("font-medium", "text-s", "text-secondary");

            header.add(avatar, name);
        } else {
            Anchor loginLink = new Anchor("login", "Sign in");
            header.add(loginLink);
        }

        /* header or navbar
        Nav nav = new Nav();
        nav.addClassNames("flex", "gap-s", "overflow-auto", "px-m");

        createDrawer();//todo
        */

        return header;
    }

    private void createDrawer() {
        /*Vaadin example how to add an element to the drawer

        RouterLink listLink = new RouterLink("List", ListView.class);
        listLink.setHighlightCondition(HighlightConditions.sameLocation());

        addToDrawer(new VerticalLayout(
                listLink
        ));
        */
    }

    private void addListeners() {
        login.addClickListener(event -> {
            login.getUI().ifPresent(ui ->
                    ui.navigate("login"));
            //Dialog loginView = LoginView.createLoginWindow();
            //loginView.open();
        });

        register.addClickListener(event -> {
            login.getUI().ifPresent(ui ->
                    ui.navigate("register"));
            //Dialog registerView = RegistrationView.createLoginWindow();
            //registerView.open();
        });
    }

}
