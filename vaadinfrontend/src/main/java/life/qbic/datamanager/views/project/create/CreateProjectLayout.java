package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.components.CardLayout;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Create Project")
@Route(value = "projects/create", layout = MainLayout.class)
@PermitAll
@Tag("create-project")
@CssImport("./styles/components/create-project.css")
public class CreateProjectLayout extends Composite<CardLayout> implements HasUrlParameter<String> {

  final TextField titleField = new TextField();
  final Button saveButton = new Button("Save");
  final Button cancelButton = new Button("Cancel");
  final FormLayout formLayout = new FormLayout();

  final TextArea experimentalDesignField = new TextArea();
  final TextArea projectObjective = new TextArea();

  final Label loadedOfferIdentifier = new Label();
  private final CardLayout cardLayout = new CardLayout();
  final CreateProjectHandlerInterface handler;

  public CreateProjectLayout(@Autowired CreateProjectHandlerInterface handler) {
    Objects.requireNonNull(handler);
    this.handler = handler;
    registerToHandler();
  }

  @Override
  protected CardLayout initContent() {
    initFormLayout();
    initCardLayout();
    setComponentStyles();
    return cardLayout;
  }

  private void initFormLayout() {
    formLayout.addFormItem(titleField, "Project Title");
    formLayout.addFormItem(projectObjective, "Project Objective");
    formLayout.addFormItem(experimentalDesignField, "Experimental Design");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
  }

  private void initCardLayout() {
    cardLayout.addButtons(cancelButton, saveButton);
    cardLayout.addFields(formLayout, loadedOfferIdentifier);
    cardLayout.addTitle("Create Project");
  }

  private void setComponentStyles() {
    titleField.setSizeFull();
    projectObjective.setWidthFull();
    experimentalDesignField.setWidthFull();
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    formLayout.setClassName("create-project-form");
  }

  private void registerToHandler() {
    handler.handle(this);
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
    handler.handleEvent(beforeEvent);
  }
}
