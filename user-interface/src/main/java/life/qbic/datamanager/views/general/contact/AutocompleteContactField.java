package life.qbic.datamanager.views.general.contact;

import static java.util.Objects.isNull;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.EmailValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <b>A component for contact person input</b>
 *
 * <p>Provides components to add a contact person with a name and email.
 * Includes methods for basic input validation.</p>
 *
 * @since 1.0.0
 */
public class AutocompleteContactField extends CustomField<Contact> {

  private final ComboBox<Contact> contactSelection;

  private final ComboBox<String> nameField;
  private final ComboBox<String> emailField;
  private final Binder<Contact> binder;

  public AutocompleteContactField(String label) {
    setLabel(label);
    addClassName("contact-field");
    binder = new Binder<>();

    contactSelection = new ComboBox<>();
    contactSelection.addClassName("contact-selection");
    contactSelection.setAllowCustomValue(true);
    contactSelection.setClearButtonVisible(true);
    contactSelection.setRenderer(new ComponentRenderer<>(contact -> {
      var contactName = new Span(contact.getFullName());
      contactName.addClassName("contact-name");
      var contactEmail = new Span(contact.getEmail());
      contactEmail.addClassName("contact-email");
      var container = new Div();
      container.addClassName("contact-item");
      container.add(contactName, contactEmail);
      return container;
    }));
    contactSelection.setItemLabelGenerator(
        contact -> "%s - %s".formatted(contact.getFullName(), contact.getEmail()));

    nameField = new ComboBox<>();
    nameField.setAllowCustomValue(true);
    nameField.setRequired(false);
    nameField.addClassName("name-field");
    nameField.setPlaceholder("Please enter a name");

    emailField = new ComboBox<>();
    emailField.setAllowCustomValue(true);
    emailField.setRequired(false);
    emailField.addClassName("email-field");
    emailField.setPlaceholder("Please enter an email address");

    contactSelection.addValueChangeListener(valueChanged -> {
      Contact selectedContact = valueChanged.getValue();
      if (selectedContact != null) {
        binder.setBean(selectedContact);
      }
      valueChanged.getSource().clear();
    });

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

    Div layout = new Div(nameField, emailField);
    layout.addClassName("input-fields");
    add(contactSelection, layout);
    setItems(new ArrayList<>());
  }

  public void setContact(Contact contact) {
    binder.setBean(contact);
  }

  public void setItems(List<Contact> contacts) {
    contactSelection.setVisible(!contacts.isEmpty());
    nameField.setAutoOpen(!contacts.isEmpty());
    emailField.setAutoOpen(!contacts.isEmpty());

    contactSelection.setItems(contacts);
    nameField.setItems(contacts.stream().map(Contact::getFullName).distinct().toList());
    emailField.setItems(contacts.stream().map(Contact::getEmail).distinct().toList());
  }

  @Override
  protected Contact generateModelValue() {
    return new Contact(Optional.ofNullable(nameField.getValue()).orElse(""),
        Optional.ofNullable(emailField.getValue()).orElse(""));
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
    return new Contact(Optional.ofNullable(nameField.getEmptyValue()).orElse(""),
        Optional.ofNullable(emailField.getEmptyValue()).orElse(""));
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
