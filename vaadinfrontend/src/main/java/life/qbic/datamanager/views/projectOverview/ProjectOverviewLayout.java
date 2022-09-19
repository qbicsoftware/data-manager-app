package life.qbic.datamanager.views.projectOverview;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.components.SearchDialog;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@PageTitle("Project Overview")
@Route(value = "projects")
public class ProjectOverviewLayout extends DataManagerLayout {

    //todo add vaadin components here eg
    //a grid containing the projects
    //create new button
    //create dialogs:
    //select creation mode dialog
    //search offer dialog
    SearchDialog searchDialog;

    public ProjectOverviewLayout(@Autowired ProjectOverviewHandlerInterface handlerInterface) {
        registerToHandler(handlerInterface);
        createLayoutContent();
    }

    private void registerToHandler(ProjectOverviewHandlerInterface handler) {
        handler.handle(this);
    }

    private void createLayoutContent() {
        //searchDialog = new SearchDialog();
        //todo open it after offer creation mode was selected
        //searchDialog.open();
    }

}
