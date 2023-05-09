package life.qbic.datamanager.views.projects.project.experiments.experiment.create;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.datamanager.views.projects.create.DefineExperimentComponent;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;

/**
 * <b>Experiment Creation Dialog</b>
 *
 * <p>Dialog to create an experiment by providing the minimal required information in the
 * {@link DefineExperimentComponent}</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class ExperimentCreationDialog extends Dialog {

  @Serial
  private static final long serialVersionUID = -7493719543408177228L;
  private final String TITLE = "Experimental Design";
  private final DefineExperimentComponent defineExperimentComponent;
  private final Button createButton = new Button("Create");
  private final Button cancelButton = new Button("Cancel");
  private final ExperimentCreationDialogHandler experimentCreationDialogHandler;


  public ExperimentCreationDialog(ExperimentalDesignSearchService experimentalDesignSearchService) {
    defineExperimentComponent = new DefineExperimentComponent(experimentalDesignSearchService);
    experimentCreationDialogHandler = new ExperimentCreationDialogHandler();
    setHeaderTitle(TITLE);
    add(defineExperimentComponent);
    getFooter().add(cancelButton, createButton);
    styleExperimentCreationDialog();
  }

  private void styleExperimentCreationDialog() {
    this.setMinWidth(66, Unit.VW);
    this.setMaxWidth(66, Unit.VW);
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

  public void resetAndClose() {
    experimentCreationDialogHandler.resetAndClose();
  }

  public void addExperimentCreationEventListener(
      ComponentEventListener<ExperimentCreationEvent> listener) {
    experimentCreationDialogHandler.addExperimentCreationEventListener(listener);
  }

  public void addCancelEventListener(
      ComponentEventListener<UserCancelEvent<ExperimentCreationDialog>> listener) {
    experimentCreationDialogHandler.addUserCancelEventListener(listener);
  }

  public ExperimentCreationContent content() {
    return new ExperimentCreationContent(defineExperimentComponent.experimentNameField.getValue(),
        defineExperimentComponent.speciesBox.getValue().stream().toList(),
        defineExperimentComponent.specimenBox.getValue().stream().toList(),
        defineExperimentComponent.analyteBox.getValue().stream().toList());
  }

  private class ExperimentCreationDialogHandler {

    private final List<ComponentEventListener<ExperimentCreationEvent>> listeners = new ArrayList<>();
    private final List<ComponentEventListener<UserCancelEvent<ExperimentCreationDialog>>> cancelListeners = new ArrayList<>();

    public ExperimentCreationDialogHandler() {
      resetDialogueUponClosure();
      closeDialogueViaCancelButton();
      configureExperimentCreation();
    }

    private void configureExperimentCreation() {
      createButton.addClickListener(event -> {
        validateInput();
        if (isInputValid()) {
          listeners.forEach(listener -> listener.onComponentEvent(
              new ExperimentCreationEvent(ExperimentCreationDialog.this, true)));
        }
      });
      cancelButton.addClickListener(event -> cancelListeners.forEach(
          listener -> listener.onComponentEvent(
              new UserCancelEvent<>(ExperimentCreationDialog.this))));
    }

    public void resetAndClose() {
      close();
      reset();
    }

    /**
     * Resets the values and validity of all components that implement value storing and validity
     * interfaces
     */
    public void reset() {
      defineExperimentComponent.reset();
    }

    private void closeDialogueViaCancelButton() {
      cancelButton.addClickListener(buttonClickEvent -> resetAndClose());
    }

    private void resetDialogueUponClosure() {
      // Calls the reset method for all possible closure methods of the dialogue window:
      addDialogCloseActionListener(closeActionEvent -> resetAndClose());
    }

    public boolean isInputValid() {
      return validateInput();
    }

    protected boolean validateInput() {
      return defineExperimentComponent.isValid();
    }

    public void addExperimentCreationEventListener(
        ComponentEventListener<ExperimentCreationEvent> listener) {
      this.listeners.add(listener);
    }

    public void addUserCancelEventListener(
        ComponentEventListener<UserCancelEvent<ExperimentCreationDialog>> listener) {
      this.cancelListeners.add(listener);
    }
  }
}
