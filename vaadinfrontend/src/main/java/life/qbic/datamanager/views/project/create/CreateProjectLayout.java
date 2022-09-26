package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
public class CreateProjectLayout extends Composite<CardLayout> implements HasUrlParameter<String> {

  final Label layoutTitle = new Label();
  final TextField titleField = new TextField();
  final Button saveButton = new Button("Save");
  final Button cancelButton = new Button("Cancel");

  final TextArea projectObjective = new TextArea();

  final CreateProjectHandlerInterface handler;


  public CreateProjectLayout(@Autowired CreateProjectHandlerInterface handler) {
    Objects.requireNonNull(handler);
    registerToHandler(handler);
    this.handler = handler;
  }

  @Override
  protected CardLayout initContent() {
    layoutTitle.setText("Project Information");

    FormLayout formLayout = new FormLayout();
    formLayout.addFormItem(titleField, "Project Title");
    formLayout.addFormItem(projectObjective, "Project Objective");
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    titleField.setSizeFull();
    projectObjective.setWidthFull();

    saveButton.setText("Save");
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout formButtons = new HorizontalLayout(cancelButton, saveButton);
    HorizontalLayout headerBar = new HorizontalLayout(layoutTitle, formButtons);
    headerBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
    headerBar.setVerticalComponentAlignment(Alignment.START, layoutTitle);
    headerBar.setVerticalComponentAlignment(Alignment.END, formButtons);
    headerBar.setWidthFull();
    CardLayout cardLayout = new CardLayout();
    cardLayout.addButtons(cancelButton, saveButton);
    cardLayout.addFields(formLayout);
    cardLayout.setTitleText("Project Information");
    cardLayout.setAlignItems(Alignment.START);
    return cardLayout;
  }

  private void registerToHandler(CreateProjectHandlerInterface handler) {
    handler.handle(this);
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
    handler.handleEvent(beforeEvent);
  }
}
