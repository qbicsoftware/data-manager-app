package life.qbic.projectmanagement.domain.project;

import java.util.Optional;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.repository.jpa.OfferIdentifierConverter;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

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

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "project", fetch = FetchType.LAZY, orphanRemoval = true)
  // "project" being the colum in the experiments table
  private List<Experiment> experiments;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private Experiment activeExperiment;

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

  public void linkExperiment(Experiment experiment) {
    experiments.add(experiment);
    activeExperiment = experiment;
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

  public Optional<Experiment> activeExperiment() {
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
