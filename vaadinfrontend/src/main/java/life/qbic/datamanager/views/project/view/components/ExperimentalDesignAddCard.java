package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import life.qbic.datamanager.views.layouts.CardLayout;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
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
