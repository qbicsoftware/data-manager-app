package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.HasValidation;
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
import life.qbic.datamanager.views.projects.project.experiments.experiment.OntologyFilterConnector;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;

/**
 * <b>Experimental Information Layout</b>
 *
 * <p>Layout which enables the user to input the information associated with an {@link Experiment}
 * during project creation and validates the provided information</p>
 */
public class ExperimentalInformationLayout extends Div implements HasValidation {

  private static final String TITLE = "Experimental Information";
  private static final String CHIP_BADGE = "chip-badge";
  private static final String WIDTH_INPUT = "full-width-input";
  private final Binder<ExperimentalInformation> experimentalInformationBinder = new Binder<>(
      ExperimentalInformation.class);
  private final transient OntologyFilterConnector ontologyFilterConnector;

  public ExperimentalInformationLayout(
      OntologyTermInformationService ontologyTermInformationService) {
    this.ontologyFilterConnector = new OntologyFilterConnector(ontologyTermInformationService);
    initTitleAndDescription();
    initNameField();
    initOntologyComboboxes();
    addClassName("experiment-information-layout");
  }

  private void initTitleAndDescription() {
    Span experimentInformationTitle = new Span(TITLE);
    Span experimentInformationDescription = new Span(
        "The experiment name and sample origin information of the samples");
    experimentInformationTitle.addClassName("title");
    add(experimentInformationTitle,
        experimentInformationDescription);
  }

  private void initNameField() {
    TextField experimentNameField = new TextField("Experiment Name");
    experimentNameField.addClassName("experiment-name-field");
    experimentNameField.setPlaceholder("Please enter a name for your experiment");
    experimentNameField.setRequired(true);
    experimentalInformationBinder.forField(experimentNameField)
        .withValidator(it -> !it.isBlank(), "Please provide a name for the experiment")
        .bind(ExperimentalInformation::getExperimentName,
            ExperimentalInformation::setExperimentName);
    add(experimentNameField);
  }

  private void initOntologyComboboxes() {
    MultiSelectComboBox<OntologyClassDTO> speciesBox = new MultiSelectComboBox<>("Species");
    ontologyFilterConnector.initComboBoxWithOntologyDatasource(speciesBox,
        List.of(Ontology.NCBI_TAXONOMY));
    speciesBox.setItemLabelGenerator(OntologyClassDTO::getLabel);
    speciesBox.setPlaceholder("Please select one or more species for your samples");
    experimentalInformationBinder.forField(speciesBox)
        .asRequired("Please select at least one species")
        .bind(experimentalInformation -> new HashSet<>(experimentalInformation.getSpecies()),
            ExperimentalInformation::setSpecies);
    speciesBox.addClassNames(CHIP_BADGE, WIDTH_INPUT);
    MultiSelectComboBox<OntologyClassDTO> specimenBox = new MultiSelectComboBox<>("Specimen");
    ontologyFilterConnector.initComboBoxWithOntologyDatasource(specimenBox,
        Arrays.asList(Ontology.PLANT_ONTOLOGY, Ontology.BRENDA_TISSUE_ONTOLOGY));
    specimenBox.setItemLabelGenerator(OntologyClassDTO::getLabel);
    specimenBox.setPlaceholder("Please select one or more specimen for your samples");
    experimentalInformationBinder.forField(specimenBox)
        .asRequired("Please select at least one specimen")
        .bind(experimentalInformation -> new HashSet<>(experimentalInformation.getSpecimens()),
            ExperimentalInformation::setSpecimens);
    specimenBox.addClassNames(CHIP_BADGE, WIDTH_INPUT);
    MultiSelectComboBox<OntologyClassDTO> analyteBox = new MultiSelectComboBox<>("Analyte");
    ontologyFilterConnector.initComboBoxWithOntologyDatasource(analyteBox,
        List.of(Ontology.BIOASSAY_ONTOLOGY));
    analyteBox.setItemLabelGenerator(OntologyClassDTO::getLabel);
    analyteBox.setPlaceholder("Please select one or more analytes for your samples");
    experimentalInformationBinder.forField(analyteBox)
        .asRequired("Please select at least one analyte")
        .bind(experimentalInformation -> new HashSet<>(experimentalInformation.getAnalytes()),
            ExperimentalInformation::setAnalytes);
    analyteBox.addClassNames(CHIP_BADGE, WIDTH_INPUT);
    add(speciesBox, specimenBox, analyteBox);
  }


  public ExperimentalInformation getExperimentalInformation() {
    ExperimentalInformation experimentalInformation = new ExperimentalInformation();
    experimentalInformationBinder.writeBeanIfValid(experimentalInformation);
    return experimentalInformation;
  }

  /**
   * Sets an error message to the component.
   * <p>
   * The Web Component is responsible for deciding when to show the error message to the user, and
   * this is usually triggered by triggering the invalid state for the Web Component. Which means
   * that there is no need to clean up the message when component becomes valid (otherwise it may
   * lead to undesired visual effects).
   *
   * @param errorMessage a new error message
   */
  @Override
  public void setErrorMessage(String errorMessage) {
    /* Unused since we are only interested in the final values stored in the component*/
  }

  /**
   * Gets current error message from the component.
   *
   * @return current error message
   */
  @Override
  public String getErrorMessage() {
    return "Invalid Input found in Experiment Information";
  }

  /**
   * Sets the validity of the component input.
   * <p>
   * When component becomes valid it hides the error message by itself, so there is no need to clean
   * up the error message via the {@link #setErrorMessage(String)} call.
   *
   * @param invalid new value for component input validity
   */
  @Override
  public void setInvalid(boolean invalid) {
    /* Unused since we are only interested in the final values stored in the component*/
  }

  /**
   * Returns {@code true} if component input is invalid, {@code false} otherwise.
   *
   * @return whether the component input is valid
   */
  @Override
  public boolean isInvalid() {
    experimentalInformationBinder.validate();
    return !experimentalInformationBinder.isValid();
  }

  public static class ExperimentalInformation implements Serializable {

    @Serial
    private static final long serialVersionUID = -2712521934990738542L;
    private String experimentName;
    private final List<OntologyClassDTO> species;
    private final List<OntologyClassDTO> specimen;
    private final List<OntologyClassDTO> analytes;

    public ExperimentalInformation() {
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
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ExperimentalInformation that = (ExperimentalInformation) o;

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

    @Override
    public String toString() {
      return "ExperimentalInformation{" +
          "experimentName='" + experimentName + '\'' +
          ", species=" + species +
          ", specimen=" + specimen +
          ", analytes=" + analytes +
          '}';
    }
  }

}
