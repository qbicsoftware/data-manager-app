package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Left;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Top;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;

;

/**
 * <b>Sample Registration Dialog</b>
 *
 * <p>Component to register {@link life.qbic.projectmanagement.domain.project.sample.Sample} with
 * their associated metadata information</p>
 */
public class BatchRegistrationDialog extends Dialog {

  private static final String TITLE = "Register Batch";
  private final TabSheet tabStepper = new TabSheet();
  private final Tab batchInformationTab = createTabStep("1", "Batch Information");
  private final Tab sampleInformationTab = createTabStep("2", "Register Samples");
  private final BatchInformationLayout batchInformationLayout = new BatchInformationLayout();
  private SampleSpreadsheetLayout sampleSpreadsheetLayout;
  private final transient RegisterBatchDialogHandler registerBatchDialogHandler;

  public BatchRegistrationDialog() {
    setHeaderTitle(TITLE);
    initSampleRegistrationLayout();
    initTabStepper();
    styleStepper();
    registerBatchDialogHandler = new RegisterBatchDialogHandler();
    this.setSizeFull();
  }

  private void initTabStepper() {
    tabStepper.add(batchInformationTab, batchInformationLayout);
    tabStepper.add(sampleInformationTab, sampleSpreadsheetLayout);
    add(tabStepper);
  }

  private Tab createTabStep(String avatarLabel, String tabLabel) {
    Avatar stepAvatar = new Avatar(avatarLabel);
    stepAvatar.setColorIndex(2);
    Span tabLabelSpan = new Span(tabLabel);
    tabLabelSpan.setClassName(Top.SMALL);
    Tab tabStep = new Tab(stepAvatar, tabLabelSpan);
    tabStep.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
    tabStep.setClassName(Left.MEDIUM);
    return tabStep;
  }

  private void styleStepper() {
    tabStepper.setSizeFull();
    tabStepper.setClassName("minimal");
  }

  private void initSampleRegistrationLayout() {
    sampleSpreadsheetLayout = new SampleSpreadsheetLayout();
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
      ComponentEventListener<UserCancelEvent<BatchRegistrationDialog>> listener) {
    registerBatchDialogHandler.addUserCancelEventListener(listener);
  }

  /**
   * Resets the dialogue to its original state, removing all user input and changes and closes the
   * dialog window
   */
  public void resetAndClose() {
    registerBatchDialogHandler.resetAndClose();
  }

  /**
   * Defines the currently active {@link Experiment} within the project from which the information
   * will be derived in the {@link SampleRegistrationSpreadsheet}
   */
  public void setExperiments(List<Experiment> experiments) {
    batchInformationLayout.experimentSelect.setItems(experiments);
  }

  private class RegisterBatchDialogHandler implements Serializable {

    @Serial
    private static final long serialVersionUID = -2692766151162405263L;
    private final List<ComponentEventListener<BatchRegistrationEvent>> batchRegistrationListeners = new ArrayList<>();
    private final List<ComponentEventListener<UserCancelEvent<BatchRegistrationDialog>>> cancelListeners = new ArrayList<>();

    public RegisterBatchDialogHandler() {
      setNavigationListeners();
      setSubmissionListeners();
      resetDialogueUponClosure();
    }

    private void setNavigationListeners() {
      batchInformationLayout.nextButton.addClickListener(
          event -> tabStepper.setSelectedTab(sampleInformationTab));
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
      sampleSpreadsheetLayout.setBatchName(batchInformationLayout.batchNameField.getValue());
      sampleSpreadsheetLayout.setExperiment(batchInformationLayout.experimentSelect.getValue());
      sampleSpreadsheetLayout.generateSampleRegistrationSheet(
          batchInformationLayout.dataTypeSelection.getValue());
    }

    private void setSubmissionListeners() {
      setCancelSubmission();
      setBatchRegistrationSubmission();
    }

    private void setCancelSubmission() {
      batchInformationLayout.cancelButton.addClickListener(event -> cancelListeners.forEach(
          listener -> listener.onComponentEvent(
              new UserCancelEvent<>(BatchRegistrationDialog.this))));
      sampleSpreadsheetLayout.cancelButton.addClickListener(event -> cancelListeners.forEach(
          listener -> listener.onComponentEvent(
              new UserCancelEvent<>(BatchRegistrationDialog.this))));
    }

    private void setBatchRegistrationSubmission() {
      sampleSpreadsheetLayout.registerButton.addClickListener(event -> {
        if (isInputValid()) {
          batchRegistrationListeners.forEach(listener -> listener.onComponentEvent(
              new BatchRegistrationEvent(BatchRegistrationDialog.this, true)));
        }
      });
    }

    protected boolean isInputValid() {
      return batchInformationLayout.isInputValid() && sampleSpreadsheetLayout.isInputValid();
    }


    public void addBatchRegistrationEventListener(
        ComponentEventListener<BatchRegistrationEvent> listener) {
      this.batchRegistrationListeners.add(listener);
    }

    public void addUserCancelEventListener(
        ComponentEventListener<UserCancelEvent<BatchRegistrationDialog>> listener) {
      this.cancelListeners.add(listener);
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
}
