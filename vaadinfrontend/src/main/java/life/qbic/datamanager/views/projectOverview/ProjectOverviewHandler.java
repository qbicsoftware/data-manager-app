package life.qbic.datamanager.views.projectOverview;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * <b>Handler</b>
 *
 * <p>Orchestrates the layout {@link ProjectOverviewLayout} and determines how the components behave.</p>
 *
 * @since <version tag>
 */
@Component
public class ProjectOverviewHandler implements ProjectOverviewHandlerInterface{

    private ProjectOverviewLayout registeredProjectOverview;
    private final OfferLookupService offerLookupService;

    ProjectOverviewHandler(@Autowired OfferLookupService offerLookupService){
        Objects.requireNonNull(offerLookupService);
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
        registeredProjectOverview.searchDialog.cancel.addClickListener(e -> registeredProjectOverview.searchDialog.close());

        registeredProjectOverview.create.addClickListener( e-> {
            loadItemsWithService(offerLookupService);
            registeredProjectOverview.searchDialog.open();
        });

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

    private void loadItemsWithService(OfferLookupService service) {
        registeredProjectOverview.searchDialog.searchField.setItems(
                query -> service.findOfferContainingProjectTitleOrId(query.getFilter().orElse(""),
                        query.getFilter().orElse(""),query.getOffset(), query.getLimit()).stream());

        registeredProjectOverview.searchDialog.searchField.setRenderer(new ComponentRenderer<Text, OfferPreview>(preview -> {
            return new Text(preview.offerId().id() +", "+preview.getProjectTitle().title());
        }));
    }

}
