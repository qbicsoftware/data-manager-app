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
import org.slf4j.Logger;


/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since 1.0.0
 */
public class SearchDialog extends Dialog {

    private static final Logger log = getLogger(SearchDialog.class);


    public ComboBox<String> searchField;
    private HorizontalLayout footerButtonLayout;
    private HorizontalLayout contentLayout;
    public Button cancel;
    public Button ok;

    public SearchDialog() {
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

    private void styleSearchBox() {
        searchField = new ComboBox<>();
        searchField.setPlaceholder("Search");
        searchField.setClassName("searchbox");
        var repo = new Repository();
        searchField.setItems(
            query -> repo.find(query.getFilter().orElse(""), query.getOffset(), query.getLimit()));
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
            log.info("blub");
            filter(e.getValue());
            System.out.println("here");
        });

    }


    private void filter(String filterString) {
        System.out.println("Called this method");
        System.out.println(filterString);
    }

    private static class Repository {

        private final List<String> dummy;

        public Repository() {
            dummy = new ArrayList<>();
            dummy.add("Hello");
            dummy.add("xyz");
            dummy.add("alkjdflasjdfklsdf");
            dummy.add("asldfjasldfjiowaefnafnasdfawsfwae fas asdfasefawerf");
        }

        public Stream<String> find(String filter, int offset, int limit) {
            return dummy.stream()
                .filter(it -> it.toUpperCase().contains(filter.toUpperCase()))
                .sorted()
                .skip(offset)
                .limit(limit);
        }
    }


}
