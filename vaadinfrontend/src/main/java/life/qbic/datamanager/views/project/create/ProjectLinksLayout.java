package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import life.qbic.datamanager.views.components.CardLayout;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@SpringComponent
public class ProjectLinksLayout extends Composite<CardLayout> {

  final VerticalLayout offerLinks;

  public ProjectLinksLayout(ProjectLinksHandler projectLinksHandler) {
    projectLinksHandler.handle(this);
    offerLinks = new VerticalLayout();
  }
}
