package life.qbic.datamanager.views.projects.create;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.general.HasBinderValidation;
import life.qbic.datamanager.views.projects.create.ProjectDesignLayout.ProjectDesign;
import life.qbic.finances.api.FinanceService;
import life.qbic.finances.api.Offer;
import life.qbic.finances.api.OfferSummary;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectObjective;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;
import org.springframework.stereotype.Component;

/**
 * <b>Project Design Layout</b>
 *
 * <p>Layout which enables the user to input the information associated with the project design
 * during project creation and validates the provided information</p>
 */
@Component
public class ProjectDesignLayout extends Div implements HasBinderValidation<ProjectDesign> {

  private static final Logger log = logger(ProjectDesignLayout.class);
  private static final String TITLE = "Project Design";
  private final ComboBox<OfferSummary> offerSearchField = new ComboBox<>("Offer");
  private final TextField codeField = new TextField("Project ID");
  private final TextField titleField = new TextField("Title");
  private final TextArea projectDescription = new TextArea("Description");
  private final Button generateCodeButton = new Button(new Icon(VaadinIcon.REFRESH));
  private final Binder<ProjectDesign> projectDesignBinder = new Binder<>(ProjectDesign.class);
  private final transient ProjectInformationService projectInformationService;
  private final transient FinanceService financeService;
  private final Span projectDesignDescription = new Span(
      "Specify the name and objective of the research project.");

  public ProjectDesignLayout(ProjectInformationService projectInformationService,
      FinanceService financeService) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService,
        "Project Information service cannot be null");
    this.financeService = Objects.requireNonNull(financeService, "Finance Service cannot be null");
    initLayout();
    initFieldValidators();
    bindOfferDataProvider(financeService);
  }

  private static void addConsumedLengthHelper(TextField textField, String newValue) {
    int maxLength = textField.getMaxLength();
    int consumedLength = newValue.length();
    textField.setHelperText(consumedLength + "/" + maxLength);
  }

  private static void addConsumedLengthHelper(TextArea textArea, String newValue) {
    int maxLength = textArea.getMaxLength();
    int consumedLength = newValue.length();
    textArea.setHelperText(consumedLength + "/" + maxLength);
  }

  /**
   * Render the preview like `#offer-id, #project title`
   *
   * @param offerSummary the offer preview
   * @return the formatted String representation
   * @since 1.0.0
   */
  private static String previewToString(OfferSummary offerSummary) {
    return offerSummary.offerId() + ", " + offerSummary.title();
  }

  private void initLayout() {
    Span projectDesignTitle = new Span(TITLE);
    projectDesignTitle.addClassName("title");

    codeField.setHelperText("Q and 4 letters/numbers");
    codeField.setValue(ProjectCode.random().value());
    codeField.addClassName("code-field");
    initCodeGenerationButton();
    titleField.setPlaceholder("Please enter a title for your project");
    titleField.addClassName("title-field");
    Span codeTitleAndButtonSpan = new Span(codeField, generateCodeButton, titleField);
    codeTitleAndButtonSpan.addClassNames("code-and-title");
    projectDescription.setPlaceholder("Please enter a description for your project");
    projectDescription.addClassName("description-field");

    // disable offer access until user authority is known
    offerSearchField.setEnabled(false);
    offerSearchField.setVisible(false);
    offerSearchField.setClassName("search-field");
    offerSearchField.setPlaceholder("Search for offers");
    offerSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    add(projectDesignTitle, projectDesignDescription, offerSearchField, codeTitleAndButtonSpan,
        projectDescription);
    addClassName("project-design-layout");
  }

  public void enableOfferSearch() {
    offerSearchField.setEnabled(true);
    offerSearchField.setVisible(true);
    projectDesignDescription.add(
        " You can either select a project from the offer list or create a new one.");
  }

  private void initCodeGenerationButton() {
    generateCodeButton.setTooltipText("Generate Project ID");
    generateCodeButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    generateCodeButton.addClickListener(event -> {
      var projectCode = generateUniqueProjectCode();
      codeField.setValue(projectCode);
    });
  }


  private String generateUniqueProjectCode() {
    String newProjectCode = ProjectCode.random().value();
    boolean isProjectCodeDuplicated = !isProjectCodeUnique(newProjectCode);
    int retries = 0;
    final int MAX_NUMBER_OF_RETRIES = 20;
    while (isProjectCodeDuplicated) {
      //Ensure that we have an exit condition and the while loop does not iterate endlessly if most project codes are taken
      if (retries > MAX_NUMBER_OF_RETRIES) {
        break;
      }
      newProjectCode = ProjectCode.random().value();
      isProjectCodeDuplicated = isProjectCodeUnique(newProjectCode);
      retries++;
    }
    return newProjectCode;
  }

  private void initFieldValidators() {
    codeField.setRequired(true);
    titleField.setRequired(true);
    projectDescription.setRequired(true);
    projectDesignBinder.forField(codeField).withValidator(ProjectCode::isValid,
            "A project code starts with Q followed by 4 letters/numbers")
        .withValidator((this::uniqueProjectCodeValidator))
        .bind(ProjectDesign::getProjectCode, ProjectDesign::setProjectCode);
    projectDesignBinder.forField(titleField)
        .withValidator(it -> !it.isBlank(), "Please provide a project title")
        .bind((ProjectDesign::getProjectTitle),
            ProjectDesign::setProjectTitle);
    restrictProjectTitleLength();
    projectDesignBinder.forField(projectDescription)
        .withValidator(value -> !value.isBlank(), "Please provide an objective")
        .bind((ProjectDesign::getProjectObjective),
            ProjectDesign::setProjectObjective);
    restrictProjectObjectiveLength();
  }

  private ValidationResult uniqueProjectCodeValidator(String value, ValueContext context) {
    if (isProjectCodeUnique(value)) {
      return ValidationResult.ok();
    } else {
      return ValidationResult.error(
          String.format("Project code %s is already taken. Please choose a different code",
              value));
    }
  }

  private void restrictProjectTitleLength() {
    titleField.setMaxLength((int) ProjectTitle.maxLength());
    titleField.setValueChangeMode(ValueChangeMode.EAGER);
    addConsumedLengthHelper(titleField, titleField.getValue());
    titleField.addValueChangeListener(e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
  }

  private void restrictProjectObjectiveLength() {
    projectDescription.setValueChangeMode(ValueChangeMode.EAGER);
    projectDescription.setMaxLength((int) ProjectObjective.maxLength());
    addConsumedLengthHelper(projectDescription, projectDescription.getValue());
    projectDescription.addValueChangeListener(
        e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
  }

  private void bindOfferDataProvider(FinanceService financeService) {
    offerSearchField.setItems(
        query -> financeService.findOfferContainingProjectTitleOrId(
            query.getFilter().orElse(""), query.getFilter().orElse(""), query.getOffset(),
            query.getLimit()).stream());

    // Render the preview
    offerSearchField.setRenderer(
        new ComponentRenderer<>(preview -> new Text(previewToString(preview))));

    // Generate labels like the rendering
    offerSearchField.setItemLabelGenerator(
        (ItemLabelGenerator<OfferSummary>) OfferSummary::offerId);

    offerSearchField.addValueChangeListener(e -> {
      if (offerSearchField.getValue() != null) {
        setOffer(offerSearchField.getValue().offerId());
      }
    });
  }

  private void setOffer(String offerId) {
    Optional<Offer> offer = financeService.findOfferById(offerId);
    offer.ifPresentOrElse(this::fillProjectInformationFromOffer,
        () -> log.error("No offer found with id: " + offerId));
  }

  private void fillProjectInformationFromOffer(Offer offer) {
    titleField.setValue(offer.title());
    projectDescription.setValue(offer.objective().replace("\n", " "));
  }

  private boolean isProjectCodeUnique(String projectCode) {
    return projectInformationService.isProjectCodeUnique(projectCode);

  }


  /**
   * Returns the project design. Fails for invalid designs with an exception.
   *
   * @return a valid project design
   */
  public ProjectDesign getProjectDesign() {
    ProjectDesign projectDesign = new ProjectDesign();
    try {
      projectDesignBinder.writeBean(projectDesign);
    } catch (ValidationException e) {
      throw new RuntimeException("Tried to access invalid project design.", e);
    }
    return projectDesign;
  }


  @Override
  public Binder<ProjectDesign> getBinder() {
    return projectDesignBinder;
  }

  /**
   * Gets current error message from the component.
   *
   * @return current error message
   */
  @Override
  public String getDefaultErrorMessage() {
    return "Invalid Input found in Project Design";
  }

  public static final class ProjectDesign implements Serializable {

    @Serial
    private static final long serialVersionUID = -6323719140533779774L;
    private String offerId = "";
    @NotEmpty
    private String projectCode = "";
    @NotEmpty
    private String projectTitle = "";
    @NotEmpty
    private String projectObjective = "";

    public String getOfferId() {
      return offerId;
    }

    public void setOfferId(String offerId) {
      this.offerId = offerId;
    }

    public String getProjectCode() {
      return projectCode;
    }

    public void setProjectCode(String projectCode) {
      this.projectCode = projectCode;
    }

    public String getProjectTitle() {
      return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
      this.projectTitle = projectTitle;
    }

    public String getProjectObjective() {
      return projectObjective;
    }

    public void setProjectObjective(String projectObjective) {
      this.projectObjective = projectObjective;
    }
  }
}
