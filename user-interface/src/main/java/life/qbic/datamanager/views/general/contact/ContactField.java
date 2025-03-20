package life.qbic.datamanager.views.general.contact;

import static life.qbic.logging.service.LoggerFactory.logger;

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
  private static final Logger log = logger(ContactField.class);
  private final TextField fullName;
  private final TextField email;
  private final Checkbox setMyselfCheckBox;
  protected transient ComboBox<OrcidEntry> orcidSelection;
  private Contact myself;
  private boolean isOptional = true;

  private ContactField(String label, PersonLookupService personLookupService) {
    this.fullName = withErrorMessage(withPlaceHolder(new TextField(), "Please provide a name"),
        "");
    this.email = withErrorMessage(withPlaceHolder(new TextField(), "Please enter an email address"),
        "");
    this.orcidSelection = createSelection(personLookupService);
    orcidSelection.addClassName(FULL_WIDTH_CSS);
    this.setMyselfCheckBox = new Checkbox();
    setLabel(label);
    add(layoutFields(setMyselfCheckBox, layoutFields(fullName, email)), orcidSelection);
    hideCheckbox(); // default is to hide the set myself checkbox
    addValueChangeListeners();
  }

  private void addValueChangeListeners() {
    setMyselfCheckBox.addValueChangeListener(listener -> {
      //isFromClient is necessary since the checkbox will be unchecked if the user changes the information
      if (listener.isFromClient()) {
        if (isChecked(listener.getSource())) {
          loadContact(this, myself);
        } else {
          //If the checkbox is unchecked, all set values should be deleted from all fields
          var emptyContact = new Contact("", "", "", "");
          loadContact(this, emptyContact);
        }
      }
    });
    fullName.addValueChangeListener(listener ->
    {
      //We need to make sure that the orcid information within the orcid selection is reset as soon as manual entry has begun
      if (listener.isFromClient()) {
        orcidSelection.setValue(null);
        //We need to make sure that the checkbox is also unchecked as soon as the user changes the field values
        if (isChecked(setMyselfCheckBox)) {
          setMyselfCheckBox.setValue(false);
        }
      }
      updateValue();
    });
    email.addValueChangeListener(listener ->
    {
      //We need to make sure that the orcid information within the orcid selection is reset as soon as manual entry has begun
      if (listener.isFromClient()) {
        orcidSelection.setValue(null);
        //We need to make sure that the checkbox is also unchecked as soon as the user changes the field values
        if (isChecked(setMyselfCheckBox)) {
          setMyselfCheckBox.setValue(false);
        }
      }
      updateValue();
    });
    orcidSelection.addValueChangeListener(listener -> {
      if (listener.isFromClient()) {
        var orcidEntry = listener.getValue();
        //If the entry in the combobox is deleted we want to remove the set values from all fields accordingly.
        var convertedContact = new Contact("", "", "", "");
        if (orcidEntry != null) {
          convertedContact = new Contact(orcidEntry.fullName(), orcidEntry.emailAddress(),
              orcidEntry.oidc(), orcidEntry.oidcIssuer());
        }
        //We need to make sure that the checkbox is also unchecked as soon as the user changes the field values
        if (isChecked(setMyselfCheckBox)) {
          setMyselfCheckBox.setValue(false);
        }
        loadContact(this, convertedContact);
      }
    });
  }

  private static void loadContact(ContactField field, Contact contact) {
    field.setPresentationValue(contact);
  }

  private static boolean isChecked(Checkbox checkbox) {
    return checkbox.getValue();
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

  private static Div layoutFields(Checkbox box, Div fields) {
    var layout = new Div();
    layout.addClassNames("flex-vertical", GAP_M_CSS);
    layout.add(box);
    layout.add(fields);
    return layout;
  }

  private static Div layoutFields(TextField fullName, TextField email) {
    var layout = new Div(fullName, email);
    layout.addClassNames(FLEX_HORIZONTAL, GAP_M_CSS, FULL_WIDTH_CSS);
    fullName.addClassName(FULL_WIDTH_CSS);
    email.addClassName(FULL_WIDTH_CSS);
    return layout;
  }

  public ContactField createWithMyselfOption(String label, Contact myself, String hint,
      boolean setOptional, PersonLookupService personLookupService) {
    var contactField = createSimple(label, personLookupService);
    contactField.setMyself(myself, hint);
    contactField.setOptional(setOptional);
    return contactField;
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
    //Avoids Null pointer exceptions if the user clicks and closes the selection box
    var oidc = "";
    var oidcIssuer = "";
    if (!orcidSelection.isEmpty()) {
      oidc = orcidSelection.getValue().oidc();
      oidcIssuer = orcidSelection.getValue().oidcIssuer();
    }
    return new Contact(fullName.getValue(), email.getValue(), oidc, oidcIssuer);
  }

  @Override
  protected void setPresentationValue(Contact contact) {
    if (contact != null) {
      fullName.setValue(contact.fullName());
      email.setValue(contact.email());
      if (contact.isComplete()) {
        orcidSelection.setValue(new OrcidEntry(contact.fullName(), contact.email(), contact.oidc(),
            contact.oidcIssuer()));
      }
      // If the checkbox is unchecked, then any selection within the orcid selection should be removed
      else {
        orcidSelection.setValue(null);
      }
    }
  }

  @Override
  public void setInvalid(boolean value) {
    if (value) {
      invalidate();
    } else {
      removeErrors();
    }
  }

  public TextField getEmailTextField() {
    return email;
  }

  public TextField getFullNameTextField() {
    return fullName;
  }

  public ComboBox<OrcidEntry> getOidcSelection() {
    return orcidSelection;
  }

  private void removeErrors() {
    email.setInvalid(false);
    fullName.setInvalid(false);
  }

  private void invalidate() {
    if (email.getValue().isEmpty() && fullName.getValue().isEmpty() && isOptional) {
      return;
    }
    if (email.getValue().isBlank()) {
      email.setInvalid(true);
    }
    if (fullName.getValue().isBlank()) {
      fullName.setInvalid(true);
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

}
