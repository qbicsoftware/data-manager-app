package life.qbic.datamanager.views.general.contact;

import static java.util.Objects.isNull;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import life.qbic.datamanager.views.general.HasBinderValidation;
import life.qbic.datamanager.views.general.utils.Utility;

/**
 * <b>A component for contact person input</b>
 *
 * <p>Provides components to add a contact person with a name and email.
 * Includes methods for basic input validation.</p>
 *
 * @since 1.0.0
 * @deprecated please use {@link ContactField} and {@link BoundContactField}.
 */
@Deprecated(since = "1.6.0")
public class AutocompleteContactField extends CustomField<Contact> implements
    HasBinderValidation<Contact> {

  private final Checkbox selfSelect;
  private final TextField nameField;
  private final TextField emailField;
  private final Binder<Contact> binder;

  public AutocompleteContactField(String label, String shortName) {
    setLabel(label);
    addClassName("contact-field");
    binder = new Binder<>();
    binder.addStatusChangeListener(event -> updateValidationProperty());

    selfSelect = new Checkbox("Choose myself as %s for this project".formatted(shortName));
    selfSelect.addClassName("contact-self-select");
    selfSelect.addValueChangeListener(this::onSelfSelected);

    nameField = new TextField();
    nameField.setRequired(false);
    nameField.addClassName("name-field");
    nameField.setPlaceholder("Please enter a name");

    emailField = new TextField();
    emailField.setRequired(false);
    emailField.addClassName("email-field");
    emailField.setPlaceholder("Please enter an email address");

    binder.forField(nameField)
        .withValidator(it -> !isRequired()
            || !(isNull(it) || it.isBlank()), "Please provide a name")
        .withValidator(it -> isNull(it) || !it.isBlank() || emailField.isEmpty(),
            "Please provide a name") // when an email is provided require a name as well
        .bind(Contact::getFullName,
            Contact::setFullName);

    binder.forField(emailField)
        .withValidator(it -> !isRequired() || !(isNull(it) || it.isBlank()),
            "Please provide an email address")
        .withValidator(new EmailValidator(
            "The email address '{0}' is invalid. Please provide a valid email name@domain.de",
            !isRequired()))
        .withValidator(it -> isNull(it) || !it.isBlank() || nameField.isEmpty(),
            "Please provide an email address") // when a name is provided require an email as well
        .bind(Contact::getEmail,
            Contact::setEmail);

    Div preselectLayout = new Div(selfSelect);
    preselectLayout.addClassName("prefill-input-fields");

    Div layout = new Div(nameField, emailField);
    layout.addClassName("input-fields");

    add(preselectLayout, layout);
    clear();
  }

  private void onSelfSelected(
      ComponentValueChangeEvent<Checkbox, Boolean> checkboxvalueChangeEvent) {
    if (Boolean.TRUE.equals(checkboxvalueChangeEvent.getValue())) {
      var userAsContact = Utility.tryToLoadFromPrincipal();
      userAsContact.ifPresent(this::setContact);
    }
  }

  private void updateValidationProperty() {
    this.getElement().setProperty("invalid", !binder.isValid());
  }

  public void setContact(Contact contact) {
    binder.setBean(contact);
    updateValidationProperty();
    setValue(contact);
  }

  @Override
  protected Contact generateModelValue() {
    return new Contact(nameField.getValue(), emailField.getValue());
  }

  @Override
  protected void setPresentationValue(Contact contact) {
    nameField.setValue(contact.getFullName());
    emailField.setValue(contact.getEmail());
  }

  public boolean isRequired() {
    return isRequiredIndicatorVisible();
  }

  /**
   * Sets the component to required
   *
   * @param required whether the user is required to setProjectInformation the field
   */
  public void setRequired(boolean required) {
    nameField.setRequired(required);
    emailField.setRequired(required);
    setRequiredIndicatorVisible(required);
  }

  @Override
  public Contact getEmptyValue() {
    return new Contact(nameField.getEmptyValue(), emailField.getEmptyValue());
  }

  @Override
  public String getDefaultErrorMessage() {
    return ""; // we do not show an error message on this field as this is part of the contained fields.
  }

  @Override
  public boolean useBinderErrorMessage() {
    return false;
  }

  @Override
  public Binder<Contact> getBinder() {
    return binder;
  }

}
