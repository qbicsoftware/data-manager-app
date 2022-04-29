package life.qbic.views;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;


/**
 * <b> An ErrorMessage component which shows an error message with a title and a detailed description. </b>
 *
 * @since 1.0.0
 */
@CssImport("./styles/views/login/login-view.css")
public class ErrorMessage extends Composite<VerticalLayout> {

    private Span icon;
    private Span title;
    public Span titleText;

    private Div description;
    public Span descriptionText;

    public ErrorMessage(String titleText, String descriptionText){
        createTitle(titleText);
        createDescriptionText(descriptionText);

        initLayout();
    }

    private void initLayout(){
        this.getContent().add(title,description);
        this.getContent().addClassName("error-10pct");

        this.getContent().getStyle().set("padding", "var(--lumo-space-xs");
    }

    private void createDescriptionText(String descriptionText) {
        this.descriptionText = new Span(descriptionText);

        description = new Div(this.descriptionText);
        description.getStyle().set("padding", "var(--lumo-space-m");
    }

    private void createTitle(String titleText){
        icon = new Span(new Icon(VaadinIcon.EXCLAMATION_CIRCLE_O));
        icon.getStyle().set("padding", "var(--lumo-space-xs");

        this.titleText = new Span(titleText);

        title = new Span(icon, this.titleText);
    }

}
