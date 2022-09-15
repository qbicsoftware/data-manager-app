package life.qbic.datamanager.views.projectOverview;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import life.qbic.datamanager.views.DataManagerLayout;
import org.springframework.beans.factory.annotation.Autowired;

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
    //searchbar

    public ProjectOverviewLayout(@Autowired ProjectOverviewHandlerInterface handlerInterface) {
        createLayoutContent();
        //registerToHandler(handlerInterface);
    }

    //private void registerToHandler(ProjectOverviewHandlerInterface handler) {
    //    handler.handle(this);
    //}

    private void createLayoutContent() {

    }


}
