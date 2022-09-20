package life.qbic.datamanager.views.projectOverview;

import com.vaadin.flow.component.ItemLabelGenerator;
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
 * @since 1.0.0
 */
@Component
public class ProjectOverviewHandler implements ProjectOverviewHandlerInterface{

    private ProjectOverviewLayout registeredProjectOverview;
    private final OfferLookupService offerLookupService;

    private CreationMode creationMode = CreationMode.NONE;

    public ProjectOverviewHandler(@Autowired OfferLookupService offerLookupService){
        Objects.requireNonNull(offerLookupService);
        this.offerLookupService = offerLookupService;
    }

    @Override
    public void handle(ProjectOverviewLayout layout) {
        if (registeredProjectOverview != layout) {
            this.registeredProjectOverview = layout;
            configureSearchDropbox();
            configureSelectionModeDialog();
        }
    }

    private void configureSelectionModeDialog(){
        registeredProjectOverview.selectCreationModeDialog.blankButton.addClickListener(e -> creationMode = CreationMode.BLANK);
        registeredProjectOverview.selectCreationModeDialog.fromOfferButton.addClickListener(e -> creationMode = CreationMode.FROM_OFFER);

        registeredProjectOverview.selectCreationModeDialog.next.addClickListener(e ->{
            switch (creationMode){
                case BLANK -> {
                    //todo link the route to the project ui
                    return;
                }
                case FROM_OFFER -> {
                    registeredProjectOverview.selectCreationModeDialog.close();
                    loadItemsWithService(offerLookupService);
                    registeredProjectOverview.searchDialog.open();
                }
            }
        });
        registeredProjectOverview.selectCreationModeDialog.cancel.addClickListener(e -> registeredProjectOverview.selectCreationModeDialog.close());
    }

    private void configureSearchDropbox(){
        configureDialogButtons();

        registeredProjectOverview.searchDialog.ok.addClickListener(e -> {
            //check if value is selected
            if(registeredProjectOverview.searchDialog.searchField.getOptionalValue().isPresent()){
                forwardSelectedOffer();
            }
        });
    }
    private void configureDialogButtons() {
        registeredProjectOverview.searchDialog.cancel.addClickListener(e -> registeredProjectOverview.searchDialog.close());

        registeredProjectOverview.create.addClickListener( e-> registeredProjectOverview.selectCreationModeDialog.open());

    }

    private void forwardSelectedOffer() {
        //todo forward to service to load into create offer UI
        OfferPreview selectedOfferPreview = registeredProjectOverview.searchDialog.searchField.getValue();
        registeredProjectOverview.add(new Text(selectedOfferPreview.offerId().id()+", "+selectedOfferPreview.getProjectTitle().title()));
        registeredProjectOverview.searchDialog.close();
    }

    private void loadItemsWithService(OfferLookupService service) {
        registeredProjectOverview.searchDialog.searchField.setItems(
                query -> service.findOfferContainingProjectTitleOrId(query.getFilter().orElse(""),
                        query.getFilter().orElse(""),query.getOffset(), query.getLimit()).stream());

        registeredProjectOverview.searchDialog.searchField.setRenderer(new ComponentRenderer<>(preview ->
                new Text(preview.offerId().id() + ", " + preview.getProjectTitle().title())));

        registeredProjectOverview.searchDialog.searchField.setItemLabelGenerator((ItemLabelGenerator<OfferPreview>) preview ->
                preview.offerId().id() +", "+preview.getProjectTitle().title());
    }

    enum CreationMode{
        BLANK, FROM_OFFER, NONE
    }

}
