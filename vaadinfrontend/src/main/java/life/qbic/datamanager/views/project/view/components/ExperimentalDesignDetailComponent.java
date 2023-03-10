package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.project.experiment.ExperimentCreationDialog;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>ExperimentalDesignDetailComponent</b>
 * <p>
 * This component shows the {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign} information associated with the selected {@link life.qbic.projectmanagement.domain.project.Project}
 * within the {@link life.qbic.datamanager.views.project.view.ProjectViewPage}
 *
 */
@SpringComponent
@UIScope
public class ExperimentalDesignDetailComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -2255999216830849632L;
  private static final String TITLE = "Experimental Design";
  private final Button createDesignButton = new Button("Add");
  private final ExperimentCreationDialog experimentCreationDialog;
  private final transient Handler handler;
  private final VirtualList<Experiment> experiments = new VirtualList<>();
  private final CardLayout experimentalDesignAddCard = new ExperimentalDesignAddCard();
  private final ComponentRenderer<Component, Experiment> experimentCardRenderer = new ComponentRenderer<>(
      ExperimentalDesignCard::new);

  public ExperimentalDesignDetailComponent(
      @Autowired ExperimentCreationDialog experimentCreationDialog,
      @Autowired ProjectInformationService projectInformationService) {

    this.experimentCreationDialog = experimentCreationDialog;
    VerticalLayout contentLayout = new VerticalLayout();
    contentLayout.add(experiments);
    contentLayout.add(experimentalDesignAddCard);
    getContent().addTitle(TITLE);
    getContent().addFields(contentLayout);
    this.handler = new Handler(projectInformationService);
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  public void projectId(String parameter) {
    this.handler.setProjectId(ProjectId.parse(parameter));
  }


  /**
   * Component logic for the {@link ExperimentalDesignDetailComponent}
   *
   * @since 1.0.0
   */
  private final class Handler {

    private final ProjectInformationService projectInformationService;
    private ProjectId projectId;

    public Handler(ProjectInformationService projectInformationService) {
      this.projectInformationService = projectInformationService;
      openDialogueListener();
      experiments.setRenderer(experimentCardRenderer);
    }

    public void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      CallbackDataProvider<Experiment, Void> experimentsDataProvider = DataProvider.fromCallbacks(
          query -> projectInformationService.getExperimentsForProject(projectId).stream()
              .skip(query.getOffset()).limit(query.getLimit()),
          query -> projectInformationService.getExperimentsForProject(projectId).size());
      experiments.setDataProvider(experimentsDataProvider);
    }

    private void openDialogueListener() {
      createDesignButton.addClickListener(clickEvent -> experimentCreationDialog.open(projectId));
      experimentalDesignAddCard.addClickListener(
          clickEvent -> experimentCreationDialog.open(projectId));
      experimentCreationDialog.addOpenedChangeListener(
          event -> {
            if (!event.isOpened()) {
              experiments.getDataProvider().refreshAll();
            }
          });
    }
  }

}
