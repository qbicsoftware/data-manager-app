package life.qbic.datamanager.views.general.funding;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.shared.ClientValidationUtil;
import com.vaadin.flow.component.shared.HasClientValidation;
import com.vaadin.flow.component.textfield.TextField;
import java.io.Serial;

/**
 * <b>Funding information field</b>
 *
 * <p>A custom field with to input fields where users can enter a grant label and grant ID for a
 * project</p>
 *
 * @since 1.0.0
 */
public class FundingField extends CustomField<FundingEntry> implements HasClientValidation {

  @Serial
  private static final long serialVersionUID = 839203706554301417L;
  private final TextField label;
  private final TextField referenceId;

  private boolean invalid;

  public FundingField(String fieldLabel) {
    super();
    invalid = false;
    addClassName("funding-field");
    setLabel(fieldLabel);
    this.label = new TextField("Grant", "e.g. SFB");
    this.label.addClassName("grant-label-field");
    this.referenceId = new TextField("Grant ID", "e.g. SFB 1101");
    this.referenceId.addClassName("grant-id-field");
    label.addValidationStatusChangeListener(e -> validate());
    referenceId.addValidationStatusChangeListener(e -> validate());
    layoutComponent();
  }

  protected void validate() {
    setInvalid(isInvalid());
  }

  private static Div layoutFundingInput(TextField label, TextField referenceId) {
    var layout = new Div(label, referenceId);
    layout.addClassName("input-fields");
    return layout;
  }

  private void layoutComponent() {
    add(layoutFundingInput(label, referenceId));
  }

  @Override
  protected FundingEntry generateModelValue() {
    return new FundingEntry(label.getValue(), referenceId.getValue());
  }

  @Override
  protected void setPresentationValue(FundingEntry funding) {
    this.label.setValue(funding.getLabel());
    this.referenceId.setValue(funding.getReferenceId());
  }


  @Override
  public FundingEntry getEmptyValue() {
    return new FundingEntry(label.getEmptyValue(), referenceId.getEmptyValue());
  }

  @Override
  public void setInvalid(boolean invalid) {
    super.setInvalid(invalid);
    label.setInvalid(invalid);
    referenceId.setInvalid(invalid);
  }





}
