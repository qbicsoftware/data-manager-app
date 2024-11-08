package life.qbic.datamanager.views.projects;

import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.funding.FundingEntry;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public final class ProjectInformation implements Serializable {

  private String projectId = "";

  @Serial
  private static final long serialVersionUID = -7260109309939021850L;
  @NotEmpty
  private String projectTitle = "";
  @NotEmpty
  private String projectObjective = "";

  private FundingEntry fundingEntry;
  @NotEmpty
  private Contact principalInvestigator;
  private Contact responsiblePerson;
  @NotEmpty
  private Contact projectManager;

  public Optional<FundingEntry> getFundingEntry() {
    if (fundingEntry == null || fundingEntry.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(fundingEntry);
  }

  public static ProjectInformation copy(ProjectInformation projectInformation) {
    ProjectInformation copy = new ProjectInformation();
    copy.projectId = projectInformation.projectId;
    copy.projectTitle = projectInformation.projectTitle;
    copy.projectObjective = projectInformation.projectObjective;
    copy.fundingEntry = projectInformation.fundingEntry;
    copy.principalInvestigator = projectInformation.principalInvestigator;
    copy.responsiblePerson = projectInformation.responsiblePerson;
    copy.projectManager = projectInformation.projectManager;
    return copy;
  }

  public void setFundingEntry(FundingEntry fundingEntry) {
    this.fundingEntry = fundingEntry;
  }

  public void setProjectTitle(String projectTitle) {
    this.projectTitle = projectTitle;
  }

  public void setProjectObjective(String projectObjective) {
    this.projectObjective = projectObjective;
  }

  public Contact getPrincipalInvestigator() {
    return principalInvestigator;
  }

  public void setPrincipalInvestigator(
      Contact principalInvestigator) {
    this.principalInvestigator = principalInvestigator;
  }

  public Optional<Contact> getResponsiblePerson() {
    return Optional.ofNullable(responsiblePerson);
  }

  public void setResponsiblePerson(Contact responsiblePerson) {
    if (responsiblePerson.getFullName().isBlank() || responsiblePerson.getEmail().isBlank()) {
      this.responsiblePerson = null;
      return;
    }
    this.responsiblePerson = responsiblePerson;
  }

  public Contact getProjectManager() {
    return projectManager;
  }

  public void setProjectManager(Contact projectManager) {
    this.projectManager = projectManager;
  }

  public String getProjectTitle() {
    return projectTitle;
  }

  public String getProjectObjective() {
    return projectObjective;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getProjectId() {
    return projectId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectInformation that = (ProjectInformation) o;
    return Objects.equals(projectTitle, that.projectTitle) && Objects.equals(
        projectObjective, that.projectObjective) && Objects.equals(fundingEntry,
        that.fundingEntry) && Objects.equals(principalInvestigator,
        that.principalInvestigator) && Objects.equals(responsiblePerson,
        that.responsiblePerson) && Objects.equals(projectManager, that.projectManager);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectTitle, projectObjective, fundingEntry, principalInvestigator,
        responsiblePerson, projectManager);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ProjectInformation.class.getSimpleName() + "[", "]")
        .add("projectTitle='" + projectTitle + "'")
        .add("projectObjective='" + projectObjective + "'")
        .add("principalInvestigator=" + principalInvestigator)
        .add("responsiblePerson=" + responsiblePerson)
        .add("projectManager=" + projectManager)
        .toString();
  }
}
