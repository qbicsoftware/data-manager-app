package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.projects.project.experiments.experiment.AddVariableToExperimentDialog;

/**
 * <b>Experimental Design Add Card</b>
 *
 * <p>A CardLayout based Component which is simliarly structured as the
 * {@link ExperimentalDesignCard}. However it's content is fixed and it's purpose is to allow for a
 * user to click on the card to create a new
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign} via the
 * {@link AddVariableToExperimentDialog}
 */
public class ExperimentalDesignAddCard extends CardLayout {

  public ExperimentalDesignAddCard() {
    Icon myIcon = VaadinIcon.PLUS.create();
    myIcon.addClassNames("mt-s", "mb-s");
    Span text = new Span("Add Experiment");
    text.addClassName("font-bold");
    VerticalLayout verticalLayout = new VerticalLayout(myIcon, text);
    verticalLayout.setJustifyContentMode(JustifyContentMode.CENTER);
    verticalLayout.setAlignItems(Alignment.CENTER);
    verticalLayout.setSizeFull();
    addFields(verticalLayout);
    setWidthFull();
    setHeight(null);
  }

}
