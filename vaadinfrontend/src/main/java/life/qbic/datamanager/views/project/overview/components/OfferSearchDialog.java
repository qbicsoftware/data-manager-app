package life.qbic.datamanager.views.project.overview.components;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import org.slf4j.Logger;


/**
 * <b>Search Dialog for Offers</b>
 *
 * <p>Dialog component for searching offers.
 * In the future this can be extended later so that it can be reused for searching other content than offer previews.</p>
 *
 * @since 1.0.0
 */
public class OfferSearchDialog extends Dialog {

    private static final Logger log = getLogger(OfferSearchDialog.class);

    private HorizontalLayout footerButtonLayout;

    public ComboBox<OfferPreview> searchField;
    public Button cancel;
    public Button ok;

    public OfferSearchDialog() {
        super();

        createButtonLayout();
        styleSearchBox();
        styleDialog();
    }

    private void styleDialog(){
        this.setHeaderTitle("Select an Offer");
        this.add("Select an offer to create a project");

        this.isCloseOnEsc();

        HorizontalLayout contentLayout = new HorizontalLayout(searchField);
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

    private void createButtonLayout(){

        cancel = new Button("Cancel");
        ok = new Button("Ok");
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        footerButtonLayout = new HorizontalLayout(cancel,ok);
    }
}
