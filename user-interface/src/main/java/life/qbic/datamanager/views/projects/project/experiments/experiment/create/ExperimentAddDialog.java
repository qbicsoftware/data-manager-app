package life.qbic.datamanager.views.projects.project.experiments.experiment.create;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.project.experiments.experiment.OntologyFilterConnector;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;

/**
 * <b>ExperimentAddDialog</b>
 *
 * <p>Dialog to create an experiment information by providing the minimal required
 * information</p>
 *
 * @since 1.0.0
 */

public class ExperimentAddDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 2142928219461555700L;

  private static final String CHIP_BADGE = "chip-badge";
  private static final String WIDTH_INPUT = "full-width-input";
  private final Binder<ExperimentDraft> binder = new Binder<>();
  private final OntologyFilterConnector ontologyFilterConnector;
//TODO
  //1. new class OntologyClassDTO
  //2. replace all occurrences of OntologyClassEntity with OntologyClassDTO
  //3. use OntologyClassEntity for lookup queries only, map to OntologyClassDTO
  //TODO
  public ExperimentAddDialog(
      OntologyTermInformationService ontologyTermInformationService) {
    this.ontologyFilterConnector = new OntologyFilterConnector(ontologyTermInformationService);
    Span experimentHeader = new Span("Experiment");
    experimentHeader.addClassName("header");

    TextField experimentNameField = new TextField("Experiment Name");
    experimentNameField.addClassName(WIDTH_INPUT);
    binder.forField(experimentNameField).asRequired("Please provide a name for the experiment")
        .bind(ExperimentDraft::getExperimentName, ExperimentDraft::setExperimentName);

    Span experimentDescription = new Span(
        "Please specify the sample origin information of the samples. Multiple "
            + "values are allowed!");

    MultiSelectComboBox<OntologyClassDTO> speciesBox = new MultiSelectComboBox<>("Species");
    speciesBox.addClassNames(CHIP_BADGE, WIDTH_INPUT);
    ontologyFilterConnector.initComboBoxWithOntologyDatasource(speciesBox, List.of(Ontology.NCBI_TAXONOMY));
    speciesBox.setItemLabelGenerator(OntologyClassDTO::getLabel);
    binder.forField(speciesBox)
        .asRequired("Please select at least one species")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getSpecies()),
            ExperimentDraft::setSpecies);

    MultiSelectComboBox<OntologyClassDTO> specimenBox = new MultiSelectComboBox<>("Specimen");
    specimenBox.addClassNames(CHIP_BADGE, WIDTH_INPUT);
    ontologyFilterConnector.initComboBoxWithOntologyDatasource(specimenBox, Arrays.asList(Ontology.PLANT_ONTOLOGY, Ontology.BRENDA_TISSUE_ONTOLOGY));
    specimenBox.setItemLabelGenerator(OntologyClassDTO::getLabel);
    binder.forField(specimenBox)
        .asRequired("Please select at least one specimen")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getSpecimens()),
            ExperimentDraft::setSpecimens);

    MultiSelectComboBox<OntologyClassDTO> analyteBox = new MultiSelectComboBox<>("Analyte");
    analyteBox.addClassNames(CHIP_BADGE, WIDTH_INPUT);
    ontologyFilterConnector.initComboBoxWithOntologyDatasource(analyteBox, List.of(Ontology.BIOASSAY_ONTOLOGY));
    analyteBox.setItemLabelGenerator(OntologyClassDTO::getLabel);
    binder.forField(analyteBox)
        .asRequired("Please select at least one analyte")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getAnalytes()),
            ExperimentDraft::setAnalytes);

    Div createExperimentContent = new Div();
    createExperimentContent.addClassName("add-experiment-content");
    createExperimentContent.add(experimentHeader,
        experimentDescription,
        experimentNameField,
        experimentDescription,
        speciesBox,
        specimenBox,
        analyteBox);

    addClassName("add-experiment-dialog");
    setHeaderTitle("Experimental Design");
    setConfirmButtonLabel("Add");
    setCancelButtonLabel("Cancel");
    add(createExperimentContent);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    ExperimentDraft experimentDraft = new ExperimentDraft();
    boolean isValid = binder.writeBeanIfValid(experimentDraft);
    if (isValid) {
      fireEvent(new ExperimentAddEvent(this, clickEvent.isFromClient(), experimentDraft));
    }
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
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

  public void addExperimentAddEventListener(ComponentEventListener<ExperimentAddEvent> listener) {
    addListener(ExperimentAddEvent.class, listener);
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public static class CancelEvent extends UserCancelEvent<ExperimentAddDialog> {

    public CancelEvent(ExperimentAddDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class ExperimentAddEvent extends ComponentEvent<ExperimentAddDialog> {

    private final ExperimentDraft experimentDraft;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source          the source component
     * @param fromClient      <code>true</code> if the event originated from the client
     *                        side, <code>false</code> otherwise
     * @param experimentDraft the draft for the experiment
     */
    public ExperimentAddEvent(ExperimentAddDialog source, boolean fromClient,
        ExperimentDraft experimentDraft) {
      super(source, fromClient);
      this.experimentDraft = experimentDraft;
    }

    public ExperimentDraft getExperimentDraft() {
      return experimentDraft;
    }
  }

  public static class ExperimentDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = -2259332255266132217L;

    private String experimentName;
    private final List<OntologyClassDTO> species;
    private final List<OntologyClassDTO> specimen;
    private final List<OntologyClassDTO> analytes;

    public ExperimentDraft() {
      species = new ArrayList<>();
      specimen = new ArrayList<>();
      analytes = new ArrayList<>();
    }

    public String getExperimentName() {
      return experimentName;
    }

    public void setExperimentName(String experimentName) {
      this.experimentName = experimentName;
    }

    public List<OntologyClassDTO> getSpecies() {
      return new ArrayList<>(species);
    }

    public void setSpecies(Collection<OntologyClassDTO> species) {
      this.species.clear();
      this.species.addAll(species);
    }

    public List<OntologyClassDTO> getSpecimens() {
      return new ArrayList<>(specimen);
    }

    public void setSpecimens(Collection<OntologyClassDTO> specimen) {
      this.specimen.clear();
      this.specimen.addAll(specimen);
    }

    public List<OntologyClassDTO> getAnalytes() {
      return new ArrayList<>(analytes);
    }

    public void setAnalytes(Collection<OntologyClassDTO> analytes) {
      this.analytes.clear();
      this.analytes.addAll(analytes);
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      ExperimentDraft that = (ExperimentDraft) object;

      if (!Objects.equals(experimentName, that.experimentName)) {
        return false;
      }
      if (!species.equals(that.species)) {
        return false;
      }
      if (!specimen.equals(that.specimen)) {
        return false;
      }
      return analytes.equals(that.analytes);
    }

    @Override
    public int hashCode() {
      int result = experimentName != null ? experimentName.hashCode() : 0;
      result = 31 * result + species.hashCode();
      result = 31 * result + specimen.hashCode();
      result = 31 * result + analytes.hashCode();
      return result;
    }
  }
}
