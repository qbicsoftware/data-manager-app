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
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class RegisterBatchDialog extends Dialog {

  private final Span title = new Span("Upload Laboratory Metadata");
  private final TabSheet tabStepper = new TabSheet();
  private final Tab generalInformationTab = createTabStep("1", "General Information");
  private final Tab sampleMetadataTab = createTabStep("2", "Sample Metadata");
  private final GeneralInformationLayout generalInformationLayout = new GeneralInformationLayout();
  private SampleMetadataLayout sampleMetadataLayout;
  private final RegisterBatchDialogHandler registerBatchDialogHandler;

  public RegisterBatchDialog(@Autowired SampleRegistrationService sampleRegistrationService) {
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
    tabStepper.add(sampleMetadataTab, sampleMetadataLayout);
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
    sampleMetadataLayout = new SampleMetadataLayout(sampleRegistrationService);
  }

  public void addSampleRegistrationEventListener(
      ComponentEventListener<SampleRegistrationEvent> listener) {
    registerBatchDialogHandler.addSampleRegistrationEventListener(listener);
  }

  public void addCancelEventListener(
      ComponentEventListener<UserCancelEvent<RegisterBatchDialog>> listener) {
    registerBatchDialogHandler.addUserCancelEventListener(listener);
  }

  public void resetAndClose() {
    registerBatchDialogHandler.resetAndClose();
  }

  //ToDo Replace with values in Spreadsheet
  public List<String> content() {
    List<String> exampleBatch = new ArrayList<String>();
    exampleBatch.addAll(List.of("SampleInfo1", "SampleInfo2", "SampleInfo3"));
    return exampleBatch;
  }

  private class RegisterBatchDialogHandler {

    private final List<ComponentEventListener<SampleRegistrationEvent>> listeners = new ArrayList<>();
    private final List<ComponentEventListener<UserCancelEvent<RegisterBatchDialog>>> cancelListeners = new ArrayList<>();

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
          listener -> listener.onComponentEvent(new UserCancelEvent<>(RegisterBatchDialog.this))));
    }

    private void setSampleRegistrationSubmission() {
      sampleMetadataLayout.nextButton.addClickListener(event -> {
        if (isInputValid()) {
          listeners.forEach(listener -> listener.onComponentEvent(
              new SampleRegistrationEvent(RegisterBatchDialog.this, true)));
        }
      });
      sampleMetadataLayout.cancelButton.addClickListener(event -> cancelListeners.forEach(
          listener -> listener.onComponentEvent(new UserCancelEvent<>(RegisterBatchDialog.this))));
    }

    protected boolean isInputValid() {
      return generalInformationLayout.isInputValid() && sampleMetadataLayout.isInputValid();
    }

    private void setTabSelectionListener() {
      tabStepper.addSelectedChangeListener(event -> {
        if (event.getSelectedTab() == sampleMetadataTab
            && generalInformationLayout.isInputValid()) {
          sampleMetadataLayout.generateSampleRegistrationSheet(
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
        ComponentEventListener<UserCancelEvent<RegisterBatchDialog>> listener) {
      this.cancelListeners.add(listener);
    }

    public void resetAndClose() {
      close();
      reset();
    }

    private void reset() {
      generalInformationLayout.reset();
      sampleMetadataLayout.reset();
      tabStepper.setSelectedTab(generalInformationTab);
    }

    private void resetDialogueUponClosure() {
      // Calls the reset method for all possible closure methods of the dialogue window:
      addDialogCloseActionListener(closeActionEvent -> resetAndClose());
    }

  }
}
