package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.stream.Stream;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.project.experiment.ExperimentCreationDialog;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
@SpringComponent
@UIScope
public class ExperimentalDesignDetailComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -2255999216830849632L;
  private static final String TITLE = "Experimental Design";
  private final Button createDesignButton = new Button("Add");
  private final ExperimentCreationDialog experimentCreationDialog = new ExperimentCreationDialog();
  private final transient Handler handler;
  private final VerticalLayout contentLayout = new VerticalLayout();
  private final VerticalLayout noDesignDefinedLayout = new VerticalLayout();
  private final VirtualList<Experiment> experiments = new VirtualList<>();
  private final CardLayout experimentalDesignAddCard;
  private final ComponentRenderer<Component, Experiment> experimentCardRenderer = new ComponentRenderer<>(
      ExperimentalDesignCard::new);

  public ExperimentalDesignDetailComponent() {
    this.handler = new Handler();
  }

  private void initNoDesignDefinedLayout() {
    Span experimentalDesignHeader = new Span("Experimental Design");
    Span experimentalDesignDescription = new Span("Add the experimental design now");
    experimentalDesignHeader.addClassName("font-bold");
    createDesignButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createDesignButton.addClassNames("mt-s", "mb-s");
    noDesignDefinedLayout.add(experimentalDesignHeader, experimentalDesignDescription,
        createDesignButton);
    noDesignDefinedLayout.setSizeFull();
    noDesignDefinedLayout.setAlignItems(Alignment.CENTER);
    noDesignDefinedLayout.setJustifyContentMode(JustifyContentMode.CENTER);
    //ToDo this should be swapped dependent on if an experimental design was defined or not
    //contentLayout.add(noDesignDefinedLayout);
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }


  /**
   * Component logic for the {@link ExperimentalDesignDetailComponent}
   *
   * @since 1.0.0
   */
  private final class Handler {

    public Handler() {
      openDialogueListener();
      experiments.setItems(getExperiments());
      experiments.setRenderer(experimentCardRenderer);
    }

    private void openDialogueListener() {
      createDesignButton.addClickListener(clickEvent -> experimentCreationDialog.open());
      experimentalDesignAddCard.addClickListener(clickEvent -> experimentCreationDialog.open());
    }

    private Stream<Experiment> getExperiments() {
      return Stream.of(new Experiment("Title_1", "Description1"),
          new Experiment("Title_2", "Description2"));
    }
  }
}
