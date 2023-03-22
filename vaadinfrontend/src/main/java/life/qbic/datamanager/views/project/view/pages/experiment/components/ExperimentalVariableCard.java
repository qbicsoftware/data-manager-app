package life.qbic.datamanager.views.project.view.pages.experiment.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;


/**
 * <b>Experiment Variable Card</b>
 *
 * <p>A CardLayout based Component showing the information stored in the
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable} associated
 * with an experiment {@link life.qbic.projectmanagement.domain.project.experiment.Experiment} in
 * the {@link ExperimentDetailsComponent} of the
 * {@link life.qbic.datamanager.views.project.view.pages.experiment.ExperimentInformationPage}
 */


public class ExperimentalVariableCard extends CardLayout {

  public final AddVariableToExperimentDialog addVariableToExperimentDialog = new AddVariableToExperimentDialog();
  FormLayout experimentalVariablesFormLayout = new FormLayout();
  VerticalLayout noExperimentalVariableLayout = new VerticalLayout();
  private final Button addExperimentalVariableButton = new Button("Add");

  public ExperimentalVariableCard() {
    addTitle("Experimental Variables");
    initEmptyView();
    initVariableView();
    setAddExperimentalVariableButtonListener();
    setSizeFull();
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

  //ToDo stop card growth if many variables are defined
  public void setExperimentalVariables(List<ExperimentalVariable> experimentalVariables) {
    if (experimentalVariables.isEmpty()) {
      showEmptyView();
    } else {
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
  }

  private void setAddExperimentalVariableButtonListener() {
    addExperimentalVariableButton.addClickListener(event -> addVariableToExperimentDialog.open());
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
