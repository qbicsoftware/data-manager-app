package life.qbic.projectmanagement.application;

import static life.qbic.logging.service.LoggerFactory.logger;

import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.finances.offer.OfferSearchService;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Allows for modification of existing projects
 *
 * @since 1.0.0
 */
@Service
public class ProjectModificationService {

  Logger log = logger(ProjectModificationService.class);
  private final ProjectRepository projectRepository;
  private final OfferSearchService offerSearchService;

  public ProjectModificationService(
      @Autowired ProjectRepository projectRepository,
      @Autowired OfferSearchService offerSearchService) {
    this.projectRepository = projectRepository;
    this.offerSearchService = offerSearchService;
  }

  public void linkOfferToProject(String offerIdentifier, String projectIdentifier) {
    log.info("linking offer " + offerIdentifier + " to project " + projectIdentifier);
    /*
     TODO:
     - offer := query offer / verify that offer exists
     - project := query project
     - project.linkOffer(offer.id())
     - projectRepository.save(project)
     */
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
  }

}
