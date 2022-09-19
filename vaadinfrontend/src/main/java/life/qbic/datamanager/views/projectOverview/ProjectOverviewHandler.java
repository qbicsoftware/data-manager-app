package life.qbic.datamanager.views.projectOverview;

import com.vaadin.flow.component.Text;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
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

    ProjectOverviewHandler(){
    }

    @Override
    public void handle(ProjectOverviewLayout layout) {
        if (registeredProjectOverview != layout) {
            this.registeredProjectOverview = layout;
            addClickListeners();
        }
    }

    private void addClickListeners() {
        registeredProjectOverview.searchDialog.cancel.addClickListener(e -> registeredProjectOverview.searchDialog.close());

        registeredProjectOverview.create.addClickListener( e-> registeredProjectOverview.searchDialog.open());

        registeredProjectOverview.searchDialog.ok.addClickListener(e -> {
                //check if value is selected
            if(registeredProjectOverview.searchDialog.searchField.getOptionalValue().isPresent()){
                //Selected Offer
                OfferPreview selectedOfferPreview = registeredProjectOverview.searchDialog.searchField.getValue();
                registeredProjectOverview.add(new Text(selectedOfferPreview.offerId()+", "+selectedOfferPreview.getProjectTitle()));
                registeredProjectOverview.searchDialog.close();
                //todo forward to service to load into create offer UI
            }
        });
    }

}
