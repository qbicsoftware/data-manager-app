package life.qbic.datamanager.views.projectOverview;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.components.OfferSearchDialog;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@PageTitle("Project Overview")
@Route(value = "")
public class ProjectOverviewLayout extends DataManagerLayout implements Serializable {

    @Serial
    private static final long serialVersionUID = -4665004581087726748L;

    //todo add vaadin components here eg
    //a grid containing the projects
    //create new button
    //create dialogs:
    //select creation mode dialog
    //search offer dialog
    OfferSearchDialog searchDialog;

    private final OfferLookupService offerLookupService;


    public ProjectOverviewLayout(@Autowired ProjectOverviewHandlerInterface handlerInterface, @Autowired OfferLookupService offerLookupService) {
        Objects.requireNonNull(offerLookupService);
        this.offerLookupService = offerLookupService;

        registerToHandler(handlerInterface);
        createLayoutContent();
        addListeners();
    }

    private void registerToHandler(ProjectOverviewHandlerInterface handler) {
        handler.handle(this);
    }

    private void createLayoutContent() {
        //searchDialog = new SearchDialog();
        //todo open it after offer creation mode was selected
        //todo remove
        searchDialog = new OfferSearchDialog(offerLookupService);
        searchDialog.open();
    }

    private void addListeners(){
        searchDialog.cancel.addClickListener(e -> {
            searchDialog.close();
        });

        searchDialog.ok.addClickListener(e -> {
            //check if value is selected
            if(searchDialog.searchField.getOptionalValue().isPresent()){
                //Selected Offer
                OfferPreview selectedOfferPreview = searchDialog.searchField.getValue();
                //todo forward to service to load into create offer UI
            }
        });
    }

}
