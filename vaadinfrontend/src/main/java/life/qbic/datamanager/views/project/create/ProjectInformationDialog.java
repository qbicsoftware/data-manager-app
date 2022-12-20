package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;

/**
 * <b>Create Project Component</b>
 *
 * <p>Component to create a project based on a project intent</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class ProjectInformationDialog extends Dialog {

  private final Handler handler;
  public ComboBox<OfferPreview> searchField;
  private final TextField titleField;
  public final Button createButton;
  public final Button cancelButton;
  private final FormLayout formLayout;
  private final VerticalLayout experimentalDesignIntroduction;
  private final TextArea experimentalDesignField;
  private final TextArea projectObjective;
  public final ComboBox<PersonReference> projectManager;
  public final ComboBox<PersonReference> principalInvestigator;
  public final HorizontalLayout sampleCountLayout;
  public final IntegerField sampleCountField;
  public final MultiSelectComboBox<String> organismBox;
  public final MultiSelectComboBox<String> specimenBox;
  public final MultiSelectComboBox<String> analyteBox;

  public ProjectInformationDialog() {
    searchField = new ComboBox<>("Offer");
    formLayout = new FormLayout();
    titleField = new TextField("Title");
    titleField.setRequired(true);
    projectObjective = new TextArea("Objective");
    projectObjective.setRequired(true);
    //ToDo Remove Field and move logic to MultiSelectComboBoxes
    experimentalDesignField = new TextArea();
    experimentalDesignIntroduction = new VerticalLayout();
    initExperimentalDesignIntroduction();
    //Layout with max width to keep the SampleCountField in a seperate row
    sampleCountLayout = new HorizontalLayout();
    sampleCountField = new IntegerField("Samples");
    sampleCountLayout.add(sampleCountField);
    organismBox = new MultiSelectComboBox<>("Organism");
    specimenBox = new MultiSelectComboBox<>("Specimen");
    analyteBox = new MultiSelectComboBox<>("Analyte");
    projectManager = new ComboBox<>("Project Manager");
    projectManager.setPlaceholder("Select a project manager");
    principalInvestigator = new ComboBox<>("Principal Investigator");
    principalInvestigator.setPlaceholder("Select a principal investigator");
    createButton = new Button("Create");
    cancelButton = new Button("Cancel");
    configureDialogLayout();
    initForm();
    styleForm();
    handler = new Handler();
    handler.handle();
  }

  private void styleForm() {
    formLayout.setClassName("create-project-form");
    styleSearchBox();
    styleSampleCountField();
    organismBox.addClassName("chip-badge");
    specimenBox.addClassName("chip-badge");
    analyteBox.addClassName("chip-badge");
    organismBox.setMaxWidth(60, Unit.VW);
    specimenBox.setMaxWidth(60, Unit.VW);
    analyteBox.setMaxWidth(60, Unit.VW);
    projectManager.setMaxWidth(60, Unit.VW);
    principalInvestigator.setMaxWidth(60, Unit.VW);
  }

  private void configureDialogLayout() {
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    setHeaderTitle("Create Project");
    add(formLayout);
    getFooter().add(cancelButton, createButton);
    this.setMinWidth(66, Unit.VW);
  }

  private void initForm() {
    formLayout.add(searchField);
    formLayout.add(titleField);
    formLayout.add(projectObjective);
    formLayout.add(experimentalDesignIntroduction);
    formLayout.add(sampleCountLayout);
    formLayout.add(organismBox);
    formLayout.add(specimenBox);
    formLayout.add(analyteBox);
    formLayout.add(projectManager);
    formLayout.add(principalInvestigator);
    // Set FormLayout with one column
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
  }

  private void styleSearchBox() {
    searchField.setPlaceholder("Search");
    searchField.setClassName("searchbox");
    searchField.setMaxWidth(50, Unit.VW);
    searchField.setMinWidth(50, Unit.VW);
  }

  private void styleSampleCountField() {
    sampleCountField.setMinWidth(150, Unit.PIXELS);
    sampleCountField.setValue(1);
    sampleCountField.setMin(1);
    sampleCountField.setStep(1);
    sampleCountField.setStepButtonsVisible(true);
    sampleCountField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
  }

  private void initExperimentalDesignIntroduction() {
    Span experimentalDesignHeader = new Span("Experimental Design");
    Span experimentalDesignDescription = new Span(
        "Describe the experimental design by the following fields. Multiple values are allowed");
    experimentalDesignHeader.addClassName("font-bold");
    experimentalDesignIntroduction.setMargin(false);
    experimentalDesignIntroduction.setPadding(false);
    experimentalDesignIntroduction.addClassName("pt-m");
    experimentalDesignIntroduction.add(experimentalDesignHeader);
    experimentalDesignIntroduction.add(experimentalDesignDescription);
  }

  public void setOffer(Offer offer) {
    handler.loadOfferContent(offer);
  }

  public String getTitle() {
    return titleField.getValue();
  }

  public String getObjective() {
    return projectObjective.getValue();
  }

  //ToDo Replace with values from Comboboxes
  public String getExperimentalDesign() {
    return experimentalDesignField.getValue();
  }

  /**
   * Resets the values and validity of all components that implement value storing and validity
   * interfaces
   */
  public void reset() {
    formLayout.getChildren().filter(component -> component instanceof HasValue<?, ?>)
        .forEach(component -> ((HasValue<?, ?>) component).clear());
    formLayout.getChildren().filter(component -> component instanceof HasValidation)
        .forEach(component -> ((HasValidation) component).setInvalid(false));
  }

  public void resetAndClose() {
    reset();
    close();
  }

  private class Handler {

    private void handle() {
      restrictInputLength();
      resetDialogueUponClosure();
      closeDialogueViaCancelButton();
    }

    public void loadOfferContent(Offer offer) {
      titleField.setValue(offer.projectTitle().title());
      projectObjective.setValue(offer.projectObjective().objective());
      experimentalDesignField.setValue(offer.experimentalDesignDescription().description());

    }

    private void restrictInputLength() {
      titleField.setMaxLength((int) ProjectTitle.maxLength());
      projectObjective.setMaxLength((int) ProjectObjective.maxLength());
      experimentalDesignField.setMaxLength(
          (int) ExperimentalDesignDescription.maxLength());

      titleField.setValueChangeMode(ValueChangeMode.EAGER);
      projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
      experimentalDesignField.setValueChangeMode(ValueChangeMode.EAGER);

      addConsumedLengthHelper(titleField, titleField.getValue());
      addConsumedLengthHelper(projectObjective, projectObjective.getValue());
      addConsumedLengthHelper(experimentalDesignField, experimentalDesignField.getValue());

      titleField.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      projectObjective.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      experimentalDesignField.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
    }

    private void addConsumedLengthHelper(TextArea textArea, String newValue) {
      int maxLength = textArea.getMaxLength();
      int consumedLength = newValue.length();
      textArea.setHelperText(consumedLength + "/" + maxLength);
    }

    private void addConsumedLengthHelper(TextField textField, String newValue) {
      int maxLength = textField.getMaxLength();
      int consumedLength = newValue.length();
      textField.setHelperText(consumedLength + "/" + maxLength);
    }

    private void closeDialogueViaCancelButton() {
      cancelButton.addClickListener(buttonClickEvent -> resetAndClose());
    }

    private void resetDialogueUponClosure() {
      // Calls the reset method for all possible closure methods of the dialogue window:
      addDialogCloseActionListener(closeActionEvent -> resetAndClose());
    }
  }
}
