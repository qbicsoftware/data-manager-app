package life.qbic.datamanager.views.projects.create;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.HasValidation;
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
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import life.qbic.finances.api.FinanceService;
import life.qbic.finances.api.Offer;
import life.qbic.finances.api.OfferSummary;
import life.qbic.logging.api.Logger;
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
public class ProjectDesignLayout extends Div implements HasValidation {

  private static final Logger log = logger(ProjectDesignLayout.class);
  private static final String TITLE = "Project Design";
  final ComboBox<OfferSummary> offerSearchField = new ComboBox<>("Offer");
  private final TextField codeField = new TextField("Code");
  private final TextField titleField = new TextField("Title");
  private final TextArea projectDescription = new TextArea("Description");
  private final Button generateCodeButton = new Button(new Icon(VaadinIcon.REFRESH));
  private final Binder<ProjectDesign> projectDesignBinder = new Binder<>(ProjectDesign.class);
  private final FinanceService financeService;

  public ProjectDesignLayout(FinanceService financeService) {
    this.financeService = financeService;
    initLayout();
    initFieldValidators();
    bindOfferDataProvider(financeService);
  }

  private void initLayout() {
    Span projectDesignTitle = new Span(TITLE);
    projectDesignTitle.addClassName("title");
    Span projectDesignDescription = new Span("Description text");
    offerSearchField.setClassName("search-field");
    offerSearchField.setPlaceholder("Search for offers");
    offerSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());
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
    add(projectDesignTitle, projectDesignDescription, offerSearchField, codeTitleAndButtonSpan,
        projectDescription);
    addClassName("project-design-layout");
  }

  private void initCodeGenerationButton() {
    generateCodeButton.setTooltipText("Generate Project Code");
    generateCodeButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    generateCodeButton.addClickListener(
        buttonClickEvent -> codeField.setValue(ProjectCode.random().value()));
  }

  private void initFieldValidators() {
    codeField.setRequired(true);
    titleField.setRequired(true);
    projectDescription.setRequired(true);
    projectDesignBinder.forField(codeField).withValidator(ProjectCode::isValid,
            "A project code starts with Q followed by 4 letters/numbers").
        bind(ProjectDesign::getProjectCode, ProjectDesign::setProjectCode);
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

  private void fillProjectInformationFromOffer(Offer offer) {
    titleField.setValue(offer.title());
    projectDescription.setValue(offer.objective().replace("\n", " "));
  }


  public ProjectDesign getProjectDesign() {
    ProjectDesign projectDesign = new ProjectDesign();
    projectDesignBinder.writeBeanIfValid(projectDesign);
    return projectDesign;
  }

  /**
   * Sets an error message to the component.
   * <p>
   * The Web Component is responsible for deciding when to show the error message to the user, and
   * this is usually triggered by triggering the invalid state for the Web Component. Which means
   * that there is no need to clean up the message when component becomes valid (otherwise it may
   * lead to undesired visual effects).
   *
   * @param errorMessage a new error message
   */
  @Override
  public void setErrorMessage(String errorMessage) {
    /* Unused since we are only interested in the final values stored in the component*/
  }

  /**
   * Gets current error message from the component.
   *
   * @return current error message
   */
  @Override
  public String getErrorMessage() {
    return "Invalid Input found in Project Design";
  }

  /**
   * Sets the validity of the component input.
   * <p>
   * When component becomes valid it hides the error message by itself, so there is no need to clean
   * up the error message via the {@link #setErrorMessage(String)} call.
   *
   * @param invalid new value for component input validity
   */
  @Override
  public void setInvalid(boolean invalid) {
    /* Unused since we are only interested in the final values stored in the component*/
  }

  /**
   * Returns {@code true} if component input is invalid, {@code false} otherwise.
   *
   * @return whether the component input is valid
   */
  @Override
  public boolean isInvalid() {
    projectDesignBinder.validate();
    return !projectDesignBinder.isValid();
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

    public void setOfferId(String offerId) {
      this.offerId = offerId;
    }

    public void setProjectTitle(String projectTitle) {
      this.projectTitle = projectTitle;
    }

    public void setProjectObjective(String projectObjective) {
      this.projectObjective = projectObjective;
    }

    public void setProjectCode(String projectCode) {
      this.projectCode = projectCode;
    }

    public String getOfferId() {
      return offerId;
    }

    public String getProjectCode() {
      return projectCode;
    }

    public String getProjectTitle() {
      return projectTitle;
    }

    public String getProjectObjective() {
      return projectObjective;
    }
  }
}
