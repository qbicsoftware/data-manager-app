package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import life.qbic.datamanager.views.layouts.CardLayout;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class ExperimentalDesignCard extends CardLayout {

  public record Experiment(String name, String description) {

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
