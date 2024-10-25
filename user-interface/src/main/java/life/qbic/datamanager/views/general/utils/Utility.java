package life.qbic.datamanager.views.general.utils;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class Utility {

  public static void addConsumedLengthHelper(TextField textField) {
    int maxLength = textField.getMaxLength();
    int consumedLength = textField.getValue().length();
    textField.setHelperText(consumedLength + "/" + maxLength);
  }

  public static void addConsumedLengthHelper(TextArea textArea) {
    int maxLength = textArea.getMaxLength();
    int consumedLength = textArea.getValue().length();
    textArea.setHelperText(consumedLength + "/" + maxLength);
  }
}
