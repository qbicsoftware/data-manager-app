package life.qbic.datamanager.views.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.combobox.ComboBox;

import java.util.ArrayList;
import java.util.List;


/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since 1.0.0
 */
public class SearchDialog extends Dialog {

    public ComboBox<String> searchField;
    private HorizontalLayout footerButtonLayout;
    private HorizontalLayout contentLayout;
    public Button cancel;
    public Button ok;

    public SearchDialog(){
        super();
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

    private void styleSearchBox(){
        searchField = new ComboBox<>();
        searchField.setPlaceholder("Search");
        searchField.setClassName("searchbox");
        contentLayout = new HorizontalLayout(searchField);
    }

    private void createButtonLayout(){

        cancel = new Button("Cancel");
        ok = new Button("Ok");
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        footerButtonLayout = new HorizontalLayout(cancel,ok);
    }

    private void listener(){
        //causes error:
        // "java: interface JsonArray is public, should be declared in a file named JsonArray.java"
        searchField.addValueChangeListener( e -> {
            filter(e.getValue());
            System.out.println("here");
        });

    }


    private void filter(String filterString) {
        System.out.println("Called this method");
        System.out.println(filterString);
        List<String> dummy = new ArrayList<>();
        dummy.add("Hello");
        dummy.add("xyz");
        dummy.add("alkjdflasjdfklsdf");
        dummy.add("asldfjasldfjiowaefnafnasdfawsfwae fas asdfasefawerf");

        String likeFilter = "%" + filterString + "%";

        searchField.setItems(q -> dummy.stream().filter(it -> it.contains(likeFilter)));
    }
}
