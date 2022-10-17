package life.qbic.projectmanagement.application;

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

  private final ProjectRepository projectRepository;
  private final OfferSearchService offerSearchService;

  public ProjectModificationService(
      @Autowired ProjectRepository projectRepository,
      @Autowired OfferSearchService offerSearchService) {
    this.projectRepository = projectRepository;
    this.offerSearchService = offerSearchService;
  }

  public void linkOfferToProject(String offerIdentifier, String projectIdentifier) {
    /*
     TODO:
     - offer := query offer / verify that offer exists
     - project := query project
     - project.linkOffer(offer.id())
     - projectRepository.save(project)
     */
  }

  public void unlinkOfferFromProject(String offerIdentifier, String projectIdentifier) {
        /*
     TODO:
     - offer := query offer / verify that offer exists
     - project := query project
     - project.unlinkOffer(offer.id())
     - projectRepository.save(project)
     */
  }

}
