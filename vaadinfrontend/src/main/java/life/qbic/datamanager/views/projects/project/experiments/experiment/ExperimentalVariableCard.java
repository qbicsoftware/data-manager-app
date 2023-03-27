package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationPage;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * <b>Experiment Variable Card</b>
 *
 * <p>A CardLayout based Component showing the information stored in the
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable} associated
 * with an experiment {@link life.qbic.projectmanagement.domain.project.experiment.Experiment} in
 * the {@link ExperimentDetailsComponent} of the {@link ExperimentInformationPage}
 */


public class ExperimentalVariableCard extends CardLayout {

  private AddVariableToExperimentDialog addVariableToExperimentDialog;
  FormLayout experimentalVariablesFormLayout = new FormLayout();
  VerticalLayout noExperimentalVariableLayout = new VerticalLayout();
  private final Button addExperimentalVariableButton = new Button("Add");
  private final Handler handler;

  public ExperimentalVariableCard(
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(experimentInformationService);
    addTitle("Experimental Variables");
    initEmptyView();
    initVariableView();
    initAddExperimentDialog(experimentInformationService);
    setSizeFull();
    this.handler = new Handler(experimentInformationService);
  }

  public void experimentId(ExperimentId experimentId) {
    handler.setExperimentId(experimentId);
    addVariableToExperimentDialog.experimentId(experimentId);
  }

  private void initVariableView() {
    experimentalVariablesFormLayout.setSizeFull();
    addFields(experimentalVariablesFormLayout);
  }

  private void initEmptyView() {
    Span templateText = new Span("No Experimental Variables defined");
    addExperimentalVariableButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    noExperimentalVariableLayout.add(templateText, addExperimentalVariableButton);
    noExperimentalVariableLayout.setAlignItems(Alignment.CENTER);
    noExperimentalVariableLayout.setSizeFull();
    addFields(noExperimentalVariableLayout);
  }

  private void initAddExperimentDialog(ExperimentInformationService experimentInformationService) {
    addVariableToExperimentDialog = new AddVariableToExperimentDialog(experimentInformationService);
  }

  private final class Handler {

    private final ExperimentInformationService experimentInformationService;
    private ExperimentId experimentId;

    public Handler(ExperimentInformationService experimentInformationService) {
      this.experimentInformationService = experimentInformationService;
      setAddExperimentalVariableButtonListener();
      setCloseDialogListener();
    }

    public void setExperimentId(ExperimentId experimentId) {
      this.experimentId = experimentId;
      loadExperimentalVariableInformation();
    }

    private void setAddExperimentalVariableButtonListener() {
      addExperimentalVariableButton.addClickListener(event -> addVariableToExperimentDialog.open());
    }

    private void setCloseDialogListener() {
      addVariableToExperimentDialog.addOpenedChangeListener(openedChangeEvent -> {
        if (!openedChangeEvent.isOpened()) {
          loadExperimentalVariableInformation();
        }
      });
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

    public void setExperimentalVariables(List<ExperimentalVariable> experimentalVariables) {
      experimentalVariablesFormLayout.removeAll();
      for (ExperimentalVariable experimentalVariable : experimentalVariables) {
        VerticalLayout experimentalVariableLayout = new VerticalLayout();
        experimentalVariablesFormLayout.addFormItem(experimentalVariableLayout,
            experimentalVariable.name().value());
        experimentalVariable.levels().forEach(level -> {
          String levelWithUnit = level.value() + " " + level.unit().orElse("");
          experimentalVariableLayout.add(new Span(levelWithUnit));
        });
      }
      showVariablesView();
    }

    private void showEmptyView() {
      experimentalVariablesFormLayout.setVisible(false);
      noExperimentalVariableLayout.setVisible(true);
    }

    private void showVariablesView() {
      noExperimentalVariableLayout.setVisible(false);
      experimentalVariablesFormLayout.setVisible(true);
    }
  }


}
