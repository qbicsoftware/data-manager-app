package life.qbic.datamanager.views.projects.project.experiments.experiment.update;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.SortDirection;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentAddDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentAddDialog.ExperimentDraft;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.application.OntologyClassEntity;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;

/**
 * <b>ExperimentUpdateDialog</b>
 *
 * <p>Dialog to edit experiment information by providing the minimal required
 * information</p>
 *
 * @since 1.0.0
 */

public class ExperimentUpdateDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 2142928219461555700L;

  private static final String CHIP_BADGE = "chip-badge";
  private static final String WIDTH_INPUT = "full-width-input";
  private final transient OntologyTermInformationService ontologyTermInformationService;

  private final Binder<ExperimentDraft> binder = new Binder<>();

  public ExperimentUpdateDialog(OntologyTermInformationService ontologyTermInformationService) {
    this.ontologyTermInformationService = ontologyTermInformationService;

    Span experimentHeader = new Span("Experiment");
    experimentHeader.addClassName("header");

    TextField experimentNameField = new TextField("Experiment Name");
    experimentNameField.addClassName(WIDTH_INPUT);
    binder.forField(experimentNameField).asRequired("Please provide a name for the experiment")
        .bind(ExperimentDraft::getExperimentName, ExperimentDraft::setExperimentName);

    Span experimentDescription = new Span(
        "Please specify the sample origin information of the samples. Multiple "
            + "values are allowed!");

    MultiSelectComboBox<Species> speciesBox = new MultiSelectComboBox<>("Species");
        initComboBoxWithDatasource(speciesBox, List.of("NCBITaxon"),
            term -> new Species(term.getLabel()));
    speciesBox.setItemLabelGenerator(Species::label);
    binder.forField(speciesBox)
        .asRequired("Please select at least one species")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getSpecies()),
            ExperimentAddDialog.ExperimentDraft::setSpecies);

    MultiSelectComboBox<Specimen> specimenBox = new MultiSelectComboBox<>("Specimen");
    initComboBoxWithDatasource(specimenBox, Arrays.asList("po", "bto"),
        term -> new Specimen(term.getLabel()));
    specimenBox.setItemLabelGenerator(Specimen::label);
    binder.forField(specimenBox)
        .asRequired("Please select at least one specimen")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getSpecimens()),
            ExperimentAddDialog.ExperimentDraft::setSpecimens);

    MultiSelectComboBox<Analyte> analyteBox = new MultiSelectComboBox<>("Analyte");
    initComboBoxWithDatasource(analyteBox, List.of("bao_complete"),
        term -> new Analyte(term.getLabel()));
    analyteBox.setItemLabelGenerator(Analyte::label);
    binder.forField(analyteBox)
        .asRequired("Please select at least one analyte")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getAnalytes()),
            ExperimentAddDialog.ExperimentDraft::setAnalytes);
    Div updateExperimentContent = new Div();
    updateExperimentContent.addClassName("update-experiment-content");
    updateExperimentContent.add(experimentHeader,
        experimentDescription,
        experimentNameField,
        experimentDescription,
        speciesBox,
        specimenBox,
        analyteBox);

    addClassName("update-experiment-dialog");
    setHeaderTitle("Experimental Design");
    setConfirmButtonLabel("Save");
    setCancelButtonLabel("Cancel");
    add(updateExperimentContent);

    confirmButton.addClickListener(this::onConfirmClicked);
    cancelButton.addClickListener(this::onCancelClicked);
  }

  private <T> void initComboBoxWithDatasource(MultiSelectComboBox<T> box, List<String> ontologies,
      Function<OntologyClassEntity, T> ontologyMapping) {

    box.setRequired(true);
    box.addClassNames(CHIP_BADGE, WIDTH_INPUT);

    box.setItemsWithFilterConverter(
        query -> ontologyTermInformationService.queryOntologyTerm(query.getFilter().orElse(""),
            ontologies,
            query.getOffset(),
            query.getLimit(), query.getSortOrders().stream().map(
                    it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.DESCENDING)))
                .collect(Collectors.toList())).stream().map(ontologyMapping),
        entity -> entity
    );
  }

  private void onConfirmClicked(ClickEvent<Button> clickEvent) {
    ExperimentDraft experimentDraft = new ExperimentDraft();
    ExperimentDraft oldDraft = binder.getBean();
    boolean isValid = binder.writeBeanIfValid(experimentDraft);
    if (isValid) {
      fireEvent(
          new ExperimentUpdateEvent(this, clickEvent.isFromClient(), oldDraft, experimentDraft));
    }
  }

  private void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  public void setExperiment(ExperimentDraft experiment) {
    binder.setBean(experiment);
  }

  @Override
  public void close() {
    super.close();
    reset();
  }

  public void reset() {
    binder.setBean(new ExperimentDraft());
  }

  public void addExperimentUpdateEventListener(
      ComponentEventListener<ExperimentUpdateEvent> listener) {
    addListener(ExperimentUpdateEvent.class, listener);
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public static class CancelEvent extends UserCancelEvent<ExperimentUpdateDialog> {

    public CancelEvent(ExperimentUpdateDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class ExperimentUpdateEvent extends ComponentEvent<ExperimentUpdateDialog> {

    private final ExperimentDraft oldDraft;
    private final ExperimentDraft experimentDraft;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source          the source component
     * @param fromClient      <code>true</code> if the event originated from the client
     *                        side, <code>false</code> otherwise
     * @param oldDraft        the draft of the old experiment
     * @param experimentDraft the draft for the changed experiment
     */
    public ExperimentUpdateEvent(ExperimentUpdateDialog source, boolean fromClient,
        ExperimentDraft oldDraft, ExperimentDraft experimentDraft) {
      super(source, fromClient);
      this.experimentDraft = experimentDraft;
      this.oldDraft = oldDraft;
    }

    public ExperimentDraft getExperimentDraft() {
      return experimentDraft;
    }

    public Optional<ExperimentDraft> getOldDraft() {
      return Optional.ofNullable(oldDraft);
    }
  }
}
