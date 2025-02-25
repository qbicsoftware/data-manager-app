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
import life.qbic.projectmanagement.application.contact.PersonLookupService;


public class ContactField extends CustomField<Contact> implements HasClientValidation {

  public static final String GAP_M_CSS = "gap-m";
  private static final String FULL_WIDTH_CSS = "full-width";
  private final TextField fullName;
  private final TextField email;
  private final Checkbox setMyselfCheckBox;
  protected transient ComboBox<Contact> orcidSelection;
  private Contact myself;
  private boolean isOptional = true;
  private static final Logger log = logger(ContactField.class);

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
    setMyselfCheckBox.addValueChangeListener(listener -> {
      if (isChecked(listener.getSource())) {
        loadContact(this, myself);
      }
    });
    fullName.addValueChangeListener(listener -> updateValue());
    email.addValueChangeListener(listener -> updateValue());
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
    layout.addClassNames("flex-horizontal", GAP_M_CSS, FULL_WIDTH_CSS);
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

  private ComboBox<Contact> createSelection(PersonLookupService personLookupService) {
    ComboBox<Contact> personSelection = new ComboBox<>("Search the Orcid Repository");
    //We want to avoid NullPointers as model or presentation values
    personSelection.setPlaceholder("Search");
    personSelection.setHelperText("Please provide at least 2 letters to search for entries");
    personSelection.setPrefixComponent(VaadinIcon.SEARCH.create());
    personSelection.setRenderer(new ComponentRenderer<>(
        contact -> new ContactInfoComponent(contact.fullName(), contact.email(),
            contact.oidc(), contact.oidcIssuer())));
    personSelection.setItemLabelGenerator(Contact::fullName);
    personSelection.setItems(
        query -> personLookupService.queryPersons(query.getFilter().orElse(""), query.getOffset(),
                query.getLimit())
            .stream()
            .map(contact -> new Contact(contact.fullName(), contact.emailAddress(), contact.oidc(),
                contact.oidcIssuer()
            )));
    personSelection.addValueChangeListener(listener -> loadContact(this, listener.getValue()));
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
    //Avoids Nullpointer exceptions if the user clicks and closes the selection box
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
      orcidSelection.setValue(contact);
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

  public TextField get() {
    return email;
  }


  public TextField getFullNameTextField() {
    return fullName;
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
      addClassNames("flex-vertical", "flex-align-items-baseline", "gap-02");
      setFullNameAndEmail(fullName, email);
      setOidc(oidc, oidcIssuer);
    }

    private void setFullNameAndEmail(String fullName, String email) {
      Span fullNameSpan = new Span(fullName);
      fullNameSpan.addClassName("bold");
      Span emailSpan = new Span(email);
      Span userNameAndFullName = new Span(fullNameSpan, emailSpan);
      userNameAndFullName.addClassNames("gap-02", "flex-horizontal");
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
      oidcSpan.addClassNames("gap-02", "icon-content-center");
      add(oidcSpan);
    }
  }

}
