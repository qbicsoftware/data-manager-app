package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Left;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Top;
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

  private class RegisterBatchDialogHandler {

    public RegisterBatchDialogHandler() {
      resetDialogueUponClosure();
      setGeneralInformationButtonsListeners();
      setSampleMetadataButtonsListeners();
      setTabSelectionListener();
    }

    private void setGeneralInformationButtonsListeners() {
      generalInformationLayout.cancelButton.addClickListener(buttonClickEvent -> closeAndReset());
      generalInformationLayout.nextButton.addClickListener(
          event -> tabStepper.setSelectedTab(sampleMetadataTab));
    }

    private void setSampleMetadataButtonsListeners() {
      sampleMetadataLayout.cancelButton.addClickListener(buttonClickEvent -> closeAndReset());
      //ToDo Register metadata
      sampleMetadataLayout.nextButton.addClickListener(buttonClickEvent -> closeAndReset());
    }

    private void setTabSelectionListener() {
      tabStepper.addSelectedChangeListener(event -> {
        if (event.getSelectedTab() == sampleMetadataTab) {
          sampleMetadataLayout.generateSampleRegistrationSheet(
              generalInformationLayout.dataTypeSelection.getValue());
        }
      });
    }

    public void closeAndReset() {
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
      addDialogCloseActionListener(closeActionEvent -> closeAndReset());
    }
  }
}
