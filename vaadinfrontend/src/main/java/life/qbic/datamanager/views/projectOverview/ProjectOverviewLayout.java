package life.qbic.datamanager.views.projectOverview;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.components.OfferSearchDialog;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.security.PermitAll;

/**
 * <b>Projects Overview</b>
 *
 * <p>The page the user is navigated to after successful login</p>
 *
 * @since 1.0.0
 */
@PageTitle("Project Overview")
@Route(value = "", layout = MainLayout.class)
@PermitAll
public class ProjectOverviewLayout extends VerticalLayout {

    //todo add vaadin components here eg
    //a grid containing the projects
    //create new button
    Button create;
    //create dialogs:
    //select creation mode dialog
    //search offer dialog
    OfferSearchDialog searchDialog;


    public ProjectOverviewLayout(@Autowired ProjectOverviewHandlerInterface handlerInterface) {
        createLayoutContent();
        registerToHandler(handlerInterface);
    }

    private void registerToHandler(ProjectOverviewHandlerInterface handler) {
        handler.handle(this);
    }

    private void createLayoutContent() {
        create = new Button("Create");
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        searchDialog = new OfferSearchDialog();

        add(create);
    }
}
