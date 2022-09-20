package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Create Project")
@Route(value = "projects/create", layout = MainLayout.class)
@PermitAll
@Tag("create-project")
public class CreateProjectLayout extends Composite<VerticalLayout> {

  final H2 layoutTitle = new H2();
  final TextField titleField = new TextField();
  final Button saveButton = new Button("Save");
  final Button cancelButton = new Button("Cancel");


  public CreateProjectLayout(@Autowired CreateProjectHandlerInterface handler) {
    registerToHandler(handler);
  }

  @Override
  protected VerticalLayout initContent() {
    layoutTitle.setText("Project Information");

    FormLayout formLayout = new FormLayout();
    formLayout.addFormItem(titleField, "Project Title");

    saveButton.setText("Save");
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout formButtons = new HorizontalLayout(cancelButton, saveButton);
    HorizontalLayout headerBar = new HorizontalLayout(layoutTitle, formButtons);
    headerBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
    headerBar.setVerticalComponentAlignment(Alignment.START, layoutTitle);
    headerBar.setVerticalComponentAlignment(Alignment.END, formButtons);
    headerBar.setWidthFull();

    return new VerticalLayout(headerBar, formLayout);
  }

  private void registerToHandler(CreateProjectHandlerInterface handler) {
    handler.handle(this);
  }
}
