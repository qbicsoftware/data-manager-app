package life.qbic.datamanager.views.project.overview;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.Serial;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.components.CardLayout;
import life.qbic.datamanager.views.project.overview.components.CreationModeDialog;
import life.qbic.datamanager.views.project.overview.components.OfferSearchDialog;
import life.qbic.projectmanagement.application.ProjectPreview;
import org.apache.poi.ss.formula.functions.T;
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
public class ProjectOverviewLayout extends Composite<CardLayout> {

    @Serial
    private static final long serialVersionUID = 5435551053955979169L;
    Button create = new Button("Create");
    Grid<ProjectPreview> projectGrid = new Grid<>(ProjectPreview.class, false);
    OfferSearchDialog searchDialog = new OfferSearchDialog();

    TextField projectSearchField = new TextField();

    private CardLayout cardLayout = new CardLayout();

    CreationModeDialog selectCreationModeDialog = new CreationModeDialog();


    public ProjectOverviewLayout(@Autowired ProjectOverviewHandlerInterface handlerInterface) {
        registerToHandler(handlerInterface);
    }

    @Override
    protected CardLayout initContent() {
        createLayoutContent();
        return cardLayout;
    }

    private void registerToHandler(ProjectOverviewHandlerInterface handler) {
        handler.handle(this);
    }

    private void createLayoutContent() {
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);


        projectSearchField.setPlaceholder("Search");
        projectSearchField.setClearButtonVisible(true);
        projectSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());

        projectGrid.addColumn(ProjectPreview::getProjectTitle).setHeader("Title");

        cardLayout.addTitle("Your projects");
        cardLayout.addFields(create, projectSearchField, projectGrid);
    }
}
