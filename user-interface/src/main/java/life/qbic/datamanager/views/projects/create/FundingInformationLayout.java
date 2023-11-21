package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
import java.io.Serial;
import java.io.Serializable;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.general.funding.FundingField;
import org.springframework.stereotype.Component;

/**
 * <b>Funding Information Layout</b>
 *
 * <p>Layout which enables the user to input the information associated with the funding information
 * during project creation and validates the provided information</p>
 */
@Component
public class FundingInformationLayout extends Div implements HasValidation {

  private final FundingField fundingField = new FundingField("");
  private final Binder<FundingInformationContainer> fundingEntryBinder = new Binder<>(
      FundingInformationContainer.class);
  private static final String TITLE = "Funding Information";

  /**
   * Creates a new empty div.
   */
  public FundingInformationLayout() {
    initLayout();
    initFieldValidators();
  }

  private void initLayout() {
    Span fundingInformationTitle = new Span(TITLE);
    fundingInformationTitle.addClassNames("title");
    Span fundingInformationDescription = new Span("Description Text");
    add(fundingInformationTitle, fundingInformationDescription, fundingField);
    addClassName("funding-information-layout");
  }

  private void initFieldValidators() {
    fundingEntryBinder.forField(fundingField).withValidator(value -> {
          if (value == null) {
            return true;
          }
          return !value.getReferenceId().isBlank() || value.getLabel().isBlank();
        }, "Please provide the grant ID for the given grant")
        .withValidator(value -> {
              if (value == null) {
                return true;
              }
              return value.getReferenceId().isBlank() || !value.getLabel().isBlank();
            },
            "Please provide the grant for the given grant ID.")
        .bind((FundingInformationContainer::getFundingEntry),
            FundingInformationContainer::setFundingEntry);
  }

  public FundingEntry getFundingInformation() {
    FundingInformationContainer fundingInformationContainer = new FundingInformationContainer();
    fundingEntryBinder.writeBeanIfValid(fundingInformationContainer);
    return fundingInformationContainer.getFundingEntry();
  }

  /**
   * Sets an error message to the component.
   * <p>
   * The Web Component is responsible for deciding when to show the error message to the user, and
   * this is usually triggered by triggering the invalid state for the Web Component. Which means
   * that there is no need to clean up the message when component becomes valid (otherwise it may
   * lead to undesired visual effects).
   *
   * @param errorMessage a new error message
   */
  @Override
  public void setErrorMessage(String errorMessage) {
  }

  /**
   * Gets current error message from the component.
   *
   * @return current error message
   */
  @Override
  public String getErrorMessage() {
    return "Invalid Input found in Funding Information";
  }

  /**
   * Sets the validity of the component input.
   * <p>
   * When component becomes valid it hides the error message by itself, so there is no need to clean
   * up the error message via the {@link #setErrorMessage(String)} call.
   *
   * @param invalid new value for component input validity
   */
  @Override
  public void setInvalid(boolean invalid) {

  }

  /**
   * Returns {@code true} if component input is invalid, {@code false} otherwise.
   *
   * @return whether the component input is valid
   */
  @Override
  public boolean isInvalid() {
    fundingEntryBinder.validate();
    return !fundingEntryBinder.isValid() && fundingField.isInvalid();
  }


  /*Vaadin expects Beans with fields for its components which is why this container is necessary*/
  public static final class FundingInformationContainer implements Serializable {

    private FundingEntry fundingEntry;
    @Serial
    private static final long serialVersionUID = -2344442429490382769L;

    public FundingEntry getFundingEntry() {
      return fundingEntry;
    }

    public void setFundingEntry(FundingEntry fundingEntry) {
      this.fundingEntry = fundingEntry;
    }

  }
}
