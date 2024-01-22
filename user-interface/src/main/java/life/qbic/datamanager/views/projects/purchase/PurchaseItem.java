package life.qbic.datamanager.views.projects.purchase;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.Objects;

/**
 * <b>Purchase Item</b>
 *
 * <p>Describes an uploaded purchase item (e.g. an offer) and a checkbox to indicate
 * weather the customer has signed the purchase or not (draft).</p>
 *
 * @since 1.0.0
 */
public class PurchaseItem extends Div {
  @Serial
  private static final long serialVersionUID = -1266444866470199274L;
  private final String fileName;
  private final Span fileNameLabel;
  private final Checkbox signedCheckBox;
  public PurchaseItem(String fileName) {
    this.fileName = fileName;
    var fileIcon = VaadinIcon.FILE.create();
    fileIcon.addClassName("file-icon");
    fileNameLabel = new Span(fileIcon, new Span(fileName));
    fileNameLabel.addClassName("file-name");
    signedCheckBox = new Checkbox();
    addClassName("purchase-item");
    Div signatureBox = new Div();
    signatureBox.addClassName("signature-box");
    signatureBox.add(new Span("Signed"));
    signatureBox.add(signedCheckBox);
    add(fileNameLabel, signatureBox);
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
