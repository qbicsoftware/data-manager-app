package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

/**
 * <b>Create Project Component</b>
 *
 * <p>Component to create a project based on a project intent</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class ProjectInformationDialog extends Dialog {

    @Serial
    private static final long serialVersionUID = 6132538769263078943L;
    private final Handler handler;
    public ComboBox<OfferPreview> offerSearchField;
    private HorizontalLayout codeAndTitleLayout;
    private final TextField codeField;
    private final Button generateCodeButton;
    private final TextField titleField;
    private final Button createButton;
    private final Button cancelButton;
    private final FormLayout formLayout;
    private final VerticalLayout experimentalDesignLayout;
    private final TextArea experimentalDesignField;
    private final TextArea projectObjective;
    private final VerticalLayout projectContactsLayout;
    public final ComboBox<PersonReference> principalInvestigator;
    private final HorizontalLayout sampleCountLayout;
    public final MultiSelectComboBox<Species> speciesBox;
    public final MultiSelectComboBox<Specimen> specimenBox;
    public final MultiSelectComboBox<Analyte> analyteBox;
    public final ComboBox<PersonReference> responsiblePerson;
    public final ComboBox<PersonReference> projectManager;

    public ProjectInformationDialog() {
        offerSearchField = new ComboBox<>("Offer");
        formLayout = new FormLayout();

        codeField = new TextField("Code");
        codeField.setRequired(true);
        codeField.setHelperText("Q and 4 letters/numbers");
        defaultProjectCodeCreation();

        generateCodeButton = new Button(new Icon(VaadinIcon.REFRESH));
        generateCodeButton.addThemeVariants(ButtonVariant.LUMO_ICON);
        generateCodeButton.getElement().setAttribute("aria-label", "Generate Code");

        titleField = new TextField("Title");
        titleField.setRequired(true);

        projectObjective = new TextArea("Objective");
        projectObjective.setRequired(true);

        //ToDo Remove Field once experimental design backend is connected
        experimentalDesignField = new TextArea("Experimental Design Description");
        experimentalDesignLayout = new VerticalLayout();
        initExperimentalDesignLayout();

        //Layout with max width to keep the SampleCountField in a separate row
        sampleCountLayout = new HorizontalLayout();
        speciesBox = new MultiSelectComboBox<>("Species");
        speciesBox.setRequired(true);
        specimenBox = new MultiSelectComboBox<>("Specimen");
        specimenBox.setRequired(true);
        analyteBox = new MultiSelectComboBox<>("Analyte");
        analyteBox.setRequired(true);

        projectContactsLayout = new VerticalLayout();
        initProjectContactsLayout();

        principalInvestigator = new ComboBox<>("Principal Investigator");
        principalInvestigator.setPlaceholder("Select a principal investigator");
        principalInvestigator.setRequired(true);

        responsiblePerson = new ComboBox<>("Project Responsible (optional)");
        responsiblePerson.setPlaceholder("Select Project Responsible");
        responsiblePerson.setHelperText("Should be contacted about project related questions");
        //Workaround since combobox does not allow empty selection https://github.com/vaadin/flow-components/issues/1998
        responsiblePerson.setClearButtonVisible(true);

        projectManager = new ComboBox<>("Project Manager");
        projectManager.setPlaceholder("Select a project manager");
        projectManager.setRequired(true);

        createButton = new Button("Create");
        cancelButton = new Button("Cancel");
        configureDialogLayout();
        initForm();
        styleForm();
        handler = new Handler();
        handler.handle();
    }

    public void addProjectCreationEventListener(ComponentEventListener<ProjectCreationEvent> listener) {
        handler.addProjectCreationEventListener(listener);
    }

    public void addCancelEventListener(ComponentEventListener<UserCancelEvent<ProjectInformationDialog>> listener) {
        handler.addUserCancelEventListener(listener);
    }

    public ProjectCreationContent content() {
        return new ProjectCreationContent(codeField.getValue(), offerSearchField.getPattern(), titleField.getValue(),
                projectObjective.getValue(), speciesBox.getValue().stream().toList(),
                specimenBox.getValue().stream().toList(), analyteBox.getValue().stream().toList(),
                experimentalDesignField.getValue(), principalInvestigator.getValue(), responsiblePerson.getValue(),
                projectManager.getValue());
    }


    private void styleForm() {
        formLayout.setClassName("create-project-form");
        styleSearchBox();

        codeField.setMaxWidth(20, Unit.VW);
        offerSearchField.setMaxWidth(30, Unit.VW);

        titleField.setMaxWidth(60, Unit.VW);
        projectObjective.setMaxWidth(60, Unit.VW);

        speciesBox.addClassName("chip-badge");
        specimenBox.addClassName("chip-badge");
        analyteBox.addClassName("chip-badge");
        experimentalDesignField.setMaxWidth(60, Unit.VW);

        speciesBox.setMaxWidth(60, Unit.VW);
        specimenBox.setMaxWidth(60, Unit.VW);
        analyteBox.setMaxWidth(60, Unit.VW);

        principalInvestigator.setMaxWidth(60, Unit.VW);
        responsiblePerson.setMaxWidth(60, Unit.VW);
        projectManager.setMaxWidth(60, Unit.VW);
    }

    private void configureDialogLayout() {
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        setHeaderTitle("Create Project");
        add(formLayout);
        getFooter().add(cancelButton, createButton);
        this.setMinWidth(66, Unit.VW);
        this.setMaxWidth(66, Unit.VW);
    }

    private void initForm() {
        codeAndTitleLayout = new HorizontalLayout();
        codeAndTitleLayout.setWidthFull();
        codeAndTitleLayout.add(codeField);
        codeAndTitleLayout.add(generateCodeButton);
        codeAndTitleLayout.add(offerSearchField);
        codeAndTitleLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        formLayout.add(codeAndTitleLayout);

        formLayout.add(titleField);
        formLayout.add(projectObjective);
        formLayout.add(experimentalDesignLayout);
        formLayout.add(sampleCountLayout);
        formLayout.add(speciesBox);
        formLayout.add(specimenBox);
        formLayout.add(analyteBox);
        formLayout.add(experimentalDesignField);
        formLayout.add(projectContactsLayout);
        formLayout.add(principalInvestigator);
        formLayout.add(responsiblePerson);
        formLayout.add(projectManager);
        // set form layout to only have one column (for any width)
        formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    }

    private void styleSearchBox() {
        offerSearchField.setPlaceholder("Search");
        offerSearchField.setClassName("searchbox");
    }

    private void initExperimentalDesignLayout() {
        Span experimentHeader = new Span("Experiment");
        Span experimentDescription = new Span("Please specify the sample origin information of the samples. Multiple "
                + "values are allowed!");
        experimentHeader.addClassName("font-bold");
        experimentalDesignLayout.setMargin(false);
        experimentalDesignLayout.setPadding(false);
        experimentalDesignLayout.addClassName("pt-m");
        experimentalDesignLayout.add(experimentHeader);
        experimentalDesignLayout.add(experimentDescription);
    }

    private void initProjectContactsLayout() {
        Span projectContactsTitle = new Span("Project Contacts");
        Span projectContactsDescription = new Span("Important contact people of the project");
        projectContactsTitle.addClassName("font-bold");
        projectContactsLayout.setMargin(false);
        projectContactsLayout.setPadding(false);
        projectContactsLayout.addClassName("pt-m");
        projectContactsLayout.add(projectContactsTitle);
        projectContactsLayout.add(projectContactsDescription);
    }

    public void setOffer(Offer offer) {
        handler.loadOfferContent(offer);
    }

    public String getCode() {
        return codeField.getValue();
    }

    public String getTitle() {
        return titleField.getValue();
    }

    public String getObjective() {
        return projectObjective.getValue();
    }

    //ToDo Replace with values from Comboboxes
    public String getExperimentalDesign() {
        return experimentalDesignField.getValue();
    }

    /**
     * Resets the values and validity of all components that implement value storing and validity
     * interfaces
     */
    public void reset() {
        resetChildValues(formLayout);
        resetChildValues(codeAndTitleLayout);
        resetChildValidation(formLayout);
        resetChildValidation(codeAndTitleLayout);
    }

    private void resetChildValues(Component component) {
        component.getChildren().filter(comp -> comp instanceof HasValue<?, ?>).forEach(comp -> ((HasValue<?, ?>) comp).clear());

    }

    private void defaultProjectCodeCreation() {
        this.addOpenedChangeListener(openedChangeEvent -> {
            if (openedChangeEvent.isOpened()) {
                codeField.setValue(ProjectCode.random().value());
            }
        });
    }

    private void resetChildValidation(Component component) {
        component.getChildren().filter(comp -> comp instanceof HasValidation).forEach(comp -> ((HasValidation) comp).setInvalid(false));
    }

    public void resetAndClose() {
        close();
        reset();
    }

    private class Handler {
        List<Binder<?>> binders = new ArrayList<>();
        private final List<ComponentEventListener<ProjectCreationEvent>> listeners = new ArrayList<>();
        private final List<ComponentEventListener<UserCancelEvent<ProjectInformationDialog>>> cancelListeners =
                new ArrayList<>();

        Handler() {
        }

        private void handle() {
            restrictInputLength();
            generateProjectCode();
            resetDialogueUponClosure();
            closeDialogueViaCancelButton();
            configureValidators();
            configureFormSubmission();
        }

        private void configureFormSubmission() {
            createButton.addClickListener(event -> {
                validateInput();
                if (isInputValid()) {
                    listeners.forEach(listener -> listener.onComponentEvent(new ProjectCreationEvent(ProjectInformationDialog.this, true)));
                }
            });
            cancelButton.addClickListener(event -> cancelListeners.forEach(listener -> listener.onComponentEvent(new UserCancelEvent<>(ProjectInformationDialog.this))));

        }


        private void configureValidators() {
            Binder<Container<String>> binderTitle = new Binder<>();
            binderTitle.forField(titleField).withValidator(value -> !value.isBlank(), "Please provide a title").bind(Container::value, Container::setValue);
            Binder<Container<String>> binderObjective = new Binder<>();
            binderObjective.forField(projectObjective).withValidator(value -> !value.isBlank(),
                    "Please provide an " + "objective").bind(Container::value, Container::setValue);
            Binder<Container<Set<Species>>> binderSpecies = new Binder<>();
            binderSpecies.forField(speciesBox).asRequired("Please select at least one species").bind(Container::value
                    , Container::setValue);
            Binder<Container<Set<Analyte>>> binderAnalyte = new Binder<>();
            binderAnalyte.forField(analyteBox).asRequired("Please select at least one analyte").bind(Container::value
                    , Container::setValue);
            Binder<Container<Set<Specimen>>> binderSpecimen = new Binder<>();
            binderSpecimen.forField(specimenBox).asRequired("Please select at least one specimen").bind(Container::value, Container::setValue);
            Binder<Container<PersonReference>> binderPI = new Binder<>();
            binderPI.forField(principalInvestigator).asRequired("Please select at least one PI").bind(Container::value, Container::setValue);
            Binder<Container<PersonReference>> binderPM = new Binder<>();
            binderPM.forField(projectManager).asRequired("Please select at least one PM").bind(Container::value,
                    Container::setValue);
            binders.addAll(List.of(binderTitle, binderObjective, binderSpecies, binderAnalyte, binderSpecimen,
                    binderPI, binderPM));
        }

        private void generateProjectCode() {
            generateCodeButton.addClickListener(buttonClickEvent -> setCodeFieldValue(ProjectCode.random().value()));
        }

        private void setCodeFieldValue(String code) {
            codeField.setValue(code);
        }

        public void loadOfferContent(Offer offer) {
            titleField.setValue(offer.projectTitle().title());
            projectObjective.setValue(offer.projectObjective().objective());
            experimentalDesignField.setValue(offer.experimentalDesignDescription().description());

        }

        private void restrictInputLength() {
            titleField.setMaxLength((int) ProjectTitle.maxLength());
            projectObjective.setMaxLength((int) ProjectObjective.maxLength());
            experimentalDesignField.setMaxLength((int) ExperimentalDesignDescription.maxLength());

            titleField.setValueChangeMode(ValueChangeMode.EAGER);
            projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
            experimentalDesignField.setValueChangeMode(ValueChangeMode.EAGER);

            addConsumedLengthHelper(titleField, titleField.getValue());
            addConsumedLengthHelper(projectObjective, projectObjective.getValue());
            addConsumedLengthHelper(experimentalDesignField, experimentalDesignField.getValue());

            titleField.addValueChangeListener(e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
            projectObjective.addValueChangeListener(e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
            experimentalDesignField.addValueChangeListener(e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
        }

        private void addConsumedLengthHelper(TextArea textArea, String newValue) {
            int maxLength = textArea.getMaxLength();
            int consumedLength = newValue.length();
            textArea.setHelperText(consumedLength + "/" + maxLength);
        }

        private void addConsumedLengthHelper(TextField textField, String newValue) {
            int maxLength = textField.getMaxLength();
            int consumedLength = newValue.length();
            textField.setHelperText(consumedLength + "/" + maxLength);
        }

        private void closeDialogueViaCancelButton() {
            cancelButton.addClickListener(buttonClickEvent -> resetAndClose());
        }

        private void resetDialogueUponClosure() {
            // Calls the reset method for all possible closure methods of the dialogue window:
            addDialogCloseActionListener(closeActionEvent -> resetAndClose());
        }

        protected boolean validateInput() {
            binders.forEach(Binder::validate);
            return binders.stream().allMatch(Binder::isValid);
        }

        public boolean isInputValid() {
            return validateInput();
        }

        public void addProjectCreationEventListener(ComponentEventListener<ProjectCreationEvent> listener) {
            this.listeners.add(listener);
        }

        public void addUserCancelEventListener(ComponentEventListener<UserCancelEvent<ProjectInformationDialog>> listener) {
            this.cancelListeners.add(listener);
        }
    }

    static class Container<T> {

        private T value;

        T value() {
            return this.value;
        }

        void setValue(T newValue) {
            this.value = newValue;
        }

    }
}
