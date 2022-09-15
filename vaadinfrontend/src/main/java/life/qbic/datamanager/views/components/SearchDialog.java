package life.qbic.datamanager.views.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.combobox.ComboBox;


/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since 1.0.0
 */
public class SearchDialog extends Dialog {

    ComboBox<String> searchField;
    private HorizontalLayout footerButtonLayout;
    private HorizontalLayout contentLayout;
    Button cancel;
    Button ok;

    public SearchDialog(){
        super();
        styleSearchBox();
        createButtonLayout();
        test();
    }

    private void test(){
        this.setHeaderTitle("Select an Offer");

        this.isCloseOnEsc();

        this.add(contentLayout);
        this.getFooter().add(footerButtonLayout);
    }

    private void styleSearchBox(){
        searchField = new ComboBox<>();
        searchField.setPlaceholder("Search");
        //todo replace icon
        searchField.getElement().getStyle().set("--lumo-icons-dropdown", "lumo:search");
        //.getElement().getStyle().set("--lumo-icons-dropdown", "\"\\ea23\"");
        //(VaadinIcon.SEARCH.create());

        contentLayout = new HorizontalLayout(searchField);
    }

    private void createButtonLayout(){

        cancel = new Button("Cancel");
        ok = new Button("Ok");
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        footerButtonLayout = new HorizontalLayout(cancel,ok);
    }

    public void setDataProvider(){
        //todo change data provider based on feedback send after search
        searchField.getDataProvider();

    }





}
