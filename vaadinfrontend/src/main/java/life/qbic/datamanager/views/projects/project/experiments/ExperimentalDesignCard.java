package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;

/**
 * <b>Experimental Design Card</b>
 *
 * <p>A CardLayout based Component showing the information stored in the
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign} associated with
 * a project in the {@link ExperimentListComponent} of the {@link ProjectViewPage}
 */
public class ExperimentalDesignCard extends CardLayout {

  public ExperimentalDesignCard(Experiment experiment) {
    Icon myIcon = VaadinIcon.FLASK.create();
    myIcon.addClassNames("mt-s", "mb-s");
    Span experimentDescription = new Span("some missing description");
    addTitle(experiment.getName());
    addFields(experimentDescription);
    HorizontalLayout horizontalLayout = new HorizontalLayout(myIcon, experimentDescription);
    experimentDescription.addClassName("font-bold");
    horizontalLayout.setJustifyContentMode(JustifyContentMode.START);
    horizontalLayout.setAlignItems(Alignment.CENTER);
    horizontalLayout.setSizeFull();
    addFields(horizontalLayout);
    setWidthFull();
    setHeight(null);
    this.getStyle().set("cursor", "pointer");
  }

}
