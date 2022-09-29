package life.qbic.datamanager.views.projectOverview.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since 1.0.0
 */
public class CreationModeDialog extends Dialog {

    private HorizontalLayout contentLayout;

    public Button blankButton;
    public Button fromOfferButton;
    public Button cancel;
    public Button next;

    public CreationModeDialog(){
        setupContent();
        styleDialog();
    }

    private void createDialogControlButtons(){
        next = new Button("Next");
        next.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        cancel = new Button("Cancel");

        setupButtonLayout();
        isCloseOnEsc();
    }

    private void setupButtonLayout(){
        this.getFooter().add(new HorizontalLayout(cancel, next));
    }

    private void setupContent(){
        createCreationModeButtons();
        Label blankLabel = new Label("Blank");
        blankLabel.addClassName("dialogue-button-label");
        VerticalLayout blankLayout = new VerticalLayout(blankButton, blankLabel);
        blankLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Label fromOfferLabel = new Label("From Offer");
        fromOfferLabel.addClassName("dialogue-button-label");
        VerticalLayout offerLayout = new VerticalLayout(fromOfferButton, fromOfferLabel);
        offerLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        contentLayout = new HorizontalLayout(blankLayout,offerLayout);
        contentLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        contentLayout.setPadding(true);
    }

    private void createCreationModeButtons() {
        blankButton = new Button("", new Icon(VaadinIcon.PLUS_CIRCLE_O));
        blankButton.addClassName("dialogue-button");
        setSecondaryButtonStyle(blankButton);


        fromOfferButton = new Button("", new Icon(VaadinIcon.FILE_TEXT_O));
        fromOfferButton.addClassName("dialogue-button");
        setContrastButtonStyle(fromOfferButton);

        listener();
    }

    private void listener(){
        blankButton.addClickListener(e -> {
            setSelectedStyle(blankButton);
            setContrastButtonStyle(fromOfferButton);
        });

        fromOfferButton.addClickListener(e -> {
            setSelectedStyle(fromOfferButton);
            setSecondaryButtonStyle(blankButton);
        });
    }

    private void styleDialog(){
        this.setHeaderTitle("Create a new Project");
        this.add("Create a project from scratch or use an offer as template");
        this.add(contentLayout);

        createDialogControlButtons();
    }

    private void setSelectedStyle(Button button){
        button.setClassName("selected");
        button.addClassName("dialogue-button");
    }

    private void setSecondaryButtonStyle(Button button){
        button.setClassName("secondary");
        button.addClassName("dialogue-button");
    }

    private void setContrastButtonStyle(Button button){
        button.setClassName("contrast");
        button.addClassName("dialogue-button");
    }

    /**
     * Rests the content of the dialog. This means specifically to reset the styles of the selected buttons.
     */
    public void reset(){
        setSecondaryButtonStyle(blankButton);
        setContrastButtonStyle(fromOfferButton);
    }


}
