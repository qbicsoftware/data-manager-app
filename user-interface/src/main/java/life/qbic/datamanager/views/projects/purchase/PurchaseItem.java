package life.qbic.datamanager.views.projects.purchase;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import java.io.Serial;
import java.util.Objects;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class PurchaseItem extends Div {

  @Serial
  private static final long serialVersionUID = -1266444866470199274L;

  private final String fileName;

  private final Div fileNameLabel;

  private final Checkbox signedCheckBox;

  public PurchaseItem(String fileName) {
    this.fileName = fileName;
    fileNameLabel = new Div();
    fileNameLabel.setText(fileName);
    signedCheckBox = new Checkbox();

    addClassName("purchase-item");
    add(fileNameLabel, signedCheckBox);
  }

  public String fileName() {
    return fileName;
  }

  public boolean isSigned() {
    return signedCheckBox.getValue();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PurchaseItem that = (PurchaseItem) o;
    return Objects.equals(fileName, that.fileName) && Objects.equals(
        fileNameLabel, that.fileNameLabel) && Objects.equals(signedCheckBox,
        that.signedCheckBox);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, fileNameLabel, signedCheckBox);
  }
}
