package life.qbic.datamanager.views.projectOverview;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.components.OfferSearchDialog;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.security.PermitAll;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@PageTitle("Project Overview")
@Route(value = "projects", layout = MainLayout.class)
@PermitAll
public class ProjectOverviewLayout extends VerticalLayout implements Serializable {

    @Serial
    private static final long serialVersionUID = -4665004581087726748L;

    //todo add vaadin components here eg
    //a grid containing the projects
    //create new button
    Button create;
    //create dialogs:
    //select creation mode dialog
    //search offer dialog
    OfferSearchDialog searchDialog;

    private final OfferLookupService offerLookupService;


    public ProjectOverviewLayout(@Autowired ProjectOverviewHandlerInterface handlerInterface, @Autowired OfferLookupService offerLookupService) {
        Objects.requireNonNull(offerLookupService);
        this.offerLookupService = offerLookupService;

        createLayoutContent();
        registerToHandler(handlerInterface);
    }

    private void registerToHandler(ProjectOverviewHandlerInterface handler) {
        handler.handle(this);
    }

    private void createLayoutContent() {
        create = new Button("Create");
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        searchDialog = new OfferSearchDialog(offerLookupService);

        add(create);
    }

}
