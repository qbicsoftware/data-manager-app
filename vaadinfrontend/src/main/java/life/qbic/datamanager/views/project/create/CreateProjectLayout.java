package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

@Tag("create-project")
public class CreateProjectLayout extends Composite<VerticalLayout> {

  final Label layoutTitle = new Label();
  final TextField titleField = new TextField();
  final Button saveButton = new Button();
  final Button cancelButton = new Button();


  @Override
  protected VerticalLayout initContent() {
    layoutTitle.setText("Project Information");
    FormLayout formLayout = new FormLayout();
    formLayout.addFormItem(titleField, "Project Title");
    VerticalLayout compositeRoot = new VerticalLayout();
    HorizontalLayout formButtons = new HorizontalLayout(cancelButton, saveButton);
    HorizontalLayout headerBar = new HorizontalLayout(layoutTitle, formButtons);
    headerBar.setVerticalComponentAlignment(Alignment.END, formButtons);
    compositeRoot.add(headerBar, formLayout);
    return compositeRoot;
  }
}
