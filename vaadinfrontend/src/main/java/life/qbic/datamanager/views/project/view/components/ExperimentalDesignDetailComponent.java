package life.qbic.datamanager.views.project.view.components;

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
  private final VerticalLayout noDesignDefinedLayout = new VerticalLayout();

  public ExperimentalDesignDetailComponent() {
    this.handler = new Handler();
    getContent().addTitle(TITLE);
    initNoDesignDefinedLayout();
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
    getContent().addFields(noDesignDefinedLayout);
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
    }

    private void openDialogueListener() {
      createDesignButton.addClickListener(clickEvent -> experimentCreationDialog.open());
    }
  }
}
