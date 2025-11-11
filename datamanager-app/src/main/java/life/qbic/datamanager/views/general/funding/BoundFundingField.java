package life.qbic.datamanager.views.general.funding;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.Validator;
import java.util.Objects;
import life.qbic.datamanager.views.general.HasBoundField;

/**
 * <b>Bound Funding Field</b>
 * <p>
 * Binds a {@link FundingField} to a {@link FundingEntry}-
 *
 * @since 1.6.0
 */
public class BoundFundingField implements HasBoundField<FundingField, FundingEntry> {

  private final FundingField fundingField;

  private final Binder<FundingInformationContainer> binder;

  private FundingEntry initValue;

  public BoundFundingField(FundingField fundingField) {
    this.fundingField = Objects.requireNonNull(fundingField);
    this.binder = new Binder<>(FundingInformationContainer.class);
    bindSimple(binder, fundingField);
  }

  @SafeVarargs
  public BoundFundingField(FundingField fundingField, Validator<FundingEntry>... validators) {
    this.fundingField = Objects.requireNonNull(fundingField);
    this.binder = new Binder<>(FundingInformationContainer.class);
    bindWithValidators(binder, fundingField, validators);
  }

  private static void bindSimple(Binder<FundingInformationContainer> binder,
      FundingField fundingField) {
    binder.forField(fundingField)
        .bind(FundingInformationContainer::get, FundingInformationContainer::set);
  }

  @SafeVarargs
  private static void bindWithValidators(Binder<FundingInformationContainer> binder,
      FundingField fundingField, Validator<FundingEntry>... validators) {
    var binding = binder.forField(fundingField);
    for (Validator<FundingEntry> validator : validators) {
      binding.withValidator(validator);
    }
    binding.bind(FundingInformationContainer::get, FundingInformationContainer::set);
  }

  @Override
  public FundingField getField() {
    return fundingField;
  }

  @Override
  public FundingEntry getValue() throws ValidationException {
    var container = new FundingInformationContainer();
    binder.writeBean(container);
    return container.get();
  }

  @Override
  public void setValue(FundingEntry value) {
    initValue = value;
    var container = new FundingInformationContainer();
    container.set(value);
    binder.setBean(container);
  }

  @Override
  public boolean isValid() {
    return binder.isValid();
  }

  @Override
  public boolean hasChanged() {
    return binder.hasChanges() || hasChanged(initValue, binder.getBean().get());
  }

  private boolean hasChanged(FundingEntry oldValue, FundingEntry newValue) {
    return !oldValue.equals(newValue);
  }

  public static final class FundingInformationContainer {

    private FundingEntry fundingEntry;

    public FundingEntry get() {
      return fundingEntry;
    }

    public void set(FundingEntry fundingEntry) {
      this.fundingEntry = fundingEntry;
    }

  }
}
