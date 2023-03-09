package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import life.qbic.datamanager.views.layouts.CardLayout;

/**
 * <b>Experimental Design Card</b>
 *
 * <p>A CardLayout based Component showing the information stored in the
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign} associated with
 * a project in the {@link ExperimentListComponent} of the
 * {@link life.qbic.datamanager.views.project.view.ProjectViewPage}
 */
public class ExperimentalDesignCard extends CardLayout {

  public record Experiment(String id, String name, String description) {

  }

  public ExperimentalDesignCard(Experiment experiment) {
    Icon myIcon = VaadinIcon.FLASK.create();
    myIcon.addClassNames("mt-s", "mb-s");
    Span experimentDescription = new Span(experiment.description());
    addTitle(experiment.name);
    addFields(experimentDescription);
    HorizontalLayout horizontalLayout = new HorizontalLayout(myIcon, experimentDescription);
    experimentDescription.addClassName("font-bold");
    horizontalLayout.setJustifyContentMode(JustifyContentMode.START);
    horizontalLayout.setAlignItems(Alignment.CENTER);
    horizontalLayout.setSizeFull();
    addFields(horizontalLayout);
    setWidthFull();
    setHeight(null);
  }

}
