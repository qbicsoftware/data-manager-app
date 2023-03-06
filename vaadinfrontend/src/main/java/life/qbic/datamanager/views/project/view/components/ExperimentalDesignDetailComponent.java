package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.project.experiment.ExperimentCreationDialog;
import life.qbic.datamanager.views.project.view.components.ExperimentalDesignCard.Experiment;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

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
  private final CardLayout experimentalDesignAddCard = new ExperimentalDesignAddCard();
  private final ComponentRenderer<Component, Experiment> experimentCardRenderer = new ComponentRenderer<>(
      ExperimentalDesignCard::new);

  public ExperimentalDesignDetailComponent(
      @Autowired ExperimentInformationService experimentInformationService) {
    contentLayout.add(experiments);
    contentLayout.add(experimentalDesignAddCard);
    getContent().addTitle(TITLE);
    getContent().addFields(contentLayout);
    this.handler = new Handler(experimentInformationService);
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

    private final ExperimentInformationService experimentInformationService;

    public Handler(ExperimentInformationService experimentInformationService) {
      this.experimentInformationService = experimentInformationService;
      openDialogueListener();
      experiments.setItems(
          experimentInformationService.listExperimentsWithProject(ProjectId.create()));
      experiments.setRenderer(experimentCardRenderer);
    }

    private void openDialogueListener() {
      createDesignButton.addClickListener(clickEvent -> experimentCreationDialog.open());
      experimentalDesignAddCard.addClickListener(clickEvent -> experimentCreationDialog.open());
    }
  }

  //ToDo this should be provided by a separate ApplicationService class.
  public static interface ExperimentInformationService {

    List<Experiment> listExperimentsWithProject(ProjectId projectId);
  }

  //ToDo this should be provided by a separate ApplicationService class.
  private static interface CreateNewExperimentService {

    Result<Experiment, Exception> createExperimentForProject(ProjectId projectId);
  }

}
