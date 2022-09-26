package life.qbic.datamanager.views.projectOverview;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.projectOverview.components.OfferSearchDialog;
import life.qbic.datamanager.views.projectOverview.components.CreationModeDialog;
import life.qbic.projectmanagement.domain.project.Project;
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
@Route(value = "projects", layout = MainLayout.class)
@PermitAll
public class ProjectOverviewLayout extends VerticalLayout {

    Button create;
    Grid<Project> projectGrid;
    OfferSearchDialog searchDialog;

    CreationModeDialog selectCreationModeDialog;


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
        selectCreationModeDialog = new CreationModeDialog();

        projectGrid = new Grid<>();

        add(create);
    }
}
