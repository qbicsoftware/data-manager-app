package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.layouts.CardComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationPage;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * <b>Experiment Variable Card</b>
 *
 * <p>A PageComponent based Component showing the information stored in the
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable} associated
 * with an experiment {@link life.qbic.projectmanagement.domain.project.experiment.Experiment} in
 * the {@link ExperimentDetailsComponent} of the {@link ExperimentInformationPage}
 */


public class ExperimentalVariablesCard extends CardComponent {

  VerticalLayout experimentalVariablesLayout = new VerticalLayout();
  VerticalLayout noExperimentalVariableLayout = new VerticalLayout();
  private final Button addExperimentalVariableButton = new Button("Add");
  private final Handler handler;
  private Registration addButtonListener;

  public ExperimentalVariablesCard(
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(experimentInformationService);
    this.addTitle("Experimental Variables");
    initEmptyView();
    initVariableView();
    setSizeFull();
    this.handler = new Handler(experimentInformationService);
  }

  public void setAddButtonAction(Runnable runnable) {
    if (addButtonListener != null) {
      addButtonListener.remove();
    }
    addButtonListener = addExperimentalVariableButton.addClickListener(it -> runnable.run());
  }

  public void experimentId(ExperimentId experimentId) {
    handler.setExperimentId(experimentId);
  }

  private void initVariableView() {
    experimentalVariablesLayout.setSizeFull();
    setPadding(false);
    experimentalVariablesLayout.setSpacing(false);
    experimentalVariablesLayout.setMargin(false);
    experimentalVariablesLayout.setPadding(false);
    this.addContent(experimentalVariablesLayout);
  }

  private void initEmptyView() {
    Span templateText = new Span("No Experimental Variables defined");
    addExperimentalVariableButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    noExperimentalVariableLayout.add(templateText, addExperimentalVariableButton);
    noExperimentalVariableLayout.setAlignItems(Alignment.CENTER);
    noExperimentalVariableLayout.setSizeFull();
    this.addContent(noExperimentalVariableLayout);
  }

  public void refresh() {
    handler.refresh();
  }

  private final class Handler {

    private final ExperimentInformationService experimentInformationService;
    private ExperimentId experimentId;

    public Handler(ExperimentInformationService experimentInformationService) {
      this.experimentInformationService = experimentInformationService;
    }

    public void setExperimentId(ExperimentId experimentId) {
      this.experimentId = experimentId;
      loadExperimentalVariableInformation();
    }

    private void loadExperimentalVariableInformation() {
      List<ExperimentalVariable> experimentalVariables = experimentInformationService.getVariablesOfExperiment(
          experimentId);
      if (experimentalVariables.isEmpty()) {
        showEmptyView();
      } else {
        setExperimentalVariables(experimentalVariables);
      }
    }

    public void refresh() {
      loadExperimentalVariableInformation();
    }

    public void setExperimentalVariables(List<ExperimentalVariable> experimentalVariables) {
      experimentalVariablesLayout.removeAll();
      for (ExperimentalVariable experimentalVariable : experimentalVariables) {
        ExperimentalVariablePanel experimentalVariablePanel = new ExperimentalVariablePanel(
            experimentalVariable);
        experimentalVariablesLayout.add(experimentalVariablePanel);
      }
      showVariablesView();
    }

    private void showEmptyView() {
      experimentalVariablesLayout.setVisible(false);
      noExperimentalVariableLayout.setVisible(true);
    }

    private void showVariablesView() {
      noExperimentalVariableLayout.setVisible(false);
      experimentalVariablesLayout.setVisible(true);
    }
  }
}
