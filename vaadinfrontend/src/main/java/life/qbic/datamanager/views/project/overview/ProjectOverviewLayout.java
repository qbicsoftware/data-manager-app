package life.qbic.datamanager.views.project.overview;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.ClientDetailsProvider.ClientDetails;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.components.CardLayout;
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
public class ProjectOverviewLayout extends Composite<CardLayout> {

    @Serial
    private static final long serialVersionUID = 5435551053955979169L;
    final Button create = new Button("Create");
    final Grid<ProjectPreview> projectGrid = new Grid<>(ProjectPreview.class, false);
    final OfferSearchDialog searchDialog = new OfferSearchDialog();

    final TextField projectSearchField = new TextField();

    final CreationModeDialog selectCreationModeDialog = new CreationModeDialog();

    private final ClientDetailsProvider clientDetailsProvider;


    public ProjectOverviewLayout(@Autowired ProjectOverviewHandlerInterface handlerInterface,
        @Autowired ClientDetailsProvider clientDetailsProvider) {
        this.clientDetailsProvider = clientDetailsProvider;
        layoutComponents();
        registerToHandler(handlerInterface);
    }

    private void registerToHandler(ProjectOverviewHandlerInterface handler) {
        handler.handle(this);
    }

    private void layoutComponents() {
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        projectSearchField.setPlaceholder("Search");
        projectSearchField.setClearButtonVisible(true);
        projectSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());

        projectGrid.addColumn(ProjectPreview::projectTitle).setHeader("Title");
        projectGrid.addColumn(new LocalDateTimeRenderer<>(projectPreview ->
                asClientLocalDateTime(projectPreview.lastModified()), "yyyy-MM-dd HH:mm:ss"))
            .setHeader("Last Modified");
        getContent().addFields(create, projectSearchField, projectGrid);
    }

    private LocalDateTime asClientLocalDateTime(Instant instant) {
        String clientTimeZone = clientDetailsProvider.latestDetails()
            .map(ClientDetails::timeZoneId)
            .orElse("UTC");
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(clientTimeZone));
        return zonedDateTime.toLocalDateTime();
    }

}
