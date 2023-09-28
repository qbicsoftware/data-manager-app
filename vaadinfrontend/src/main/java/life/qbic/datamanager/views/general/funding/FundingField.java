package life.qbic.datamanager.views.general.funding;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FundingField extends CustomField<FundingEntry> {

  @Serial
  private static final long serialVersionUID = 839203706554301417L;
  private final TextField label;
  private final TextField referenceId;
  private final Binder<FundingEntry> binder;

  public FundingField(String fieldLabel) {
    super();
    setLabel(fieldLabel);
    this.label = new TextField("Grant", "e.g. SFB");
    this.referenceId = new TextField("Grant ID", "e.g. SFB 1101");
    this.binder = new Binder<>();
    layoutComponent();
    configureBinder();
  }

  private void configureBinder() {

  }

  private void layoutComponent() {
    add(layoutFundingInput(label, referenceId));
  }

  private static Div layoutFundingInput(TextField label, TextField referenceId) {
    var layout = new Div(label, referenceId);
    layout.addClassName("input-fields");
    return layout;
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
  public FundingEntry getEmptyValue(){
    return new FundingEntry(label.getEmptyValue(), referenceId.getEmptyValue());
  }

}
