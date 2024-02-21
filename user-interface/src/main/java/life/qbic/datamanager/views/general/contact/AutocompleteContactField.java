package life.qbic.datamanager.views.general.contact;

import static java.util.Objects.isNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.EmailValidator;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.general.HasBinderValidation;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
  private final Button selfSelect;
  private final TextField nameField;
  private final TextField emailField;
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

    selfSelect = new Button("Myself");
    selfSelect.addClassName("contact-self-select");
    selfSelect.addClickListener(this::onSelfSelected);

    nameField = new TextField();
    nameField.setRequired(false);
    nameField.addClassName("name-field");
    nameField.setPlaceholder("Please enter a name");

    emailField = new TextField();
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

    Div preselectLayout = new Div(contactSelection, selfSelect);
    preselectLayout.addClassName("prefill-input-fields");

    Div layout = new Div(nameField, emailField);
    layout.addClassName("input-fields");

    add(preselectLayout, layout);
    setItems(new ArrayList<>());
    clear();
  }

  private void onSelfSelected(ClickEvent<Button> buttonClickEvent) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    QbicUserDetails details = (QbicUserDetails) authentication.getPrincipal();
    Contact userAsContact = new Contact(details.fullName(), details.getEmailAddress());
    setContact(userAsContact);
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
    setValue(contact);
  }

  public void setItems(List<Contact> contacts) {
    contactSelection.setItems(contacts);
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

  /**
   * Runs all configured field level validators and returns whether any of the validators failed.
   * <p>
   * <b>Note:</b> Calling this method will not trigger status change events, unlike
   * {@link #validate()} and will not modify the UI. Also updates error indicators on fields.
   *
   * @return true if the field is valid; false otherwise
   */
  public boolean isOk() {
    return binder.validate().isOk();
  }
}
