package life.qbic.datamanager.views.components;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import life.qbic.finance.persistence.SimpleOfferSearchService;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.application.finances.offer.OfferSearchService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since 1.0.0
 */
public class SearchDialog extends Dialog {

    private static final Logger log = getLogger(SearchDialog.class);

    private OfferLookupService offerLookupService;
    public ComboBox<String> searchField;
    private HorizontalLayout footerButtonLayout;
    private HorizontalLayout contentLayout;
    public Button cancel;
    public Button ok;

    public SearchDialog(OfferLookupService offerLookupService) {
        super();
        this.offerLookupService = offerLookupService;
        styleSearchBox();
        createButtonLayout();
        test();
        listener();
    }

    private void test(){
        this.setHeaderTitle("Select an Offer");

        this.isCloseOnEsc();

        this.add(contentLayout);
        this.getFooter().add(footerButtonLayout);
    }

    private void styleSearchBox() {
        searchField = new ComboBox<>();
        searchField.setPlaceholder("Search");
        searchField.setClassName("searchbox");

        searchField.setItems(
            query -> offerLookupService.findOfferContainingProjectTitleOrId(query.getFilter().orElse(""),
                            query.getFilter().orElse(""))
                    .stream()
                    .map(preview -> preview.offerId().id() +", "+preview.getProjectTitle().title()));
        contentLayout = new HorizontalLayout(searchField);

    }

    private void createButtonLayout(){

        cancel = new Button("Cancel");
        ok = new Button("Ok");
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        footerButtonLayout = new HorizontalLayout(cancel,ok);
    }

    private void listener(){
        searchField.addValueChangeListener( e -> {
            log.info("blub");
            System.out.println("here");
        });
    }

    @Service
    private static class Repository {

        private final List<String> dummy;
        @Autowired
        private OfferLookupService offerSearchService;

        public Repository() {
            dummy = new ArrayList<>();
            dummy.add("Hello");
            dummy.add("xyz");
            dummy.add("alkjdflasjdfklsdf");
            dummy.add("asldfjasldfjiowaefnafnasdfawsfwae fas asdfasefawerf");


        }

        public Stream<String> find(String filter, int offset, int limit) {
            /*return dummy.stream()
                .filter(it -> it.toUpperCase().contains(filter.toUpperCase()))
                .sorted()
                .skip(offset)
                .limit(limit);*/
            return offerSearchService.findOfferContainingProjectTitleOrId(filter, filter)
                    .stream()
                    .map(preview -> preview.offerId().id() +", "+preview.getProjectTitle().title())
                    .sorted()
                    .skip(offset)
                    .limit(limit);
        }
    }


}
