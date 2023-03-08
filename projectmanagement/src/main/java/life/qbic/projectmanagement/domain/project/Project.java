package life.qbic.projectmanagement.domain.project;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.repository.jpa.OfferIdentifierConverter;

/**
 * A project planned and run at QBiC.
 *
 * @since <version tag>
 */
@Entity
@Table(name = "projects_datamanager")
public class Project {

  @EmbeddedId
  private ProjectId projectId;

  @Embedded
  private ProjectIntent projectIntent;

  @AttributeOverride(name = "uuid", column = @Column(name = "activeExperiment"))
  private ExperimentId activeExperiment;

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL)
  @JoinColumn(name = "project")
  private List<Experiment> experiments;

  @Convert(converter = ProjectCode.Converter.class)
  @Column(name = "projectCode", nullable = false)
  private ProjectCode projectCode;

  @Column(name = "lastModified", nullable = false)
  private Instant lastModified;

  @ElementCollection(fetch = FetchType.EAGER, targetClass = String.class)
  @Convert(converter = OfferIdentifierConverter.class)
  @CollectionTable(name = "projects_offers", joinColumns = @JoinColumn(name = "projectIdentifier"))
  @Column(name = "offerIdentifier")
  private List<OfferIdentifier> linkedOffers;

  @Embedded
  @AttributeOverrides({
          @AttributeOverride(name = "referenceId", column = @Column(name = "projectManagerReferenceId")),
          @AttributeOverride(name = "fullName", column = @Column(name = "projectManagerFullName")),
          @AttributeOverride(name = "emailAddress", column = @Column(name = "projectManagerEmailAddress"))
  })
  private PersonReference projectManager;

  @Embedded
  @AttributeOverrides({
          @AttributeOverride(name = "referenceId", column = @Column(name = "principalInvestigatorReferenceId")),
          @AttributeOverride(name = "fullName", column = @Column(name = "principalInvestigatorFullName")),
          @AttributeOverride(name = "emailAddress", column = @Column(name = "principalInvestigatorEmailAddress"))
  })
  private PersonReference principalInvestigator;

  @Embedded
  @AttributeOverrides({
          @AttributeOverride(name = "referenceId", column = @Column(name = "responsibePersonReferenceId")),
          @AttributeOverride(name = "fullName", column = @Column(name = "responsibePersonFullName")),
          @AttributeOverride(name = "emailAddress", column = @Column(name = "responsibePersonEmailAddress"))
  })
  private PersonReference responsiblePerson;


  private Project(ProjectId projectId, ProjectIntent projectIntent, ProjectCode projectCode,
      PersonReference projectManager, PersonReference principalInvestigator,
      PersonReference responsiblePerson) {
    requireNonNull(principalInvestigator);
    requireNonNull(projectCode);
    requireNonNull(projectId);
    requireNonNull(projectIntent);
    requireNonNull(projectManager);
    setPrincipalInvestigator(principalInvestigator);
    setProjectCode(projectCode);
    setProjectId(projectId);
    setProjectIntent(projectIntent);
    setProjectManager(projectManager);
    setResponsiblePerson(responsiblePerson);
    linkedOffers = new ArrayList<>();
    experiments = new ArrayList<>();
  }

  public void setProjectManager(PersonReference projectManager) {
    this.projectManager = projectManager;
    this.lastModified = Instant.now();
  }

  public void setPrincipalInvestigator(PersonReference principalInvestigator) {
    this.principalInvestigator = principalInvestigator;
    this.lastModified = Instant.now();
  }

  public void setResponsiblePerson(PersonReference responsiblePerson) {
    this.responsiblePerson = responsiblePerson;
    this.lastModified = Instant.now();
  }

  private void setProjectCode(ProjectCode projectCode) {
    this.projectCode = projectCode;
  }

  protected Project() {
    linkedOffers = new ArrayList<>();
  }

  public void updateTitle(ProjectTitle title) {
    if (projectIntent.projectTitle().equals(title)) {
      return;
    }
    projectIntent.projectTitle(title);
    lastModified = Instant.now();
  }

  public void describeExperimentalDesign(
      ExperimentalDesignDescription experimentalDesignDescription) {
    if (projectIntent.experimentalDesign().equals(experimentalDesignDescription)) {
      return;
    }
    projectIntent.experimentalDesign(experimentalDesignDescription);
    lastModified = Instant.now();
  }

  public void addExperiment(Experiment experiment) {
    activeExperiment = experiment.experimentId();
    experiments.add(experiment);
    lastModified = Instant.now();
  }

  public void stateObjective(ProjectObjective projectObjective) {
    if (projectIntent.objective().equals(projectObjective)) {
      return;
    }
    this.projectIntent.objective(projectObjective);
    lastModified = Instant.now();
  }


  public void linkOffer(OfferIdentifier offerIdentifier) {
    if (linkedOffers.contains(offerIdentifier)) {
      return;
    }
    linkedOffers.add(offerIdentifier);
    this.lastModified = Instant.now();
  }

  public void unlinkOffer(OfferIdentifier offerIdentifier) {
    boolean offerRemoved = linkedOffers.remove(offerIdentifier);
    if (offerRemoved) {
      this.lastModified = Instant.now();
    }
  }

  public List<OfferIdentifier> linkedOffers() {
    return linkedOffers.stream().toList();
  }

  protected void setProjectId(ProjectId projectId) {
    this.projectId = projectId;
    this.lastModified = Instant.now();
  }

  protected void setProjectIntent(ProjectIntent projectIntent) {
    this.projectIntent = projectIntent;
    this.lastModified = Instant.now();
  }

  /**
   * Creates a new project with code and project intent
   *
   * @param projectIntent         the intent of the project
   * @param projectManager        the assigned project manager
   * @param principalInvestigator the principal investigator
   * @param responsiblePerson     the person who should be contacted
   * @return a new project instance
   */
  public static Project create(ProjectIntent projectIntent, ProjectCode projectCode,
      PersonReference projectManager, PersonReference principalInvestigator,
      PersonReference responsiblePerson) {
    return new Project(ProjectId.create(), projectIntent, projectCode, projectManager,
        principalInvestigator, responsiblePerson);
  }

  /**
   * Generates a project with the specified values injected.
   *
   * @param projectId             the identifier of the project
   * @param projectIntent         the project intent
   * @param projectManager        the assigned project manager
   * @param principalInvestigator the principal investigator
   * @param responsiblePerson     the person who should be contacted
   * @return a project with the given identity and project intent
   */
  public static Project of(ProjectId projectId, ProjectIntent projectIntent,
      ProjectCode projectCode, PersonReference projectManager,
      PersonReference principalInvestigator, PersonReference responsiblePerson) {
    return new Project(projectId, projectIntent, projectCode, projectManager,
        principalInvestigator, responsiblePerson);
  }

  public ProjectId getId() {
    return projectId;
  }

  public ProjectIntent getProjectIntent() {
    return projectIntent;
  }

  public ProjectCode getProjectCode() {
    return projectCode;
  }

  public PersonReference getProjectManager() {
    return projectManager;
  }

  public PersonReference getPrincipalInvestigator() {
    return principalInvestigator;
  }

  public PersonReference getResponsiblePerson() {
    return responsiblePerson;
  }

  public Optional<ExperimentId> activeExperiment() {
    return Optional.ofNullable(activeExperiment);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Project project = (Project) o;

    return projectId.equals(project.projectId);
  }

  @Override
  public int hashCode() {
    return projectId.hashCode();
  }
}
