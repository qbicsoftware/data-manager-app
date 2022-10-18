package life.qbic.projectmanagement.application;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Optional;
import java.util.UUID;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.finances.offer.OfferSearchService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.project.OfferIdentifier;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Allows for modification of existing projects
 *
 * @since 1.0.0
 */
@Service
public class ProjectLinkingService {

  Logger log = logger(ProjectLinkingService.class);
  private final ProjectRepository projectRepository;
  private final OfferSearchService offerSearchService;

  public ProjectLinkingService(
      @Autowired ProjectRepository projectRepository,
      @Autowired OfferSearchService offerSearchService) {
    this.projectRepository = projectRepository;
    this.offerSearchService = offerSearchService;
  }

  public void linkOfferToProject(String offerIdentifier, String projectIdentifier) {
    log.info("linking offer " + offerIdentifier + " to project " + projectIdentifier);
    loadOfferOrThrow(offerIdentifier);
    Project project = loadProjectOrThrow(projectIdentifier);
    project.linkOffer(OfferIdentifier.of(offerIdentifier));
    projectRepository.update(project);
  }

  public void unlinkOfferFromProject(String offerIdentifier, String projectIdentifier) {
    log.info("un-linking offer " + offerIdentifier + " to project " + projectIdentifier);
    loadOfferOrThrow(offerIdentifier);
    Project project = loadProjectOrThrow(projectIdentifier);
    project.unlinkOffer(OfferIdentifier.of(offerIdentifier));
    projectRepository.update(project);
  }

  private Offer loadOfferOrThrow(String offerIdentifier) throws ProjectManagementException {
    Optional<Offer> offerSearchResult = offerSearchService.findByOfferId(offerIdentifier);
    if (offerSearchResult.isEmpty()) {
      throw new ProjectManagementException(
          "No offer with identifier " + offerIdentifier + " exists.");
    }
    return offerSearchResult.get();
  }

  private Project loadProjectOrThrow(String projectIdentifier) throws ProjectManagementException {
    ProjectId projectId = ProjectId.of(UUID.fromString(projectIdentifier));
    Optional<Project> projectSearchResult = projectRepository.find(projectId);
    if (projectSearchResult.isEmpty()) {
      throw new ProjectManagementException(
          "No project with identifier " + projectIdentifier + " exists.");
    }
    return projectSearchResult.get();
  }

}
