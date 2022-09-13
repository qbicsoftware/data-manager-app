package life.qbic.projectmanagement.finances.offer.api;

import java.util.List;
import life.qbic.projectmanagement.finances.offer.Offer;
import life.qbic.projectmanagement.finances.offer.OfferId;
import life.qbic.projectmanagement.finances.offer.ProjectTitle;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface OfferSearchService {

  List<Offer> findByProjectTitleOrOfferId(ProjectTitle projectTitle, OfferId offerId);

  List<Offer> findAll();

}
