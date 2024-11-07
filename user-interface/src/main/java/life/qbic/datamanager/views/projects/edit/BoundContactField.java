package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.function.SerializablePredicate;
import java.util.Objects;
import life.qbic.datamanager.views.general.BoundField;
import life.qbic.datamanager.views.general.contact.Contact;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class BoundContactField implements BoundField<ContactField, Contact> {

  private final ContactField contactField;

  private final Binder<ContactContainer> binder;

  private BoundContactField(ContactField contactField,
      SerializablePredicate<Contact> predicate) {
    this.contactField = contactField;
    this.binder = createBinder(predicate);
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
    return contact -> {
      var onlyEmailEmpty = contact.getEmail().isBlank() && !contact.getFullName().isBlank();
      var onlyNameEmpty = !contact.getEmail().isBlank() && contact.getFullName().isBlank();
      var bothEmpty = contact.getEmail().isBlank() && contact.getFullName().isBlank();
      return !(onlyEmailEmpty || onlyNameEmpty || bothEmpty);
    };
  }

  /**
   * This predicate will return <code>true</code>, if all fields are empty or both are filled.
   * <p>
   * If either is filled alone, the predicate will return <code>false</code>
   *
   * @return
   * @since
   */
  private static SerializablePredicate<Contact> isOptional() {
    return contact -> {
      var onlyEmailEmpty = contact.getEmail().isBlank() && !contact.getFullName().isBlank();
      var onlyNameEmpty = !contact.getEmail().isBlank() && contact.getFullName().isBlank();
      return !(onlyEmailEmpty || onlyNameEmpty);
    };
  }

  private Binder<ContactContainer> createBinder(SerializablePredicate<Contact> predicate) {
    Binder<ContactContainer> binder = new Binder<>(ContactContainer.class);
    binder.setBean(new ContactContainer());
    binder.forField(contactField).withValidator(predicate, "There is still information missing")
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
  }

  @Override
  public boolean isValid() {
   return binder.validate().isOk();
  }

  @Override
  public boolean hasChanged() {
    return binder.hasChanges();
  }

  private static class ContactContainer {

    private Contact contact;

    public ContactContainer() {
      contact = new Contact("", "");
    }

    public Contact getContact() {
      return contact;
    }

    public void setContact(Contact contact) {
      this.contact = Objects.requireNonNull(contact);
    }

  }
}
