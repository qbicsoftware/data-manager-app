package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import life.qbic.datamanager.views.components.CardLayout;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@SpringComponent
public class ProjectInformationComponent extends Composite<CardLayout> {

  private final Handler handler = new Handler();

  private final TextField titleField = new TextField();
  final Button saveButton = new Button("Save");
  final Button cancelButton = new Button("Cancel");
  private final FormLayout formLayout = new FormLayout();

  private final TextArea experimentalDesignField = new TextArea();
  private final TextArea projectObjective = new TextArea();

  public ProjectInformationComponent() {
    configureCardLayout();
    initForm();
    styleForm();
    handler.handle();
  }

  private void styleForm() {
    titleField.setSizeFull();
    projectObjective.setWidthFull();
    experimentalDesignField.setWidthFull();
    formLayout.setClassName("create-project-form");
  }

  private void configureCardLayout() {
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    getContent().addButtons(cancelButton, saveButton);
    getContent().addFields(formLayout);
    getContent().addTitle("Create Project");
  }

  private void initForm() {
    formLayout.addFormItem(titleField, "Project Title");
    formLayout.addFormItem(projectObjective, "Project Objective");
    formLayout.addFormItem(experimentalDesignField, "Experimental Design");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
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

  @SpringComponent
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

      titleField.addValueChangeListener(
          e -> addConsumedLengthHelper(e, titleField));
      projectObjective.addValueChangeListener(
          e -> addConsumedLengthHelper(e, projectObjective));
      experimentalDesignField.addValueChangeListener(
          e -> addConsumedLengthHelper(e, experimentalDesignField));
    }

    private void addConsumedLengthHelper(ComponentValueChangeEvent<TextArea, String> e,
        TextArea textArea) {
      int maxLength = textArea.getMaxLength();
      int consumedLength = e.getValue().length();
      e.getSource().setHelperText(consumedLength + "/" + maxLength);
    }

    private void addConsumedLengthHelper(ComponentValueChangeEvent<TextField, String> e,
        TextField textField) {
      int maxLength = textField.getMaxLength();
      int consumedLength = e.getValue().length();
      e.getSource().setHelperText(consumedLength + "/" + maxLength);
    }
  }
}
