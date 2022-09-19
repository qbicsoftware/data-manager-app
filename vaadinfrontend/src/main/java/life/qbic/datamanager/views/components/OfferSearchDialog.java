package life.qbic.datamanager.views.components;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.Objects;
import java.util.stream.Stream;

import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import org.slf4j.Logger;


/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since 1.0.0
 */
public class OfferSearchDialog extends Dialog {

    private static final Logger log = getLogger(OfferSearchDialog.class);

    private final OfferLookupService offerLookupService;
    public ComboBox<String> searchField;
    private HorizontalLayout footerButtonLayout;
    private HorizontalLayout contentLayout;
    public Button cancel;
    public Button ok;

    public OfferSearchDialog(OfferLookupService offerLookupService) {
        super();
        Objects.requireNonNull(offerLookupService);
        this.offerLookupService = offerLookupService;

        createButtonLayout();
        styleSearchBox();
        styleDialog();
        setItems();
    }

    private void styleDialog(){
        this.setHeaderTitle("Select an Offer");
        this.add("Select an offer to create a project");

        this.isCloseOnEsc();

        contentLayout = new HorizontalLayout(searchField);
        this.add(contentLayout);

        this.getFooter().add(footerButtonLayout);
    }

    private void styleSearchBox() {
        searchField = new ComboBox<>();
        searchField.setPlaceholder("Search");
        searchField.setClassName("searchbox");
        searchField.addClassNames("flex",
                "flex-col",
                "w-full",
                "min-width-300px",
                "max-width-15vw");
    }

    private void setItems() {
        searchField.setItems(
            query -> find(query.getFilter().orElse(""),query.getOffset(), query.getLimit()));
    }

    private void createButtonLayout(){

        cancel = new Button("Cancel");
        ok = new Button("Ok");
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        footerButtonLayout = new HorizontalLayout(cancel,ok);
    }
    private Stream<String> find(String filter, int offset, int limit) {
        return offerLookupService.findOfferContainingProjectTitleOrId(filter, filter)
                .stream()
                .map(preview -> preview.offerId().id() +", "+preview.getProjectTitle().title())
                .sorted()
                .skip(offset)
                .limit(limit);
    }
}
