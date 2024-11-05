package life.qbic.datamanager.views.general.funding;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.ValidationException;
import java.util.Objects;
import life.qbic.datamanager.views.general.BoundField;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FundingInputForm extends FormLayout  {

  private final BoundField<FundingField, FundingEntry> fundingField;

  public static FundingInputForm create(BoundField<FundingField, FundingEntry> fundingField) {
    Objects.requireNonNull(fundingField);
    return new FundingInputForm(fundingField);
  }

  private FundingInputForm(BoundField<FundingField, FundingEntry> fundingField) {
    this.fundingField = fundingField;
    add(fundingField.getField());
  }

  public void setContent(FundingEntry fundingEntry) {
    fundingField.setValue(fundingEntry);
  }

  public FundingEntry fromUserInput() throws ValidationException {
    return fundingField.getValue();
  }

  public boolean hasChanges() {
    return fundingField.hasChanged();
  }
}
