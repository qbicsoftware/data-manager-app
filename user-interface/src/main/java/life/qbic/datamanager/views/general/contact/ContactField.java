package life.qbic.datamanager.views.general.contact;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.shared.HasClientValidation;
import com.vaadin.flow.component.textfield.TextField;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ContactField extends CustomField<Contact> implements HasClientValidation {

  private final TextField fullName;
  private final TextField email;

  public ContactField(String label) {
    this.fullName = withErrorMessage(withPlaceHolder(new TextField(), "Please provide a name"),
        "Name is missing");
    this.email = withErrorMessage(withPlaceHolder(new TextField(), "Please enter an email address"),
        "Email is missing");
    setLabel(label);
    add(layoutFields(fullName, email));
  }

  private static TextField withPlaceHolder(TextField textField, String placeHolder) {
    textField.setPlaceholder(placeHolder);
    return textField;
  }

  private static TextField withErrorMessage(TextField textField, String errorMessage) {
    textField.setErrorMessage(errorMessage);
    return textField;
  }

  private static Div layoutFields(TextField fullName, TextField email) {
    var layout = new Div(fullName, email);
    layout.addClassNames("flex-horizontal", "gap-m", "full-width");
    fullName.addClassName("full-width");
    email.addClassName("full-width");
    return layout;
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
