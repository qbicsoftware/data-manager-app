package life.qbic.datamanager.views.projectOverview;

import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Component
public class ProjectOverviewHandler implements ProjectOverviewHandlerInterface{

    private ProjectOverviewLayout registeredProjectOverview;
    private OfferLookupService offerLookupService;

    ProjectOverviewHandler(@Autowired OfferLookupService offerLookupService){
        this.offerLookupService = offerLookupService;
    }

    @Override
    public void handle(ProjectOverviewLayout layout) {
        if (registeredProjectOverview != layout) {
            this.registeredProjectOverview = layout;
            addClickListeners();
        }
    }

    private void addClickListeners() {

        //List<String> mapped = searchResult.stream().map(offerPreview -> offerPreview.offerId() + ", " +  offerPreview.getProjectTitle()).collect(Collectors.toList());
        //List<OfferPreview> searchResult = offerLookupService.findOfferContainingProjectTitleOrId(searchValue,searchValue);

    }

}
