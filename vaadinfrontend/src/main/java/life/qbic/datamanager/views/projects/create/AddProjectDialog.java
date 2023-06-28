package life.qbic.datamanager.views.projects.create;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.application.PersonSearchService;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
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
public class AddProjectDialog extends DialogWindow {
  private static final Logger log = logger(AddProjectDialog.class);
  private Span codeAndTitleLayout = new Span();
  @Serial
  private static final long serialVersionUID = 6132538769263078943L;
  public final ComboBox<OfferPreview> offerSearchField = new ComboBox<>("Offer");
  public final ComboBox<PersonReference> principalInvestigator = new ComboBox<>(
      "Principal Investigator");
  public final ComboBox<PersonReference> responsiblePerson = new ComboBox<>(
      "Project Responsible (optional)");
  public final ComboBox<PersonReference> projectManager = new ComboBox<>("Project Manager");
  private final TextField codeField = new TextField("Code");
  private final Button generateCodeButton = new Button(new Icon(VaadinIcon.REFRESH));
  private final FormLayout formLayout = new FormLayout();
  private final TextField titleField = new TextField("Title");
  private final TextArea projectObjective = new TextArea("Objective");
  private final DefineExperimentComponent defineExperimentComponent;
  private final Div projectContactsLayout = new Div();
  private final Handler handler;

  private final OfferLookupService offerLookupService;

  private final PersonSearchService personSearchService;

  public AddProjectDialog(ExperimentalDesignSearchService experimentalDesignSearchService,
      OfferLookupService offerLookupService, PersonSearchService personSearchService) {
    super();
    this.offerLookupService = Objects.requireNonNull(offerLookupService);
    this.personSearchService = Objects.requireNonNull(personSearchService);

    setConfirmButtonLabel("Add");
    setCancelButtonLabel("Cancel");
    addClassName("create-project-dialog");
    initCodeAndTitleLayout();
    projectObjective.setRequired(true);

    defineExperimentComponent = new DefineExperimentComponent(experimentalDesignSearchService);
    defineExperimentComponent.hideNameField();

    initProjectContactsLayout();

    principalInvestigator.setPlaceholder("Select a principal investigator");
    principalInvestigator.setRequired(true);

    projectManager.setPlaceholder("Select a project manager");
    projectManager.setRequired(true);

    responsiblePerson.setPlaceholder("Select Project Responsible");
    responsiblePerson.setHelperText("Should be contacted about project related questions");
    //Workaround since combobox does not allow empty selection https://github.com/vaadin/flow-components/issues/1998
    responsiblePerson.setClearButtonVisible(true);

    configureDialogLayout();
    initForm();
    styleForm();
    handler = new Handler();

    configureComponent();
  }

  private void configureComponent() {
    configureOfferSearch();
    configurePersonSearch();
  }

  private void configurePersonSearch() {
    setUpPrincipalInvestigatorSearch();
    setUpResponsiblePersonSearch();
    setUpProjectManagerSearch();
  }

  private void setUpPrincipalInvestigatorSearch() {
    setUpPersonSearch(principalInvestigator);
  }

  private void setUpResponsiblePersonSearch() {
    setUpPersonSearch(responsiblePerson);
  }

  private void setUpProjectManagerSearch() {
    setUpPersonSearch(projectManager);
  }

  private void setUpPersonSearch(ComboBox<PersonReference> comboBox) {
    comboBox.setItems(
        query -> personSearchService.find(query.getFilter().orElse(""), query.getOffset(),
            query.getLimit()).stream());
    comboBox.setRenderer(
        new ComponentRenderer<>(personReference -> new Text(personReference.fullName())));
    comboBox.setItemLabelGenerator(
        (ItemLabelGenerator<PersonReference>) PersonReference::fullName);
  }
  private void initCodeAndTitleLayout() {
    codeAndTitleLayout = new Span();
    generateCodeButton.getElement().setAttribute("aria-label", "Generate Code");
    generateCodeButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    defaultProjectCodeCreation();
    codeAndTitleLayout.addClassName("code-and-title");
    codeField.addClassName("code");
    titleField.addClassName("title");

    codeField.setRequired(true);
    codeField.setHelperText("Q and 4 letters/numbers");
    titleField.setRequired(true);

    codeAndTitleLayout.add(codeField);
    codeAndTitleLayout.add(generateCodeButton);
    codeAndTitleLayout.add(titleField);
  }

  private void initProjectContactsLayout() {
    projectContactsLayout.setClassName("project-contacts");

    Span projectContactsTitle = new Span("Project Contacts");
    projectContactsTitle.addClassName("title");

    Span projectContactsDescription = new Span("Important contact people of the project");

    projectContactsLayout.add(projectContactsTitle);
    projectContactsLayout.add(projectContactsDescription);
  }

  private void configureDialogLayout() {
    setHeaderTitle("Add Project");
    add(formLayout);
    getFooter().add(cancelButton, confirmButton);
  }

  private void initForm() {

    formLayout.add(offerSearchField);
    formLayout.add(codeAndTitleLayout);
    formLayout.add(projectObjective);
    formLayout.add(defineExperimentComponent);
    formLayout.add(projectContactsLayout);
    formLayout.add(principalInvestigator);
    formLayout.add(projectManager);
    formLayout.add(responsiblePerson);
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
  }

  private void styleForm() {
    formLayout.setClassName("form-content");
    styleSearchBox();
  }

  private void defaultProjectCodeCreation() {
    this.addOpenedChangeListener(openedChangeEvent -> {
      if (openedChangeEvent.isOpened()) {
        codeField.setValue(ProjectCode.random().value());
      }
    });
  }

  private void styleSearchBox() {
    offerSearchField.setClassName("search-field");
    offerSearchField.setPlaceholder("Search");
    offerSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());
  }

  public void addCancelEventListener(
      ComponentEventListener<UserCancelEvent<AddProjectDialog>> listener) {
    handler.addUserCancelEventListener(listener);
  }

  public ProjectDraft content() {
    return new ProjectDraft(offerSearchField.getPattern(),
        codeField.getValue(), titleField.getValue(), projectObjective.getValue(),
        defineExperimentComponent.experimentNameField.getValue(),
        defineExperimentComponent.speciesBox.getValue().stream().toList(),
        defineExperimentComponent.specimenBox.getValue().stream().toList(),
        defineExperimentComponent.analyteBox.getValue().stream().toList(),
        defineExperimentComponent.experimentalDesignDescription.getValue(),
        principalInvestigator.getValue(), responsiblePerson.getValue(), projectManager.getValue());
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

  public void resetAndClose() {
    close();
    reset();
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

  private void resetChildValidation(Component component) {
    component.getChildren().filter(comp -> comp instanceof HasValidation)
        .forEach(comp -> ((HasValidation) comp).setInvalid(false));
  }

  private void configureOfferSearch() {
    offerSearchField.setItems(
        query -> offerLookupService.findOfferContainingProjectTitleOrId(
            query.getFilter().orElse(""), query.getFilter().orElse(""), query.getOffset(),
            query.getLimit()).stream());

    // Render the preview
    offerSearchField.setRenderer(
        new ComponentRenderer<>(preview -> new Text(previewToString(preview))));

    // Generate labels like the rendering
    offerSearchField.setItemLabelGenerator(
        (ItemLabelGenerator<OfferPreview>) it -> it.offerId().id());

    offerSearchField.addValueChangeListener(e -> {
      if (offerSearchField.getValue() != null) {
        preloadContentFromOffer(offerSearchField.getValue().offerId().id());
      }
    });
  }

  /**
   * Render the preview like `#offer-id, #project title`
   *
   * @param offerPreview the offer preview
   * @return the formatted String representation
   * @since 1.0.0
   */
  private static String previewToString(OfferPreview offerPreview) {
    return offerPreview.offerId().id() + ", " + offerPreview.getProjectTitle().title();
  }

  private void preloadContentFromOffer(String offerId) {
    OfferId id = OfferId.from(offerId);
    Optional<Offer> offer = offerLookupService.findOfferById(id);
    offer.ifPresentOrElse(this::setOffer,
        () -> log.error("No offer found with id: " + offerId));
  }

  public void addProjectAddEventListener(ComponentEventListener<ProjectAddEvent> listener) {
    handler.addProjectCreationEventListener(listener);
  }

  private class Handler {

    private final List<ComponentEventListener<ProjectAddEvent>> listeners = new ArrayList<>();
    private final List<ComponentEventListener<UserCancelEvent<AddProjectDialog>>> cancelListeners = new ArrayList<>();
    List<Binder<?>> binders = new ArrayList<>();

    Handler() {
      restrictInputLength();
      generateProjectCode();
      resetDialogueUponClosure();
      closeDialogueViaCancelButton();
      configureValidators();
      configureFormSubmission();
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

    private void generateProjectCode() {
      generateCodeButton.addClickListener(
          buttonClickEvent -> setCodeFieldValue(ProjectCode.random().value()));
    }

    private void resetDialogueUponClosure() {
      // Calls the reset method for all possible closure methods of the dialogue window:
      addDialogCloseActionListener(closeActionEvent -> resetAndClose());
    }

    private void closeDialogueViaCancelButton() {
      cancelButton.addClickListener(buttonClickEvent -> resetAndClose());
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

    private void configureFormSubmission() {
      confirmButton.addClickListener(event -> {
        validateInput();
        if (isInputValid()) {
          listeners.forEach(listener -> listener.onComponentEvent(
              new ProjectAddEvent(AddProjectDialog.this, true)));
        }
      });
      cancelButton.addClickListener(event -> cancelListeners.forEach(
          listener -> listener.onComponentEvent(
              new UserCancelEvent<>(AddProjectDialog.this))));
    }

    private void addConsumedLengthHelper(TextField textField, String newValue) {
      int maxLength = textField.getMaxLength();
      int consumedLength = newValue.length();
      textField.setHelperText(consumedLength + "/" + maxLength);
    }

    private void addConsumedLengthHelper(TextArea textArea, String newValue) {
      int maxLength = textArea.getMaxLength();
      int consumedLength = newValue.length();
      textArea.setHelperText(consumedLength + "/" + maxLength);
    }

    private void setCodeFieldValue(String code) {
      codeField.setValue(code);
    }

    protected boolean validateInput() {
      binders.forEach(Binder::validate);
      return binders.stream().allMatch(Binder::isValid) && defineExperimentComponent.isValid();
    }

    public boolean isInputValid() {
      return validateInput();
    }

    public void loadOfferContent(Offer offer) {
      titleField.setValue(offer.projectTitle().title());
      projectObjective.setValue(offer.projectObjective().objective());
      defineExperimentComponent.experimentalDesignDescription.setValue(
          offer.experimentalDesignDescription().description());
    }

    public void addProjectCreationEventListener(
        ComponentEventListener<ProjectAddEvent> listener) {
      this.listeners.add(listener);
    }

    public void addUserCancelEventListener(
        ComponentEventListener<UserCancelEvent<AddProjectDialog>> listener) {
      this.cancelListeners.add(listener);
    }
  }

}
