package life.qbic.datamanager.views.general.funding;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
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
  private final Div layoutFundingInput;

  public FundingField(String fieldLabel) {
    super();
    addClassName("funding-field");
    this.label = new TextField("Grant", "e.g. SFB");
    this.label.addClassName("grant-label-field");
    this.referenceId = new TextField("Grant ID", "e.g. SFB 1101");
    this.referenceId.addClassName("grant-id-field");
    // we need to override the text-fields internal default validation, since we do not directly add binders
    // with validators to the encapsulated fields, which results in removal of the invalid HTML property and disabling
    // us correctly display invalid element status
    setLabel(fieldLabel);
    label.addValidationStatusChangeListener(e -> validate());
    referenceId.addValidationStatusChangeListener(e -> validate());
    layoutFundingInput = layoutFundingInput(label, referenceId);
    add(layoutFundingInput);
  }

  public static FundingField createVertical(String fieldLabel) {
    return new FundingField(fieldLabel);
  }

  public static FundingField createHorizontal(String fieldLabel) {
    var field = new FundingField(fieldLabel);
    field.layoutFundingInput.addClassNames("flex-horizontal", "gap-m");
    return field;
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
    // we track the funding field element's invalid status explicitly
    super.setInvalid(invalid);

    // we forward the invalid status to both fields, to get Vaadin's error rendering support for
    // text-fields via the 'invalid' HTML property
    label.setInvalid(invalid);
    referenceId.setInvalid(invalid);
  }

  public void setLabel(String label) {
    this.label.setValue(label);
  }

  public void setReferenceId(String referenceId) {
    this.referenceId.setValue(referenceId);
  }

}
