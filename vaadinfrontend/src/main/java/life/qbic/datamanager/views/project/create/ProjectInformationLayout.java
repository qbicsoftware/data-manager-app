package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.util.Objects;
import life.qbic.datamanager.views.components.CardLayout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@SpringComponent
public class ProjectInformationLayout extends Composite<CardLayout> {

  final TextField titleField = new TextField();
  final Button saveButton = new Button("Save");
  final Button cancelButton = new Button("Cancel");
  final FormLayout formLayout = new FormLayout();

  final TextArea experimentalDesignField = new TextArea();
  final TextArea projectObjective = new TextArea();

  final Label loadedOfferIdentifier = new Label();
  final ProjectInformationHandler handler;

  public ProjectInformationLayout(@Autowired ProjectInformationHandler handler) {
    Objects.requireNonNull(handler);

    configureCardLayout();
    initForm();
    styleForm();

    this.handler = handler;
    registerToHandler();
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
    getContent().addFields(formLayout, loadedOfferIdentifier);
    getContent().addTitle("Create Project");
  }

  private void initForm() {
    formLayout.addFormItem(titleField, "Project Title");
    formLayout.addFormItem(projectObjective, "Project Objective");
    formLayout.addFormItem(experimentalDesignField, "Experimental Design");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
  }

  private void registerToHandler() {
    handler.handle(this);
  }

}
