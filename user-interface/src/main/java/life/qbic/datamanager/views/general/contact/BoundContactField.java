package life.qbic.datamanager.views.general.contact;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.function.SerializablePredicate;
import java.util.Objects;
import java.util.regex.Pattern;
import life.qbic.datamanager.views.general.HasBoundField;

/**
 * <b>Bound Contact Field</b>
 * <p>
 * Binds a {@link ContactField} to a {@link Contact}-
 *
 * @since 1.6.0
 */
public class BoundContactField implements HasBoundField<ContactField, Contact> {

  private final ContactField contactField;

  private final Binder<ContactContainer> binder;

  private Contact originalValue;

  private BoundContactField(ContactField contactField,
      SerializablePredicate<Contact> predicate) {
    this.contactField = contactField;
    this.binder = createBinder(predicate, contactField);
    binder.addStatusChangeListener(
        event -> updateStatus(contactField, event.hasValidationErrors()));
    this.originalValue = Contact.empty();
  }

  private static void updateStatus(ContactField contactField, boolean isInvalid) {
    contactField.getElement().setProperty("invalid", isInvalid);
  }

  /**
   * The contact field will only invalidate, if one of the fields is empty. Since it is optional,
   * the contact field will not invalidate, if both inputs are empty.
   *
   * @param contactField
   * @return
   * @since
   */
  public static BoundContactField createOptional(ContactField contactField) {
    contactField.setOptional(true);
    return new BoundContactField(contactField, isOptional());
  }

  /**
   * The contact field will invalidate, if one of the fields is empty or both are empty, since it is
   * mandatory to be filled with information.
   *
   * @param contactField
   * @return
   * @since
   */
  public static BoundContactField createMandatory(ContactField contactField) {
    return new BoundContactField(contactField, isMandatory());
  }

  /**
   * This predicate will return <code>true</code>, if all fields are filled.
   * <p>
   * If either is filled alone or none, the predicate will return <code>false</code>
   *
   * @return
   * @since
   */
  private static SerializablePredicate<Contact> isMandatory() {

    SerializablePredicate<Contact> isFilled = contact -> !contact.email().isBlank()
        && !contact.fullName().isBlank();
    return isValidContact().and(isFilled);
  }

  /**
   * This predicate will return <code>true</code>, if all fields are empty or both are filled.
   * <p>
   * If either is filled alone, the predicate will return <code>false</code>
   */
  private static SerializablePredicate<Contact> isOptional() {
    return isValidContact();
  }

  private static SerializablePredicate<Contact> isValidContact() {
    return contact -> {
      var onlyEmailProvided = !contact.email().isBlank() && contact.fullName().isBlank();
      var onlyNameProvided = contact.email().isBlank() && !contact.fullName().isBlank();
      var isValidEmail = contact.email().isBlank() || isValidEmail(contact.email());
      return !onlyEmailProvided && !onlyNameProvided && isValidEmail;
    };
  }

  private static boolean isValidEmail(String email) {
    var pattern = Pattern.compile(EmailValidator.PATTERN);
    var matcher = pattern.matcher(email);
    return matcher.matches();
  }

  private static Binder<ContactContainer> createBinder(SerializablePredicate<Contact> predicate,
      ContactField contactField) {
    Binder<ContactContainer> binder = new Binder<>(ContactContainer.class);
    binder.setBean(new ContactContainer());
    // If the user specified an invalid contact show a general error message (e.g. no selection mode was chosen)
    binder.forField(contactField)
        .withValidator(predicate, "Please specify a valid contact")
        .bind(ContactContainer::getContact, ContactContainer::setContact);
    return binder;
  }

  @Override
  public ContactField getField() {
    return contactField;
  }

  @Override
  public Contact getValue() throws ValidationException {
    var container = new ContactContainer();
    binder.writeBean(container);
    return container.getContact();
  }

  @Override
  public void setValue(Contact value) {
    var container = new ContactContainer();
    container.setContact(value);
    binder.readBean(container);
    originalValue = value;
  }

  @Override
  public boolean isValid() {
    return binder.validate().isOk();
  }

  @Override
  public boolean hasChanged() {
    return binder.hasChanges() || isDifferent(originalValue, binder.getBean());
  }

  private boolean isDifferent(Contact originalValue, ContactContainer bean) {
    var newValue = bean.getContact();
    return !Objects.equals(originalValue, newValue);
  }


  private static class ContactContainer {

    private Contact contact;

    public ContactContainer() {
      contact = new Contact("", "", "", "");
    }

    public Contact getContact() {
      return contact;
    }

    public void setContact(Contact contact) {
      this.contact = Objects.requireNonNull(contact);
    }
  }
}
