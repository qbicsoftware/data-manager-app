package life.qbic.datamanager.views.general.contact;

import static java.util.Objects.isNull;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.EmailValidator;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.general.HasBinderValidation;

/**
 * <b>A component for contact person input</b>
 *
 * <p>Provides components to add a contact person with a name and email.
 * Includes methods for basic input validation.</p>
 *
 * @since 1.0.0
 */
public class AutocompleteContactField extends CustomField<Contact> implements
    HasBinderValidation<Contact> {


  private final ComboBox<Contact> contactSelection;

  private final ComboBox<String> nameField;
  private final ComboBox<String> emailField;
  private final Binder<Contact> binder;

  public AutocompleteContactField(String label) {
    setLabel(label);
    addClassName("contact-field");
    binder = new Binder<>();
    binder.addStatusChangeListener(event -> updateValidationProperty());

    contactSelection = new ComboBox<>();
    contactSelection.addClassName("contact-selection");
    contactSelection.setPlaceholder("(Optional) select from existing contacts");
    contactSelection.setAllowCustomValue(false);
    contactSelection.setClearButtonVisible(true);
    contactSelection.setRenderer(new ComponentRenderer<>(AutocompleteContactField::renderContact));
    contactSelection.setItemLabelGenerator(
        contact -> "%s - %s".formatted(contact.getFullName(), contact.getEmail()));

    nameField = new ComboBox<>();
    nameField.setAllowCustomValue(true);
    nameField.addCustomValueSetListener(
        customValueSet -> customValueSet.getSource().setValue(customValueSet.getDetail()));
    nameField.setRequired(false);
    nameField.addClassName("name-field");
    nameField.setPlaceholder("Please enter a name");

    emailField = new ComboBox<>();
    emailField.setAllowCustomValue(true);
    emailField.addCustomValueSetListener(
        customValueSet -> customValueSet.getSource().setValue(customValueSet.getDetail()));
    emailField.setRequired(false);
    emailField.addClassName("email-field");
    emailField.setPlaceholder("Please enter an email address");

    contactSelection.addValueChangeListener(this::onContactSelectionChanged); //write only

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
    clear();
  }

  private void updateValidationProperty() {
    this.getElement().setProperty("invalid", !binder.isValid());
  }

  private static Div renderContact(Contact contact) {
    var contactName = new Span(contact.getFullName());
    contactName.addClassName("contact-name");
    var contactEmail = new Span(contact.getEmail());
    contactEmail.addClassName("contact-email");
    var container = new Div();
    container.addClassName("contact-item");
    container.add(contactName, contactEmail);
    return container;
  }

  private void onContactSelectionChanged(
      ComponentValueChangeEvent<ComboBox<Contact>, Contact> valueChanged) {
    //ignore clearing the combobox or empty selection
    if (valueChanged.getValue() == null) {
      return;
    }
    if (valueChanged.getValue().isEmpty()) {
      return;
    }
    // update the contact to the selected value
    setContact(valueChanged.getValue());
    // clear selection box
    valueChanged.getHasValue().clear();
  }

  public void setContact(Contact contact) {
    binder.setBean(contact);
    updateValidationProperty();
  }

  public void setItems(List<Contact> contacts) {
    List<String> fullNames = contacts.stream()
        .map(Contact::getFullName)
        .distinct()
        .toList();
    List<String> emails = contacts.stream()
        .map(Contact::getEmail)
        .distinct()
        .toList();

    contactSelection.setItems(contacts);
    nameField.setItems(fullNames);
    emailField.setItems(emails);
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

  @Override
  public Binder<Contact> getBinder() {
    return binder;
  }
}
