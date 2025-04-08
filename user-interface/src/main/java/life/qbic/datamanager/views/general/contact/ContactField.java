package life.qbic.datamanager.views.general.contact;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.HasClientValidation;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.Serial;
import java.util.Arrays;
import java.util.Objects;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.contact.OrcidEntry;
import life.qbic.projectmanagement.application.contact.PersonLookupService;


public class ContactField extends CustomField<Contact> implements HasClientValidation {

  public static final String GAP_M_CSS = "gap-m";
  public static final String GAP_02_CSS = "gap-02";
  private static final String FULL_WIDTH_CSS = "full-width";
  private static final String FLEX_HORIZONTAL = "flex-horizontal";
  private static final String PADDING_BOTTOM = "padding-top-bottom-04";
  private static final Logger log = logger(ContactField.class);

  private final Checkbox setMyselfCheckBox;
  private final ManualContactSetter manualContactSetter;
  protected transient ComboBox<OrcidEntry> orcidSelection;
  private Contact myself;
  private boolean isOptional = true;

  private ContactField(String label, PersonLookupService personLookupService) {
    this.orcidSelection = createSelection(personLookupService);
    orcidSelection.addClassName(FULL_WIDTH_CSS);
    this.setMyselfCheckBox = new Checkbox();
    this.manualContactSetter = new ManualContactSetter();
    setLabel(label);
    add(layoutFields(setMyselfCheckBox, orcidSelection, manualContactSetter));
    hideCheckbox(); // default is to hide the set myself checkbox
    addValueChangeListeners();
  }

  private static void resetFieldValues(ContactField field) {
    var emptyContact = new Contact("", "", "", "");
    field.setPresentationValue(emptyContact);
  }

  public static ContactField createSimple(String label, PersonLookupService personLookupService) {
    return new ContactField(label, personLookupService);
  }

  private static TextField withPlaceHolder(TextField textField, String placeHolder) {
    textField.setPlaceholder(placeHolder);
    return textField;
  }

  private static TextField withErrorMessage(TextField textField, String errorMessage) {
    textField.setErrorMessage(errorMessage);
    return textField;
  }

  private static Div layoutFields(Checkbox box, ComboBox<OrcidEntry> contactBox,
      ManualContactSetter manualContactSetter) {
    var layout = new Div();
    layout.addClassNames("flex-vertical", GAP_M_CSS);
    layout.add(box);
    layout.add(contactBox);
    layout.add(manualContactSetter);
    return layout;
  }

  private void addValueChangeListeners() {

    setMyselfCheckBox.addValueChangeListener(listener -> {
      //isFromClient is necessary since the checkbox will be unchecked if another selection mode is chosen
      if (!listener.isFromClient()) {
        return;
      }
      //Ensures that all other field values are set to empty before the value provided by the listener is set
      resetFieldValues(this);
      setMyselfCheckBox.setValue(listener.getValue());
      updateValue();
    });
    orcidSelection.addValueChangeListener(listener -> {
      if (!listener.isFromClient()) {
        return;
      }
      //Ensures that all other field values are set to empty before the value provided by the listener is set
      resetFieldValues(this);
      orcidSelection.setValue(listener.getValue());
      updateValue();
    });
    manualContactSetter.addExpandSpanClickListener(listener -> {
      if (!listener.isFromClient()) {
        return;
      }
      //Ensures that all other field values are set to empty before the value provided by the listener is set
      resetFieldValues(this);
      updateValue();
      manualContactSetter.openFieldLayout();
    });
  }

  private ComboBox<OrcidEntry> createSelection(PersonLookupService personLookupService) {
    ComboBox<OrcidEntry> personSelection = new ComboBox<>("Search the Orcid Repository");
    //We want to avoid NullPointers as model or presentation values
    personSelection.setPlaceholder("Search");
    personSelection.setHelperText("Please provide at least 2 letters to search for entries");
    personSelection.setPrefixComponent(VaadinIcon.SEARCH.create());
    personSelection.setRenderer(new ComponentRenderer<>(
        orcidEntry -> new ContactInfoComponent(orcidEntry.fullName(), orcidEntry.emailAddress(),
            orcidEntry.oidc(), orcidEntry.oidcIssuer())));
    personSelection.setItemLabelGenerator(OrcidEntry::fullName);
    personSelection.setItems(
        query -> personLookupService.queryPersons(query.getFilter().orElse(""), query.getOffset(),
                query.getLimit())
            .stream()
            .map(orcidEntry -> new OrcidEntry(orcidEntry.fullName(), orcidEntry.emailAddress(),
                orcidEntry.oidc(),
                orcidEntry.oidcIssuer()
            )));
    return personSelection;
  }

  public void setOptional(boolean optional) {
    isOptional = optional;
  }

  public void setMyself(Contact myself, String hint) {
    this.myself = Objects.requireNonNull(myself);
    this.setMyselfCheckBox.setLabel(hint);
    showCheckbox();
  }

  private void showCheckbox() {
    setMyselfCheckBox.setVisible(true);
  }

  private void hideCheckbox() {
    setMyselfCheckBox.setVisible(false);
  }

  @Override
  protected Contact generateModelValue() {
    //If the checkbox was checked return the currently loggedin user as a contact
    if (Boolean.TRUE.equals(setMyselfCheckBox.getValue())) {
      return myself;
    }
    //If an orcidEntry was selected return the values provided from the orcid Repository
    if (orcidSelection.getValue() != orcidSelection.getEmptyValue()) {
      var selectedEntry = orcidSelection.getValue();
      return new Contact(selectedEntry.fullName(), selectedEntry.emailAddress(),
          selectedEntry.oidc(), selectedEntry.oidcIssuer());
    }
    // Else return the values provided within the manual fields.
    return new Contact(fullName().getValue(), email().getValue(), "", "");
  }

  public TextField fullName() {
    return manualContactSetter.fullNameField();
  }

  public TextField email() {
    return manualContactSetter.emailField();
  }

  @Override
  protected void setPresentationValue(Contact contact) {
    //If no contact was provided all fields should be set to empty values.
    if (contact == null || contact.isEmpty()) {
      orcidSelection.setValue(orcidSelection.getEmptyValue());
      setMyselfCheckBox.setValue(setMyselfCheckBox.getEmptyValue());
      manualContactSetter.closeFieldLayout();
      manualContactSetter.resetLayoutFields();
      return;
    }
    //If the user selected herself, only the checkbox should be highlighted
    if (contact.equals(myself)) {
      setMyselfCheckBox.setValue(true);
      return;
    }
    //If the user provided an entry from the orcid repository show the full name within the combobox
    if (contact.isComplete()) {
      orcidSelection.setValue(new OrcidEntry(contact.fullName(), contact.email(), contact.oidc(),
          contact.oidcIssuer()));
      return;
    }
    // Only open the Field Layout if the user was not provided via the checkbox or the orcid.
    manualContactSetter.openFieldLayout();
    manualContactSetter.setValues(contact.fullName(), contact.email());
  }

  @Override
  public void setInvalid(boolean value) {
    if (value) {
      invalidate();
    } else {
      removeErrors();
    }
  }

  private void removeErrors() {
    email().setInvalid(false);
    fullName().setInvalid(false);
    orcidSelection.setInvalid(false);
  }

  private void invalidate() {
    if (email().getValue().isEmpty() && fullName().getValue().isEmpty() && isOptional) {
      return;
    }
    if (email().getValue().isBlank()) {
      email().setInvalid(true);
    }
    if (fullName().getValue().isBlank()) {
      fullName().setInvalid(true);
    }
  }

  /**
   * A component displaying a users orcid, full name and email
   */
  public static class ContactInfoComponent extends Div {

    public ContactInfoComponent(String fullName, String email, String oidc, String oidcIssuer) {
      addClassNames("flex-vertical", "flex-align-items-baseline", GAP_02_CSS);
      setFullNameAndEmail(fullName, email);
      setOidc(oidc, oidcIssuer);
    }

    private void setFullNameAndEmail(String fullName, String email) {
      Span fullNameSpan = new Span(fullName);
      fullNameSpan.addClassName("bold");
      Span emailSpan = new Span(email);
      Span userNameAndFullName = new Span(fullNameSpan, emailSpan);
      userNameAndFullName.addClassNames(GAP_02_CSS, FLEX_HORIZONTAL);
      add(userNameAndFullName);
    }

    protected void setOidc(String oidc, String oidcIssuer) {
      if (oidcIssuer.isEmpty() || oidc.isEmpty()) {
        return;
      }
      Arrays.stream(OidcType.values())
          .filter(ot -> ot.getIssuer().equals(oidcIssuer))
          .findFirst()
          .ifPresentOrElse(oidcType -> addOidcInfoItem(oidcType, oidc),
              () -> log.warn("Unknown oidc Issuer %s".formatted(oidcIssuer)));
    }

    private void addOidcInfoItem(OidcType oidcType, String oidc) {
      String oidcUrl = String.format(oidcType.getUrl()) + oidc;
      Anchor oidcLink = new Anchor(oidcUrl, oidc);
      oidcLink.setTarget(AnchorTarget.BLANK);
      OidcLogo oidcLogo = new OidcLogo(oidcType);
      Span oidcSpan = new Span(oidcLogo, oidcLink);
      oidcSpan.addClassNames(GAP_02_CSS, "flex-align-items-center", FLEX_HORIZONTAL);
      add(oidcSpan);
    }
  }

  /**
   * A hideable component enabling the user to provide full name and email
   */
  public class ManualContactSetter extends Div {

    private final TextField fullNameField;
    private final TextField emailField;
    private final Span fieldLayout;

    public ManualContactSetter() {
      this.fullNameField = withErrorMessage(
          withPlaceHolder(new TextField(), "Please provide a name"),
          "Please provide the full name of the contact");
      this.emailField = withErrorMessage(
          withPlaceHolder(new TextField(), "Please enter an email address"),
          "Please provide a valid email address, e.g. my.name@example.com");
      fieldLayout = new Span();
      fieldLayout.add(fullNameField, emailField);
      styleFieldLayout();
      add(createManualSelectionSpan(), fieldLayout);
      addValueChangeListeners();
    }

    private void styleFieldLayout() {
      fieldLayout.addClassNames(FLEX_HORIZONTAL, GAP_M_CSS, FULL_WIDTH_CSS, PADDING_BOTTOM);
      fullNameField.addClassName(FULL_WIDTH_CSS);
      emailField.addClassName(FULL_WIDTH_CSS);
      closeFieldLayout();
    }

    private void addValueChangeListeners() {
      fullNameField.setValueChangeMode(ValueChangeMode.ON_BLUR);
      emailField.setValueChangeMode(ValueChangeMode.ON_BLUR);
      fullNameField.addValueChangeListener(
          event -> updateValue());
      emailField.addValueChangeListener(event -> updateValue());
    }

    private Span createManualSelectionSpan() {
      Span manualSelectionSpan = new Span();
      Span textSpan = new Span("Not on the list?");
      Span clickableSpan = new Span("Enter the details manually");
      clickableSpan.addClassName("link");
      manualSelectionSpan.add(textSpan, clickableSpan);
      manualSelectionSpan.addClassNames(FLEX_HORIZONTAL, GAP_M_CSS, FULL_WIDTH_CSS);
      clickableSpan.addClickListener(spanClickEvent -> {
        //Only Fire an Event if the layout has not been opened yet
        if (fieldLayout.isVisible()) {
          return;
        }
        fireEvent(new SetContactManuallyEvent(this, true));
      });
      return manualSelectionSpan;
    }

    private void openFieldLayout() {
      if (fieldLayout.isVisible()) {
        return;
      }
      fieldLayout.setVisible(true);
    }

    private void closeFieldLayout() {
      if (!fieldLayout.isVisible()) {
        return;
      }
      fieldLayout.setVisible(false);
      resetLayoutFields();
    }

    private void resetLayoutFields() {
      fullNameField.setValue(fullNameField.getEmptyValue());
      emailField.setValue(emailField.getEmptyValue());
      fullNameField.setInvalid(false);
      emailField.setInvalid(false);
    }

    private void setValues(String fullName, String email) {
      fullNameField.setValue(fullName);
      emailField.setValue(email);
    }

    private void addExpandSpanClickListener(
        ComponentEventListener<SetContactManuallyEvent> listener) {
      addListener(SetContactManuallyEvent.class, listener);
    }

    public TextField fullNameField() {
      return fullNameField;
    }

    public TextField emailField() {
      return emailField;
    }

    /**
     * <b>Set Manual Contact Event</b>
     *
     * <p>Indicates that the user intends to set the contact manually</p>
     */
    public static class SetContactManuallyEvent extends ComponentEvent<ManualContactSetter> {

      @Serial
      private static final long serialVersionUID = 5053589646150265555L;

      public SetContactManuallyEvent(ManualContactSetter source, boolean fromClient) {
        super(source, fromClient);
      }
    }

  }
}
