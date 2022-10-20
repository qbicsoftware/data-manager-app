package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
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

  private final TextArea experimentalDesignField;
  private final TextArea projectObjective;

  public final ComboBox<PersonReference> projectManager;

  public final ComboBox<PersonReference> principalInvestigator;

  public ProjectInformationDialog() {
    searchField = new ComboBox<>("Offer");

    formLayout = new FormLayout();

    titleField = new TextField("Title");
    titleField.setRequired(true);
    experimentalDesignField = new TextArea("Experimental Design");
    projectObjective = new TextArea("Objective");
    projectObjective.setRequired(true);

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
    titleField.setSizeFull();
    projectObjective.setWidthFull();
    experimentalDesignField.setWidthFull();
    formLayout.setClassName("create-project-form");
  }

  private void configureDialogLayout() {
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    styleSearchBox();

    setHeaderTitle("Create Project");
    add(formLayout);
    getFooter().add(cancelButton, createButton);
  }

  private void initForm() {
    formLayout.add(searchField);
    formLayout.add(titleField);
    formLayout.add(projectObjective);
    formLayout.add(experimentalDesignField);
    formLayout.add(projectManager);
    formLayout.add(principalInvestigator);
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
  }

  private void styleSearchBox() {
    searchField.setPlaceholder("Search");
    searchField.setClassName("searchbox");
    searchField.addClassNames("flex",
        "flex-row",
        "w-full",
        "min-width-300px",
        "max-width-15vw");
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

  public String getExperimentalDesign() {
    return experimentalDesignField.getValue();

  }


  /**
   * Resets all user-defined values set for this dialog
   */
  public void reset(){
    searchField.clear();
    titleField.clear();
    projectObjective.clear();
    experimentalDesignField.clear();
    projectManager.clear();
    principalInvestigator.clear();
  }

  private class Handler {

    private void handle() {
      restrictInputLength();
    }

    public void loadOfferContent(Offer offer) {
      titleField.setValue(offer.projectTitle().title());
      projectObjective.setValue(offer.projectObjective().objective());
      offer.experimentalDesignDescription()
          .ifPresent(it -> experimentalDesignField.setValue(it.description()));
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


  }
}
