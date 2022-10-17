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
    Optional<Offer> offerSearchResult = offerSearchService.findByOfferId(offerIdentifier);
    if (offerSearchResult.isEmpty()) {
      throw new ProjectManagementException(
          "No offer with identifier " + offerIdentifier + " exists.");
    }
    Offer offer = offerSearchResult.get();
    ProjectId projectId = ProjectId.of(UUID.fromString(projectIdentifier));
    Optional<Project> projectSearchResult = projectRepository.find(projectId);
    if (projectSearchResult.isEmpty()) {
      throw new ProjectManagementException(
          "No project with identifier " + projectIdentifier + " exists.");
    }
    Project project = projectSearchResult.get();
    OfferIdentifier offerIdentifier2 = OfferIdentifier.of(offer.offerId().id());
    project.linkOffer(offerIdentifier2);
    projectRepository.update(project);
  }

  public void unlinkOfferFromProject(String offerIdentifier, String projectIdentifier) {
    log.info("un-linking offer " + offerIdentifier + " to project " + projectIdentifier);
    /*
     TODO:
     - offer := query offer / verify that offer exists
     - project := query project
     - project.unlinkOffer(offer.id())
     - projectRepository.save(project)
     */
    Optional<Offer> offerSearchResult = offerSearchService.findByOfferId(offerIdentifier);
    if (offerSearchResult.isEmpty()) {
      throw new ProjectManagementException(
          "No offer with identifier " + offerIdentifier + " exists.");
    }
    Offer offer = offerSearchResult.get();
    ProjectId projectId = ProjectId.of(UUID.fromString(projectIdentifier));
    Optional<Project> projectSearchResult = projectRepository.find(projectId);
    if (projectSearchResult.isEmpty()) {
      throw new ProjectManagementException(
          "No project with identifier " + projectIdentifier + " exists.");
    }
    Project project = projectSearchResult.get();
    OfferIdentifier offerIdentifier2 = OfferIdentifier.of(offer.offerId().id());
    project.unlinkOffer(offerIdentifier2);
    projectRepository.update(project);
  }

}
