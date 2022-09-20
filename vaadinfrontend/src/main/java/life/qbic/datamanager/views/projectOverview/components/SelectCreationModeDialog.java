package life.qbic.datamanager.views.projectOverview.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
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
public class SelectCreationModeDialog extends Dialog {

    private HorizontalLayout contentLayout;

    public Button blankButton;
    public Button fromOfferButton;
    public Button cancel;
    public Button next;

    public SelectCreationModeDialog(){
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
        VerticalLayout blankLayout = new VerticalLayout(blankButton);
        blankLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        blankLayout.setSizeFull();

        VerticalLayout offerLayout = new VerticalLayout(fromOfferButton);
        offerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        offerLayout.setSizeFull();

        contentLayout = new HorizontalLayout(blankLayout,offerLayout);
        contentLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        contentLayout.setSizeFull();

        contentLayout.setPadding(true);
    }

    private void createCreationModeButtons() {
        blankButton = new Button("", new Icon(VaadinIcon.PLUS_CIRCLE_O));
        blankButton.setSizeFull();
        setSecondaryButtonStyle(blankButton);


        fromOfferButton = new Button("", new Icon(VaadinIcon.FILE));
        fromOfferButton.setSizeFull();
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
    }

    private void setSecondaryButtonStyle(Button button){
        button.setClassName("secondary");
    }

    private void setContrastButtonStyle(Button button){
        button.setClassName("contrast");
    }


}
