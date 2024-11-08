package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.ValidationException;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.datamanager.views.projects.create.FundingInformationLayout;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FundingInformationForm extends FormLayout {

  private final FundingInformationLayout fundingInformationLayout;

  private ProjectInformation oldValue;

  public FundingInformationForm() {
    super();
    fundingInformationLayout = new FundingInformationLayout();
    addClassName("form-content");
    add(fundingInformationLayout);
  }

  public void setContent(ProjectInformation information) {
    oldValue = ProjectInformation.copy(information);
    this.fundingInformationLayout.setFundingInformation(
        information.getFundingEntry().orElse(new FundingEntry("", "")));
  }


  public boolean isValid() {
    return fundingInformationLayout.isValid();
  }

  public boolean hasChanges() {
    return hasChanged(oldValue.getFundingEntry().orElse(new FundingEntry("", "")),
        fundingInformationLayout.getFundingInformation());
  }

  private boolean hasChanged(FundingEntry oldValue, FundingEntry newValue) {
    return !oldValue.equals(newValue);
  }

  public ProjectInformation fromUserInput() throws ValidationException {
    var userInput = ProjectInformation.copy(oldValue);
    userInput.setFundingEntry(fundingInformationLayout.getFundingInformationWithException());
    return userInput;
  }
}
