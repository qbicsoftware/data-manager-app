package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.projectmanagement.application.ProjectInformationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project Details Component
 * <p>
 * Shows project details to the user.
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class ProjectDetailsComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -5781313306040217724L;

  private static final String TITLE = "Project Information";

  final TextField titleField = new TextField();
  final FormLayout formLayout = new FormLayout();

  final TextArea experimentalDesignField = new TextArea();
  final TextArea projectObjective = new TextArea();

  private transient final ProjectDetailsHandler handler;

  public ProjectDetailsComponent(@Autowired ProjectInformationService projectInformationService) {
    Objects.requireNonNull(projectInformationService);
    initLayout();
    setComponentStyles();
    this.handler = new ProjectDetailsHandler(this, projectInformationService);
  }

  private static <T extends Component & HasValue<?, ?> & Focusable<?>> Component editableOnFocus(
      T element) {
    VerticalLayout layout = new VerticalLayout();
    layout.add(element);
    element.setReadOnly(true);
    element.addFocusListener(it -> {
      System.out.println("focus event on " + element);
      element.setReadOnly(false);
    });
    element.addBlurListener(it -> {
      System.out.println("blur event on " + element);
      element.setReadOnly(true);
    });
    layout.setPadding(false);
    layout.setSpacing(false);
    layout.setSizeUndefined();
    return layout;
  }


  private void initLayout() {
    formLayout.addFormItem(editableOnFocus(titleField), "Project Title");
    formLayout.addFormItem(editableOnFocus(projectObjective), "Project Objective");
    formLayout.addFormItem(editableOnFocus(experimentalDesignField), "Experimental Design");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    getContent().addFields(formLayout);
    getContent().addTitle(TITLE);
  }

  private void setComponentStyles() {
    titleField.setSizeFull();
    projectObjective.setWidthFull();
    experimentalDesignField.setWidthFull();
    formLayout.setClassName("create-project-form");
  }

  public void projectId(String projectId) {
    handler.projectId(projectId);

  }

}
