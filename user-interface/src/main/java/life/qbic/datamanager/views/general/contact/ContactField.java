package life.qbic.datamanager.views.general.contact;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.shared.HasClientValidation;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Objects;


public class ContactField extends CustomField<Contact> implements HasClientValidation {

  public static final String GAP_M_CSS = "gap-m";
  private static final String FULL_WIDTH_CSS = "full-width";
  private final TextField fullName;
  private final TextField email;
  private final Checkbox setMyselfCheckBox;
  private Contact myself;
  private boolean isOptional = true;

  private ContactField(String label) {
    this.fullName = withErrorMessage(withPlaceHolder(new TextField(), "Please provide a name"),
        "");
    this.email = withErrorMessage(withPlaceHolder(new TextField(), "Please enter an email address"),
        "");
    this.setMyselfCheckBox = new Checkbox();
    setLabel(label);
    add(layoutFields(setMyselfCheckBox, layoutFields(fullName, email)));
    hideCheckbox(); // default is to hide the set myself checkbox
    setMyselfCheckBox.addValueChangeListener(listener -> {
      if (isChecked(listener.getSource())) {
        loadContact(this, myself);
      }
    });
    fullName.addValueChangeListener(listener -> {
      updateValue();
    });
    email.addValueChangeListener(listener -> {
      updateValue();
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

  public static ContactField createWithMyselfOption(String label, Contact myself, String hint,
      boolean setOptional) {
    var contactField = createSimple(label);
    contactField.setMyself(myself, hint);
    contactField.setOptional(setOptional);
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

}
