package life.qbic.datamanager.views.general.contact;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.validator.EmailValidator;

/**
 *
 * <b>A component for contact person input</b>
 *
 * <p>Provides components to add a contact person with a name and email.
 * Includes methods for basic input validation.</p>
 *
 * @since 1.0.0
 */
public class ContactField extends CustomField<Contact> {

  private final TextField nameField;
  private final TextField emailField;
  private final Binder<Contact> binder;

  public ContactField(String label) {
    setLabel(label);
    addClassName("contact-field");
    binder = new Binder<>();
    nameField = new TextField();
    nameField.addClassName("name-field");
    nameField.setPlaceholder("Please enter a name");

    emailField = new TextField();
    emailField.addClassName("email-field");
    emailField.setPlaceholder("Please enter an email address");

    binder.forField(nameField)
        .withValidator(it -> !isRequired() || !it.isBlank(), "Please provide a name")
        .withValidator(it -> !it.isBlank() || emailField.isEmpty(),
            "Please provide a name") // when an email is provided require a name as well
        .bind(Contact::getFullName, Contact::setFullName);

    binder.forField(emailField)
        .withValidator(it -> !isRequired() || !it.isBlank(), "Please provide an email address")
        .withValidator(new EmailValidator(
            "The email address '{0}' is invalid. Please provide a valid email name@domain.de",
            !isRequired()))
        .withValidator(it -> !it.isBlank() || nameField.isEmpty(),
            "Please provide an email address") // when a name is provided require an email as well
        .bind(Contact::getEmail, Contact::setEmail);

    Div layout = new Div(nameField, emailField);
    layout.addClassName("input-fields");
    add(layout);
  }

  public void setContact(Contact contact) {
    binder.setBean(contact);
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

  public boolean isRequired() {
    return isRequiredIndicatorVisible();
  }

  @Override
  public Contact getEmptyValue() {
    return new Contact(nameField.getEmptyValue(), emailField.getEmptyValue());
  }

  /**
   * Validates the values of all bound fields and returns the validation status.
   *
   * @return the validation status of the binder.
   */
  public BinderValidationStatus<Contact> validate() {
    return binder.validate();
  }

  /**
   * Runs all configured field level validators and returns whether any of the validators failed.
   * <p>
   * <b>Note:</b> Calling this method will not trigger status change events, unlike
   * {@link #validate()} and will not modify the UI. To also update error indicators on fields, use
   * {@code validate().isOk()}
   *
   * @return true if the field is valid; false otherwise
   */
  public boolean isValid() {
    return binder.isValid();
  }
}
