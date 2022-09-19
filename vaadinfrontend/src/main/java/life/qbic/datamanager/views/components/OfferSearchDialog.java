package life.qbic.datamanager.views.components;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.flow.data.renderer.ComponentRenderer;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
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
    public ComboBox<OfferPreview> searchField;
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
            query -> offerLookupService.findOfferContainingProjectTitleOrId(query.getFilter().orElse(""),
                    query.getFilter().orElse(""),query.getOffset(), query.getLimit()).stream());

        searchField.setRenderer(new ComponentRenderer<Text,OfferPreview>(preview -> {
            return new Text(preview.offerId().id() +", "+preview.getProjectTitle().title());
        }));

        searchField.addValueChangeListener(e->{
            System.out.println(e.getValue());
        });
    }

    private void createButtonLayout(){

        cancel = new Button("Cancel");
        ok = new Button("Ok");
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        footerButtonLayout = new HorizontalLayout(cancel,ok);
    }
}
