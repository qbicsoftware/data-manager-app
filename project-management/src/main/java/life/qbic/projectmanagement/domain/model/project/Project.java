package life.qbic.projectmanagement.domain.model.project;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.translation.OfferIdentifierConverter;
import org.springframework.lang.Nullable;

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

  @Version
  private int version;

  @Embedded
  private ProjectIntent projectIntent;

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL)
  @JoinColumn(name = "project")
  private List<Experiment> experiments = new ArrayList<>();

  @Embedded
  @Column(name = "projectCode", nullable = false)
  private ProjectCode projectCode;

  @Column(name = "lastModified", nullable = false)
  private Instant lastModified;

  @ElementCollection
  @Convert(converter = OfferIdentifierConverter.class)
  @CollectionTable(name = "projects_offers", joinColumns = @JoinColumn(name = "projectIdentifier"))
  @Column(name = "offerIdentifier")
  private List<OfferIdentifier> linkedOffers = new ArrayList<>();

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "fullName", column = @Column(name = "projectManagerFullName")),
      @AttributeOverride(name = "emailAddress", column = @Column(name = "projectManagerEmailAddress")),
      @AttributeOverride(name = "oidc", column = @Column(name = "projectManagerOidc")),
      @AttributeOverride(name = "oidcIssuer", column = @Column(name = "projectManagerOidcIssuer"))
  })
  private Contact projectManager;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "fullName", column = @Column(name = "principalInvestigatorFullName")),
      @AttributeOverride(name = "emailAddress", column = @Column(name = "principalInvestigatorEmailAddress")),
      @AttributeOverride(name = "oidc", column = @Column(name = "principalInvestigatorOidc")),
      @AttributeOverride(name = "oidcIssuer", column = @Column(name = "principalInvestigatorOidcIssuer"))
  })
  private Contact principalInvestigator;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "fullName", column = @Column(name = "responsibePersonFullName")),
      @AttributeOverride(name = "emailAddress", column = @Column(name = "responsibePersonEmailAddress")),
      @AttributeOverride(name = "oidc", column = @Column(name = "responsiblePersonOidc")),
      @AttributeOverride(name = "oidcIssuer", column = @Column(name = "responsiblePersonOidcIssuer"))
  })
  private Contact responsiblePerson;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "grant", column = @Column(name = "grantLabel")),
      @AttributeOverride(name = "grantId", column = @Column(name = "grantId"))
  })
  private Funding funding;

  private Project(ProjectId projectId, ProjectIntent projectIntent, ProjectCode projectCode,
      Contact projectManager, Contact principalInvestigator,
      @Nullable Contact responsiblePerson) {
    requireNonNull(principalInvestigator, "requires non-null principal investigator");
    requireNonNull(projectCode, "requires non-null project code");
    requireNonNull(projectId, "requires non-null project id");
    requireNonNull(projectIntent, "requires non-null project intent");
    requireNonNull(projectManager, "requires non-null project manager");
    setPrincipalInvestigator(principalInvestigator);
    setProjectCode(projectCode);
    setProjectId(projectId);
    setProjectIntent(projectIntent);
    setProjectManager(projectManager);
    Optional.ofNullable(responsiblePerson).ifPresent(this::setResponsiblePerson);
  }

  @PostLoad
  private void loadCollections() {
    int size = experiments.size();
    int offersSize = linkedOffers.size();
  }

  public void setProjectManager(Contact projectManager) {
    Objects.requireNonNull(projectManager);
    if (projectManager.equals(this.projectManager)) {
      return;
    }
    this.projectManager = projectManager;
    this.lastModified = Instant.now();
  }

  public void setPrincipalInvestigator(Contact principalInvestigator) {
    Objects.requireNonNull(principalInvestigator);
    if (principalInvestigator.equals(this.principalInvestigator)) {
      return;
    }
    this.principalInvestigator = principalInvestigator;
    this.lastModified = Instant.now();
  }

  public void setResponsiblePerson(Contact responsiblePerson) {
    if (Objects.isNull(this.responsiblePerson) && Objects.isNull(responsiblePerson)) {
      return;
    } else if (Objects.nonNull(this.responsiblePerson)) {
      if (this.responsiblePerson.equals(responsiblePerson)) {
        return;
      }
    }
    this.responsiblePerson = responsiblePerson;
    this.lastModified = Instant.now();
  }

  public void setFunding(Funding funding) {
    requireNonNull(funding);
    this.funding = funding;
    this.lastModified = Instant.now();
  }

  private void setProjectCode(ProjectCode projectCode) {
    this.projectCode = projectCode;
  }

  protected Project() {
  }

  public void updateTitle(ProjectTitle title) {
    if (projectIntent.projectTitle().equals(title)) {
      return;
    }
    projectIntent.projectTitle(title);
    lastModified = Instant.now();
  }

  public void setLastModified(Instant modifiedOn) {
    lastModified = modifiedOn;
  }

  public void addExperiment(Experiment experiment) {
    experiments.add(experiment);
    lastModified = Instant.now();
  }

  public void removeResponsiblePerson() {
    responsiblePerson = null;
  }

  public void stateObjective(ProjectObjective projectObjective) {
    Objects.requireNonNull(projectObjective);
    if (projectObjective.equals(projectIntent.objective())) {
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
    Objects.requireNonNull(projectId);
    if (projectId.equals(this.projectId)) {
      return;
    }
    this.projectId = projectId;
    this.lastModified = Instant.now();
  }

  protected void setProjectIntent(ProjectIntent projectIntent) {
    Objects.requireNonNull(projectIntent);
    if (projectIntent.equals(this.projectIntent)) {
      return;
    }
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
      Contact projectManager, Contact principalInvestigator,
      @Nullable Contact responsiblePerson) {
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
      ProjectCode projectCode, Contact projectManager,
      Contact principalInvestigator, @Nullable Contact responsiblePerson) {
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

  public Contact getProjectManager() {
    return projectManager;
  }

  public Contact getPrincipalInvestigator() {
    return principalInvestigator;
  }

  public Optional<Contact> getResponsiblePerson() {
    return Optional.ofNullable(responsiblePerson);
  }

  public Optional<Funding> funding() {
    return Optional.ofNullable(funding);
  }

  public List<ExperimentId> experiments() {
    return experiments.stream().map(Experiment::experimentId).toList();
  }

  public Instant getLastModified() {
    return lastModified;
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

  public void removeFunding() {
    funding = null;
  }
}
