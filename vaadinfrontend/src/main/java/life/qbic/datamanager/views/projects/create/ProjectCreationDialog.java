package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;

/**
 * <b>Project Creation Dialog</b>
 *
 * <p>Dialog to create a project based on a project intent</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class ProjectCreationDialog extends Dialog {

  @Serial
  private static final long serialVersionUID = 6132538769263078943L;
  public final ComboBox<OfferPreview> offerSearchField = new ComboBox<>("Offer");
  private HorizontalLayout codeAndTitleLayout = new HorizontalLayout();
  private final TextField codeField = new TextField("Code");
  private final Button generateCodeButton = new Button(new Icon(VaadinIcon.REFRESH));
  private final FormLayout formLayout = new FormLayout();
  private final TextField titleField = new TextField("Title");
  private final TextArea projectObjective = new TextArea("Objective");
  private final DefineExperimentComponent defineExperimentComponent;
  private final VerticalLayout projectContactsLayout = new VerticalLayout();
  public final ComboBox<PersonReference> principalInvestigator = new ComboBox<>(
      "Principal Investigator");
  public final ComboBox<PersonReference> responsiblePerson = new ComboBox<>(
      "Project Responsible (optional)");
  public final ComboBox<PersonReference> projectManager = new ComboBox<>("Project Manager");
  private final Button createButton = new Button("Create");
  private final Button cancelButton = new Button("Cancel");
  private final Handler handler;

  public ProjectCreationDialog(ExperimentalDesignSearchService experimentalDesignSearchService) {

    initCodeAndTitleLayout();
    projectObjective.setRequired(true);

    defineExperimentComponent = new DefineExperimentComponent(experimentalDesignSearchService);
    defineExperimentComponent.hideNameField();

    initProjectContactsLayout();

    principalInvestigator.setPlaceholder("Select a principal investigator");
    principalInvestigator.setRequired(true);

    responsiblePerson.setPlaceholder("Select Project Responsible");
    responsiblePerson.setHelperText("Should be contacted about project related questions");
    //Workaround since combobox does not allow empty selection https://github.com/vaadin/flow-components/issues/1998
    responsiblePerson.setClearButtonVisible(true);

    projectManager.setPlaceholder("Select a project manager");
    projectManager.setRequired(true);

    configureDialogLayout();
    initForm();
    styleForm();
    handler = new Handler();
    handler.handle();
  }

  public void addProjectCreationEventListener(
      ComponentEventListener<ProjectCreationEvent> listener) {
    handler.addProjectCreationEventListener(listener);
  }

  public void addCancelEventListener(
      ComponentEventListener<UserCancelEvent<ProjectCreationDialog>> listener) {
    handler.addUserCancelEventListener(listener);
  }

  public ProjectCreationContent content() {
    return new ProjectCreationContent(offerSearchField.getPattern(),
        codeField.getValue(), titleField.getValue(), projectObjective.getValue(),
        defineExperimentComponent.experimentNameField.getValue(),
        defineExperimentComponent.speciesBox.getValue().stream().toList(),
        defineExperimentComponent.specimenBox.getValue().stream().toList(),
        defineExperimentComponent.analyteBox.getValue().stream().toList(),
        defineExperimentComponent.experimentalDesignDescription.getValue(),
        principalInvestigator.getValue(), responsiblePerson.getValue(), projectManager.getValue());
  }

  private void configureDialogLayout() {
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    setHeaderTitle("Create Project");
    add(formLayout);
    getFooter().add(cancelButton, createButton);
    this.setMinWidth(66, Unit.VW);
    this.setMaxWidth(66, Unit.VW);
  }

  private void initCodeAndTitleLayout() {
    codeAndTitleLayout = new HorizontalLayout();
    generateCodeButton.getElement().setAttribute("aria-label", "Generate Code");
    generateCodeButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    defaultProjectCodeCreation();

    codeField.setMaxWidth(20, Unit.VW);
    codeField.setRequired(true);
    codeField.setHelperText("Q and 4 letters/numbers");
    titleField.setRequired(true);

    codeAndTitleLayout.add(codeField);
    codeAndTitleLayout.add(generateCodeButton);
    codeAndTitleLayout.add(titleField);
    titleField.setWidthFull();
    codeAndTitleLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
    codeAndTitleLayout.setWidthFull();
  }

  private void initForm() {

    formLayout.add(offerSearchField);
    formLayout.add(codeAndTitleLayout);
    formLayout.add(projectObjective);
    formLayout.add(defineExperimentComponent);
    formLayout.add(projectContactsLayout);
    formLayout.add(principalInvestigator);
    formLayout.add(responsiblePerson);
    formLayout.add(projectManager);
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
  }

  private void styleForm() {
    formLayout.setClassName("create-project-form");
    styleSearchBox();
    formLayout.setMaxWidth(60, Unit.VW);
  }

  private void styleSearchBox() {
    offerSearchField.setMaxWidth(30, Unit.VW);
    offerSearchField.setPlaceholder("Search");
    offerSearchField.setClassName("searchbox");
  }

  private void initProjectContactsLayout() {
    Span projectContactsTitle = new Span("Project Contacts");
    Span projectContactsDescription = new Span("Important contact people of the project");
    projectContactsTitle.addClassName("font-bold");
    projectContactsLayout.setMargin(false);
    projectContactsLayout.setPadding(false);
    projectContactsLayout.addClassName("pt-m");
    projectContactsLayout.add(projectContactsTitle);
    projectContactsLayout.add(projectContactsDescription);
  }

  public void setOffer(Offer offer) {
    handler.loadOfferContent(offer);
  }

  public String getCode() {
    return codeField.getValue();
  }

  public String getTitle() {
    return titleField.getValue();
  }

  /**
   * Resets the values and validity of all components that implement value storing and validity
   * interfaces
   */
  public void reset() {
    resetChildValues(codeAndTitleLayout);
    resetChildValues(formLayout);
    resetChildValidation(formLayout);
    resetChildValidation(codeAndTitleLayout);
    defineExperimentComponent.reset();
  }

  private void resetChildValues(Component component) {
    component.getChildren().filter(comp -> comp instanceof HasValue<?, ?>)
        .forEach(comp -> ((HasValue<?, ?>) comp).clear());
  }

  private void defaultProjectCodeCreation() {
    this.addOpenedChangeListener(openedChangeEvent -> {
      if (openedChangeEvent.isOpened()) {
        codeField.setValue(ProjectCode.random().value());
      }
    });
  }

  private void resetChildValidation(Component component) {
    component.getChildren().filter(comp -> comp instanceof HasValidation)
        .forEach(comp -> ((HasValidation) comp).setInvalid(false));
  }

  public void resetAndClose() {
    close();
    reset();
  }

  private class Handler {

    List<Binder<?>> binders = new ArrayList<>();
    private final List<ComponentEventListener<ProjectCreationEvent>> listeners = new ArrayList<>();
    private final List<ComponentEventListener<UserCancelEvent<ProjectCreationDialog>>> cancelListeners = new ArrayList<>();

    Handler() {
    }

    private void handle() {
      restrictInputLength();
      generateProjectCode();
      resetDialogueUponClosure();
      closeDialogueViaCancelButton();
      configureValidators();
      configureFormSubmission();
    }

    private void configureFormSubmission() {
      createButton.addClickListener(event -> {
        validateInput();
        if (isInputValid()) {
          listeners.forEach(listener -> listener.onComponentEvent(
              new ProjectCreationEvent(ProjectCreationDialog.this, true)));
        }
      });
      cancelButton.addClickListener(event -> cancelListeners.forEach(
          listener -> listener.onComponentEvent(
              new UserCancelEvent<>(ProjectCreationDialog.this))));
    }


    private void configureValidators() {
      Binder<Container<String>> binderTitle = new Binder<>();
      binderTitle.forField(titleField)
          .withValidator(value -> !value.isBlank(), "Please provide a title")
          .bind(Container::value, Container::setValue);
      Binder<Container<String>> binderObjective = new Binder<>();
      binderObjective.forField(projectObjective)
          .withValidator(value -> !value.isBlank(), "Please provide an " + "objective")
          .bind(Container::value, Container::setValue);
      Binder<Container<PersonReference>> binderPI = new Binder<>();
      binderPI.forField(principalInvestigator).asRequired("Please select at least one PI")
          .bind(Container::value, Container::setValue);
      Binder<Container<PersonReference>> binderPM = new Binder<>();
      binderPM.forField(projectManager).asRequired("Please select at least one PM")
          .bind(Container::value, Container::setValue);
      binders.addAll(List.of(binderTitle, binderObjective, binderPI, binderPM));
    }

    private void generateProjectCode() {
      generateCodeButton.addClickListener(
          buttonClickEvent -> setCodeFieldValue(ProjectCode.random().value()));
    }

    private void setCodeFieldValue(String code) {
      codeField.setValue(code);
    }

    public void loadOfferContent(Offer offer) {
      titleField.setValue(offer.projectTitle().title());
      projectObjective.setValue(offer.projectObjective().objective());
      defineExperimentComponent.experimentalDesignDescription.setValue(
          offer.experimentalDesignDescription().description());
    }

    private void restrictInputLength() {
      titleField.setMaxLength((int) ProjectTitle.maxLength());
      projectObjective.setMaxLength((int) ProjectObjective.maxLength());
      defineExperimentComponent.experimentalDesignDescription.setMaxLength(
          (int) ExperimentalDesignDescription.maxLength());

      titleField.setValueChangeMode(ValueChangeMode.EAGER);
      projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
      defineExperimentComponent.experimentalDesignDescription.setValueChangeMode(
          ValueChangeMode.EAGER);

      addConsumedLengthHelper(titleField, titleField.getValue());
      addConsumedLengthHelper(projectObjective, projectObjective.getValue());
      addConsumedLengthHelper(defineExperimentComponent.experimentalDesignDescription,
          defineExperimentComponent.experimentalDesignDescription.getValue());

      titleField.addValueChangeListener(e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      projectObjective.addValueChangeListener(
          e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
      defineExperimentComponent.experimentalDesignDescription.addValueChangeListener(
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

    protected boolean validateInput() {
      binders.forEach(Binder::validate);
      return binders.stream().allMatch(Binder::isValid) && defineExperimentComponent.isValid();
    }

    public boolean isInputValid() {
      return validateInput();
    }

    public void addProjectCreationEventListener(
        ComponentEventListener<ProjectCreationEvent> listener) {
      this.listeners.add(listener);
    }

    public void addUserCancelEventListener(
        ComponentEventListener<UserCancelEvent<ProjectCreationDialog>> listener) {
      this.cancelListeners.add(listener);
    }
  }

  static class Container<T> {

    private T value;

    T value() {
      return this.value;
    }

    void setValue(T newValue) {
      this.value = newValue;
    }

  }
}
