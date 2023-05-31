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
import life.qbic.projectmanagement.application.SampleInformationService.Sample;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;

/**
 * <b>Sample Registration Dialog</b>
 *
 * <p>Component to register {@link Sample} with their associated metadata information</p>
 */
public class BatchRegistrationDialog extends Dialog {

  private static final String TITLE = "Register Batch";
  private final TabSheet tabStepper = new TabSheet();
  private final Tab batchInformationTab = createTabStep("1", "Batch Information");
  private final Tab sampleInformationTab = createTabStep("2", "Register Samples");
  private final BatchInformationLayout batchInformationLayout = new BatchInformationLayout();
  private SampleSpreadsheetLayout sampleSpreadsheetLayout;
  private final RegisterBatchDialogHandler registerBatchDialogHandler;

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

  public void addBatchRegistrationEventListener(
      ComponentEventListener<BatchRegistrationEvent> listener) {
    registerBatchDialogHandler.addBatchRegistrationEventListener(listener);
  }

  public void addSampleRegistrationEventListener(
      ComponentEventListener<SampleRegistrationEvent> listener) {
    registerBatchDialogHandler.addSampleRegistrationEventListener(listener);
  }

  public void addCancelEventListener(
      ComponentEventListener<UserCancelEvent<BatchRegistrationDialog>> listener) {
    registerBatchDialogHandler.addUserCancelEventListener(listener);
  }

  public void resetAndClose() {
    registerBatchDialogHandler.resetAndClose();
  }

  public void setActiveExperiment(Experiment experiment) {
    sampleSpreadsheetLayout.setActiveExperiment(experiment);
  }

  private class RegisterBatchDialogHandler implements Serializable {

    @Serial
    private static final long serialVersionUID = -2692766151162405263L;
    private final List<ComponentEventListener<SampleRegistrationEvent>> sampleRegistrationListeners = new ArrayList<>();
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
        if (event.getSelectedTab() == sampleInformationTab
            && batchInformationLayout.isInputValid()) {
          sampleSpreadsheetLayout.generateSampleRegistrationSheet(
              batchInformationLayout.dataTypeSelection.getValue());
          sampleSpreadsheetLayout.setBatchName(batchInformationLayout.batchNameField.getValue());
        } else {
          tabStepper.setSelectedTab(batchInformationTab);
        }
      });
    }

    private void setSubmissionListeners() {
      setCancelSubmission();
      setBatchRegistrationSubmission();
      setSampleRegistrationSubmission();
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
        sampleSpreadsheetLayout.sampleRegistrationSpreadsheet.reload();
      });
    }

    private void setSampleRegistrationSubmission() {
      sampleSpreadsheetLayout.registerButton.addClickListener(event -> {
        if (isInputValid()) {
          sampleRegistrationListeners.forEach(listener -> listener.onComponentEvent(
              new SampleRegistrationEvent(BatchRegistrationDialog.this, true)));
        }
        sampleSpreadsheetLayout.sampleRegistrationSpreadsheet.reload();
      });
    }

    protected boolean isInputValid() {
      return batchInformationLayout.isInputValid() && sampleSpreadsheetLayout.isInputValid();
    }


    public void addBatchRegistrationEventListener(
        ComponentEventListener<BatchRegistrationEvent> listener) {
      this.batchRegistrationListeners.add(listener);
    }

    public void addSampleRegistrationEventListener(
        ComponentEventListener<SampleRegistrationEvent> listener) {
      this.sampleRegistrationListeners.add(listener);
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
    return new BatchRegistrationContent(batchInformationLayout.batchNameField.getValue(), false);
  }

  public List<SampleRegistrationContent> sampleRegistrationContent() {
    return sampleSpreadsheetLayout.getContent();
  }
}
