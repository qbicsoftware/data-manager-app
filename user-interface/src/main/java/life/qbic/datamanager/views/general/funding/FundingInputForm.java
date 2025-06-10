package life.qbic.datamanager.views.general.funding;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.ValidationException;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.general.HasBoundField;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import org.springframework.lang.NonNull;

/**
 * <b>Funding Input Form</b>
 * <p>
 * Form that can be used to request funding information about a project from a user.
 *
 * @since 1.6.0
 */
public class FundingInputForm extends FormLayout implements UserInput {

  private final transient HasBoundField<FundingField, FundingEntry> fundingField;

  private FundingInputForm(HasBoundField<FundingField, FundingEntry> fundingField) {
    this.fundingField = fundingField;
    add(fundingField.getField());
  }

  public static FundingInputForm create(HasBoundField<FundingField, FundingEntry> fundingField) {
    Objects.requireNonNull(fundingField);
    return new FundingInputForm(fundingField);
  }

  public void setContent(FundingEntry fundingEntry) {
    fundingField.setValue(fundingEntry);
  }

  public FundingEntry fromUserInput() throws ValidationException {
    return fundingField.getValue();
  }

  public Optional<FundingEntry> getIfValid() {
    if (fundingField.isValid()) {
      try {
        return Optional.ofNullable(fundingField.getValue());
      } catch (ValidationException e) {
        // swallow exception by design
      }
    }
    return Optional.empty();
  }


  @Override
  @NonNull
  public InputValidation validate() {
    if (fundingField.isValid()) {
      return InputValidation.passed();
    }
    return InputValidation.failed();
  }

  @Override
  public boolean hasChanges() {
    return fundingField.hasChanged();
  }
}
