package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.router.BeforeEvent;

public interface CreateProjectHandlerInterface {

  void handle(CreateProjectLayout createProjectLayout);

  void handleEvent(BeforeEvent event);

}
