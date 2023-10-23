package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabVariant;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.controlling.domain.model.experiment.Experiment;
import life.qbic.controlling.domain.model.experiment.ExperimentId;
import life.qbic.controlling.domain.model.sample.Sample;

/**
 * <b>Sample Registration Dialog</b>
 *
 * <p>Component to register {@link Sample} with
 * their associated metadata information</p>
 */
public class BatchRegistrationDialog extends Dialog {

  private static final String TITLE = "Register Batch";
  private final TabSheet tabStepper = new TabSheet();
  private final Tab batchInformationTab = createTabStep("1", "Batch Information");
  private final Tab sampleInformationTab = createTabStep("2", "Register Samples");
  private final BatchInformationLayout batchInformationLayout = new BatchInformationLayout();
  private final SampleSpreadsheetLayout sampleSpreadsheetLayout = new SampleSpreadsheetLayout();
  private final transient RegisterBatchDialogHandler registerBatchDialogHandler;
  private boolean wasPrefillPreviouslySelected = false;

  public BatchRegistrationDialog() {
    addClassName("batch-registration-dialog");
    setResizable(true);
    setHeaderTitle(TITLE);
    initTabStepper();
    registerBatchDialogHandler = new RegisterBatchDialogHandler();
  }

  private void initTabStepper() {
    tabStepper.add(batchInformationTab, batchInformationLayout);
    tabStepper.add(sampleInformationTab, sampleSpreadsheetLayout);
    tabStepper.addClassName("minimal");
    tabStepper.addClassName("stepper");
    add(tabStepper);
  }

  private Tab createTabStep(String avatarLabel, String tabLabel) {
    Avatar stepAvatar = new Avatar(avatarLabel);
    stepAvatar.setColorIndex(2);
    Span tabLabelSpan = new Span(tabLabel);
    Tab tabStep = new Tab(stepAvatar, tabLabelSpan);
    tabStep.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
    return tabStep;
  }

  /**
   * Adds the provided {@link ComponentEventListener} to the list of listeners that will be notified
   * if an {@link BatchRegistrationEvent} occurs within this Dialog
   *
   * @param listener The {@link ComponentEventListener} to be notified
   */
  public void addBatchRegistrationEventListener(
      ComponentEventListener<BatchRegistrationEvent> listener) {
    registerBatchDialogHandler.addBatchRegistrationEventListener(listener);
  }

  /**
   * Adds the provided {@link ComponentEventListener} to the list of listeners that will be notified
   * if an {@link UserCancelEvent} occurs within this Dialog
   *
   * @param listener The {@link ComponentEventListener} to be notified
   */
  public void addCancelEventListener(
      ComponentEventListener<CancelEvent> listener) {
    registerBatchDialogHandler.addUserCancelEventListener(listener);
  }

  /**
   * Defines the currently active {@link Experiment} within the project from which the information
   * will be derived in the {@link SampleRegistrationSpreadsheet}
   */
  public void setExperiments(Collection<Experiment> experiments) {
    batchInformationLayout.experimentSelect.setItems(experiments);
  }

  /**
   * Sets the provided {@link Experiment} as preselected in the
   * {@link com.vaadin.flow.component.select.Select} component
   */
  public void setSelectedExperiment(Experiment experiment) {
    batchInformationLayout.experimentSelect.setValue(experiment);
  }

  public void resetAndClose() {
    registerBatchDialogHandler.resetAndClose();
  }

  private class RegisterBatchDialogHandler implements Serializable {

    @Serial
    private static final long serialVersionUID = -2692766151162405263L;

    public RegisterBatchDialogHandler() {
      setNavigationListeners();
      setSubmissionListeners();
      resetDialogueUponClosure();
    }

    private void setNavigationListeners() {
      batchInformationLayout.nextButton.addClickListener(
          event -> tabStepper.setSelectedTab(sampleInformationTab));
      sampleSpreadsheetLayout.backButton.addClickListener(
          event -> tabStepper.setSelectedTab(batchInformationTab));
      setTabSelectionListener();
    }

    private void setTabSelectionListener() {
      tabStepper.addSelectedChangeListener(event -> {
        if (event.getSelectedTab() == sampleInformationTab) {
          if (batchInformationLayout.isInputValid()) {
            generateSampleRegistrationLayout();
            tabStepper.setSelectedTab(event.getSelectedTab());
          } else {
            tabStepper.setSelectedTab(event.getPreviousTab());
          }
        }
      });
    }

    private void generateSampleRegistrationLayout() {
      //we always set the batch name, as it can change without affecting the rest of the spreadsheet
      sampleSpreadsheetLayout.setBatchName(batchInformationLayout.batchNameField.getValue());

      //We reset the spreadsheet independent on which selection was changed
      if (hasPrefillStatusChanged() || hasExperimentInformationChanged()) {
        sampleSpreadsheetLayout.resetSpreadSheet();
        //If the user changes the experiment or selects one for the first time the spreadsheet has to be generated anew
        if (hasExperimentInformationChanged()) {
          sampleSpreadsheetLayout.resetExperimentInformation();
          sampleSpreadsheetLayout.setExperiment(batchInformationLayout.experimentSelect.getValue());
        }
        sampleSpreadsheetLayout.generateSampleRegistrationSheet(
            batchInformationLayout.dataTypeSelection.getValue());
        //If the user changed the prefill selection the fields have to be filled dependent on if the checkbox was checked or not
        if (hasPrefillStatusChanged()) {
          boolean isPrefillSelected = batchInformationLayout.prefillSelection.getValue();
          if (isPrefillSelected) {
            sampleSpreadsheetLayout.prefillConditionsAndReplicates();
          }
          wasPrefillPreviouslySelected = isPrefillSelected;
        }
      }
      //rerender spreadsheet
      sampleSpreadsheetLayout.reloadSpreadsheet();
    }

    private boolean hasExperimentInformationChanged() {
      ExperimentId previouslySelectedExperiment = sampleSpreadsheetLayout.getExperiment();
      ExperimentId selectedExperiment = batchInformationLayout.experimentSelect.getValue()
          .experimentId();
      if (previouslySelectedExperiment == null) {
        return true;
      } else {
        return !previouslySelectedExperiment.equals(selectedExperiment);
      }
    }

    private boolean hasPrefillStatusChanged() {
      return wasPrefillPreviouslySelected != batchInformationLayout.prefillSelection.getValue();
    }

    private void setSubmissionListeners() {
      setCancelSubmission();
      setBatchRegistrationSubmission();
    }

    private void setCancelSubmission() {
      batchInformationLayout.cancelButton.addClickListener(event ->
          fireEvent(new UserCancelEvent<>(BatchRegistrationDialog.this)));
      sampleSpreadsheetLayout.cancelButton.addClickListener(event ->
          fireEvent(new UserCancelEvent<>(BatchRegistrationDialog.this)));
    }

    private void setBatchRegistrationSubmission() {
      sampleSpreadsheetLayout.registerButton.addClickListener(event -> {
        if (isInputValid()) {
          fireEvent(new BatchRegistrationEvent(BatchRegistrationDialog.this, true));
        }
      });
    }

    protected boolean isInputValid() {
      return batchInformationLayout.isInputValid() && sampleSpreadsheetLayout.isInputValid();
    }

    public void addBatchRegistrationEventListener(
        ComponentEventListener<BatchRegistrationEvent> listener) {
      addListener(BatchRegistrationEvent.class, listener);
    }

    public void addUserCancelEventListener(
        ComponentEventListener<CancelEvent> listener) {
      addListener(CancelEvent.class, listener);

    }

    public void resetAndClose() {
      close();
      reset();
    }

    private void reset() {
      batchInformationLayout.reset();
      sampleSpreadsheetLayout.reset();
      tabStepper.setSelectedTab(batchInformationTab);
    }

    private void resetDialogueUponClosure() {
      // Calls the reset method for all possible closure methods of the dialogue window:
      addDialogCloseActionListener(closeActionEvent -> resetAndClose());
    }

  }

  public BatchRegistrationContent batchRegistrationContent() {
    return new BatchRegistrationContent(batchInformationLayout.batchNameField.getValue(),
        batchInformationLayout.experimentSelect.getValue().experimentId(), false);
  }

  public List<SampleRegistrationContent> sampleRegistrationContent() {
    return sampleSpreadsheetLayout.getContent();
  }

  public static class CancelEvent extends UserCancelEvent<BatchRegistrationDialog> {

    @Serial
    private static final long serialVersionUID = 8348902371944310641L;

    public CancelEvent(BatchRegistrationDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

}
