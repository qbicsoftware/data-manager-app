package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Left;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Top;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.SampleInformationService.Sample;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;

/**
 * <b>Sample Registration Dialog</b>
 *
 * <p>Component to register {@link Sample} with their associated metadata information</p>
 */
public class SampleRegistrationDialog extends Dialog {

  private final Span title = new Span("Upload Laboratory Metadata");
  private final TabSheet tabStepper = new TabSheet();
  private final Tab generalInformationTab = createTabStep("1", "General Information");
  private final Tab sampleMetadataTab = createTabStep("2", "Sample Metadata");
  private final GeneralInformationLayout generalInformationLayout = new GeneralInformationLayout();
  private SampleSpreadsheetLayout sampleSpreadsheetLayout;
  private final RegisterBatchDialogHandler registerBatchDialogHandler;

  public SampleRegistrationDialog(SampleRegistrationService sampleRegistrationService) {
    add(title);
    title.addClassNames("text-2xl", "font-bold", "text-secondary");
    initSampleMetadataLayout(sampleRegistrationService);
    initTabStepper();
    styleStepper();
    registerBatchDialogHandler = new RegisterBatchDialogHandler();
    this.setSizeFull();
  }

  private void initTabStepper() {
    tabStepper.add(generalInformationTab, generalInformationLayout);
    tabStepper.add(sampleMetadataTab, sampleSpreadsheetLayout);
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

  private void initSampleMetadataLayout(SampleRegistrationService sampleRegistrationService) {
    sampleSpreadsheetLayout = new SampleSpreadsheetLayout(sampleRegistrationService);
  }

  public void addSampleRegistrationEventListener(
      ComponentEventListener<SampleRegistrationEvent> listener) {
    registerBatchDialogHandler.addSampleRegistrationEventListener(listener);
  }

  public void addCancelEventListener(
      ComponentEventListener<UserCancelEvent<SampleRegistrationDialog>> listener) {
    registerBatchDialogHandler.addUserCancelEventListener(listener);
  }

  public void resetAndClose() {
    registerBatchDialogHandler.resetAndClose();
  }

  //ToDo Replace with values in Spreadsheet
  public List<String> content() {
    List<String> exampleBatch = new ArrayList<>(
        List.of("SampleInfo1", "SampleInfo2", "SampleInfo3"));
    return exampleBatch;
  }

  public void setActiveExperiment(Experiment experiment) {
    sampleSpreadsheetLayout.setActiveExperiment(experiment);
  }

  private class RegisterBatchDialogHandler {

    private final List<ComponentEventListener<SampleRegistrationEvent>> listeners = new ArrayList<>();
    private final List<ComponentEventListener<UserCancelEvent<SampleRegistrationDialog>>> cancelListeners = new ArrayList<>();

    public RegisterBatchDialogHandler() {
      resetDialogueUponClosure();
      setGeneralInformationButtonsListeners();
      setSampleRegistrationSubmission();
      setTabSelectionListener();
    }

    private void setGeneralInformationButtonsListeners() {
      generalInformationLayout.nextButton.addClickListener(
          event -> tabStepper.setSelectedTab(sampleMetadataTab));
      generalInformationLayout.cancelButton.addClickListener(event -> cancelListeners.forEach(
          listener -> listener.onComponentEvent(
              new UserCancelEvent<>(SampleRegistrationDialog.this))));
    }

    private void setSampleRegistrationSubmission() {
      sampleSpreadsheetLayout.nextButton.addClickListener(event -> {
        if (isInputValid()) {
          listeners.forEach(listener -> listener.onComponentEvent(
              new SampleRegistrationEvent(SampleRegistrationDialog.this, true)));
        }
      });
      sampleSpreadsheetLayout.cancelButton.addClickListener(event -> cancelListeners.forEach(
          listener -> listener.onComponentEvent(
              new UserCancelEvent<>(SampleRegistrationDialog.this))));
    }

    protected boolean isInputValid() {
      return generalInformationLayout.isInputValid() && sampleSpreadsheetLayout.isInputValid();
    }

    private void setTabSelectionListener() {
      tabStepper.addSelectedChangeListener(event -> {
        if (event.getSelectedTab() == sampleMetadataTab
            && generalInformationLayout.isInputValid()) {
          sampleSpreadsheetLayout.generateSampleRegistrationSheet(
              generalInformationLayout.dataTypeSelection.getValue());
        } else {
          tabStepper.setSelectedTab(generalInformationTab);
        }
      });
    }

    public void addSampleRegistrationEventListener(
        ComponentEventListener<SampleRegistrationEvent> listener) {
      this.listeners.add(listener);
    }

    public void addUserCancelEventListener(
        ComponentEventListener<UserCancelEvent<SampleRegistrationDialog>> listener) {
      this.cancelListeners.add(listener);
    }

    public void resetAndClose() {
      close();
      reset();
    }

    private void reset() {
      generalInformationLayout.reset();
      sampleSpreadsheetLayout.reset();
      tabStepper.setSelectedTab(generalInformationTab);
    }

    private void resetDialogueUponClosure() {
      // Calls the reset method for all possible closure methods of the dialogue window:
      addDialogCloseActionListener(closeActionEvent -> resetAndClose());
    }

  }
}
