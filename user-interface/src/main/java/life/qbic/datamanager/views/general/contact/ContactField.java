package life.qbic.datamanager.views.general.contact;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.shared.HasClientValidation;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Objects;


public class ContactField extends CustomField<Contact> implements HasClientValidation {

  private final TextField fullName;
  private final TextField email;
  private final Checkbox setMyselfCheckBox;

  private Contact myself;

  private ContactField(String label) {
    this.fullName = withErrorMessage(withPlaceHolder(new TextField(), "Please provide a name"),
        "Name is missing");
    this.email = withErrorMessage(withPlaceHolder(new TextField(), "Please enter an email address"),
        "Email is missing");
    this.setMyselfCheckBox = new Checkbox();
    setLabel(label);
    add(layoutFields(setMyselfCheckBox, layoutFields(fullName, email)));
    hideCheckbox(); // default is to hide the set myself checkbox
    setMyselfCheckBox.addValueChangeListener(listener -> {
      if (isChecked(listener.getSource())) {
        loadContact(this, myself);
      }
    });
  }

  private static void loadContact(ContactField field, Contact contact) {
    field.setPresentationValue(contact);
  }

  private static boolean isChecked(Checkbox checkbox) {
    return checkbox.getValue();
  }

  public static ContactField createSimple(String label) {
    return new ContactField(label);
  }

  public static ContactField createWithMyselfOption(String label, Contact myself, String hint) {
    var contactField = createSimple(label);
    contactField.setMyself(myself, hint);
    return contactField;
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
    layout.addClassNames("flex-vertical", "gap-m");
    layout.add(box);
    layout.add(fields);
    return layout;
  }

  private static Div layoutFields(TextField fullName, TextField email) {
    var layout = new Div(fullName, email);
    layout.addClassNames("flex-horizontal", "gap-m", "full-width");
    fullName.addClassName("full-width");
    email.addClassName("full-width");
    return layout;
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
    return new Contact(fullName.getValue(), email.getValue());
  }

  @Override
  protected void setPresentationValue(Contact contact) {
    fullName.setValue(contact.getFullName());
    email.setValue(contact.getEmail());
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

  private void removeErrors() {
    email.setInvalid(false);
    fullName.setInvalid(false);
  }

  private void invalidate() {
    if (email.getValue().isBlank()) {
      email.setInvalid(true);
    }
    if (fullName.getValue().isBlank()) {
      fullName.setInvalid(true);
    }
  }

}
