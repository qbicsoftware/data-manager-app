package life.qbic.datamanager.views.project.overview;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.ClientDetailsProvider.ClientDetails;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.project.overview.components.CreationModeDialog;
import life.qbic.datamanager.views.project.overview.components.OfferSearchDialog;
import life.qbic.projectmanagement.application.ProjectPreview;
import org.springframework.beans.factory.annotation.Autowired;

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
    Grid<ProjectPreview> projectGrid;
    OfferSearchDialog searchDialog;

    TextField projectSearchField;

    CreationModeDialog selectCreationModeDialog;

    private final ClientDetailsProvider clientDetailsProvider;


    public ProjectOverviewLayout(@Autowired ProjectOverviewHandlerInterface handlerInterface,
        @Autowired ClientDetailsProvider clientDetailsProvider) {
        this.clientDetailsProvider = clientDetailsProvider;
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

        projectSearchField = new TextField();
        projectSearchField.setPlaceholder("Search");
        projectSearchField.setClearButtonVisible(true);
        projectSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());

        projectGrid = new Grid<>(ProjectPreview.class, false);
        projectGrid.addColumn(ProjectPreview::projectTitle).setHeader("Title");
        projectGrid.addColumn(new LocalDateTimeRenderer<>(projectPreview ->
                asClientLocalDateTime(projectPreview.lastModified()), "yyyy-MM-dd HH:mm:ss"))
            .setHeader("Last Modified");
        add(create, projectSearchField, projectGrid);
    }

    private LocalDateTime asClientLocalDateTime(Instant instant) {
        String clientTimeZone = clientDetailsProvider.latestDetails()
            .map(ClientDetails::timeZoneId)
            .orElse("UTC");
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(clientTimeZone));
        return zonedDateTime.toLocalDateTime();
    }


}
