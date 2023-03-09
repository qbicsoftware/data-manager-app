package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
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
 * <b>ExperimentListComponent</b>
 * <p>
 * This component shows the
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign} information
 * associated with the selected {@link life.qbic.projectmanagement.domain.project.Project} within
 * the {@link life.qbic.datamanager.views.project.view.ProjectViewPage}
 */
@SpringComponent
@UIScope
public class ExperimentListComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -2255999216830849632L;
  private static final String TITLE = "Experimental Design";
  private final Button createDesignButton = new Button("Add");
  private final ExperimentCreationDialog experimentCreationDialog = new ExperimentCreationDialog();
  private final transient Handler handler;
  private final VerticalLayout contentLayout = new VerticalLayout();
  private final VirtualList<Experiment> experiments = new VirtualList<>();
  private final CardLayout experimentalDesignAddCard = new ExperimentalDesignAddCard();
  private final ComponentRenderer<Component, Experiment> experimentCardRenderer = new ComponentRenderer<>(
      ExperimentalDesignCard::new);

  public ExperimentListComponent(
      @Autowired ExperimentInformationService experimentInformationService) {
    contentLayout.add(experiments);
    contentLayout.add(experimentalDesignAddCard);
    getContent().addTitle(TITLE);
    getContent().addFields(contentLayout);
    this.handler = new Handler(experimentInformationService);
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }


  /**
   * Component logic for the {@link ExperimentListComponent}
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
