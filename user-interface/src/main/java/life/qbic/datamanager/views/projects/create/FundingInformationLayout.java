package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import java.io.Serial;
import java.io.Serializable;
import life.qbic.datamanager.views.general.HasBinderValidation;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.general.funding.FundingField;
import life.qbic.datamanager.views.projects.create.FundingInformationLayout.FundingInformationContainer;
import org.springframework.stereotype.Component;


public class FundingInformationLayout extends Div implements
    HasBinderValidation<FundingInformationContainer> {

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

  public FundingInformationLayout(FundingEntry fundingEntry) {
    this();
    fundingField.setLabel(fundingEntry.getLabel());
    fundingField.setReferenceId(fundingEntry.getReferenceId());
  }

  public void setFundingInformation(FundingEntry fundingEntry) {
    fundingField.setLabel(fundingEntry.getLabel());
    fundingField.setReferenceId(fundingEntry.getReferenceId());
  }

  private void initLayout() {
    Span fundingInformationTitle = new Span(TITLE);
    fundingInformationTitle.addClassNames("title");
    Span fundingInformationDescription = new Span(
        "Specify the name and id of the research project grant, if present.");
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

  public FundingEntry getFundingInformationWithException() throws ValidationException {
    FundingInformationContainer fundingInformationContainer = new FundingInformationContainer();
    fundingEntryBinder.writeBean(fundingInformationContainer);
    return fundingInformationContainer.getFundingEntry();
  }

  @Override
  public Binder<FundingInformationContainer> getBinder() {
    return fundingEntryBinder;
  }

  /**
   * Gets current error message from the component.
   *
   * @return current error message
   */
  @Override
  public String getDefaultErrorMessage() {
    return "Invalid Input found in Funding Information";
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
