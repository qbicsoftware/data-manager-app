package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.general.funding.FundingField;
import org.springframework.stereotype.Component;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
@Component
public class FundingInformationLayout extends Div {

  private final FundingField fundingField = new FundingField("");
  private final Binder<FundingEntry> fundingEntryBinder = new Binder<>();
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
    fundingEntryBinder.setBean(new FundingEntry("", ""));
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
            "Please provide the grant for the given grant ID.");
    //ToDo Missing Validation
  }
  private BinderValidationStatus<FundingEntry> validateFields() {
    return fundingEntryBinder.validate();
  }

}
