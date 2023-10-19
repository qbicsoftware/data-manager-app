package life.qbic.projectmanagement.application;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import life.qbic.application.commons.ApplicationException;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.finances.offer.OfferSearchService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.model.project.OfferIdentifier;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Allows for modification of existing projects
 *
 * @since 1.0.0
 */
@Service
public class ProjectLinkingService {

  private static final Logger log = logger(ProjectLinkingService.class);
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
    log.info("un-linking offer " + offerIdentifier + " from project " + projectIdentifier);
    loadOfferOrThrow(offerIdentifier);
    Project project = loadProjectOrThrow(projectIdentifier);
    project.unlinkOffer(OfferIdentifier.of(offerIdentifier));
    projectRepository.update(project);
  }

  private Offer loadOfferOrThrow(String offerIdentifier) throws ApplicationException {
    Optional<Offer> offerSearchResult = offerSearchService.findByOfferId(offerIdentifier);
    if (offerSearchResult.isEmpty()) {
      throw new ApplicationException(
          "No offer with identifier " + offerIdentifier + " exists.");
    }
    return offerSearchResult.get();
  }

  private Project loadProjectOrThrow(String projectIdentifier) throws ApplicationException {
    ProjectId projectId = ProjectId.of(UUID.fromString(projectIdentifier));
    Optional<Project> projectSearchResult = projectRepository.find(projectId);
    if (projectSearchResult.isEmpty()) {
      throw new ApplicationException(
          "No project with identifier " + projectIdentifier + " exists.");
    }
    return projectSearchResult.get();
  }

  public List<OfferIdentifier> queryLinkedOffers(ProjectId projectId) {
    return projectRepository.find(projectId)
        .map(Project::linkedOffers)
        .orElse(Collections.emptyList());
  }

}
