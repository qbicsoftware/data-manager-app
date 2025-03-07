package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import static java.util.Objects.requireNonNull;
import life.qbic.datamanager.views.general.HasBinderValidation;
import life.qbic.datamanager.views.projects.create.ExperimentalInformationLayout.ExperimentalInformation;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent.BioIcon;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent.SampleSourceType;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;

/**
 * <b>Experimental Information Layout</b>
 *
 * <p>Layout which enables the user to input the information associated with an {@link Experiment}
 * during project creation and validates the provided information</p>
 */
public class ExperimentalInformationLayout extends Div implements
    HasBinderValidation<ExperimentalInformation> {

  private final Binder<ExperimentalInformation> experimentalInformationBinder;

  public ExperimentalInformationLayout(
      SpeciesLookupService ontologyTermInformationService, TerminologyService terminologyService) {
    requireNonNull(ontologyTermInformationService,
        "ontologyTermInformationService must not be null");
    requireNonNull(terminologyService);
    OntologyComboboxFactory ontologyComboboxFactory = new OntologyComboboxFactory(
        ontologyTermInformationService, terminologyService);
    final BioIconComboboxFactory bioIconComboboxFactory = new BioIconComboboxFactory();

    MultiSelectComboBox<OntologyTerm> speciesBox = ontologyComboboxFactory.speciesBox();
    speciesBox.addClassName("box-flexgrow");
    MultiSelectComboBox<OntologyTerm> specimenBox = ontologyComboboxFactory.specimenBox();
    specimenBox.addClassName("box-flexgrow");
    MultiSelectComboBox<OntologyTerm> analyteBox = ontologyComboboxFactory.analyteBox();
    TextField nameField = nameField();

    experimentalInformationBinder = new Binder<>(ExperimentalInformation.class);

    ComboBox<BioIcon> speciesIconBox = bioIconComboboxFactory.iconBox(SampleSourceType.SPECIES,
        "Species icon");
    experimentalInformationBinder.forField(speciesIconBox)
        .bind(ExperimentalInformation::getSpeciesIcon,
            ExperimentalInformation::setSpeciesIcon);

    ComboBox<BioIcon> specimenIconBox = bioIconComboboxFactory.iconBox(SampleSourceType.SPECIMEN,
        "Specimen icon");
    experimentalInformationBinder.forField(specimenIconBox)
        .bind(ExperimentalInformation::getSpecimenIcon,
            ExperimentalInformation::setSpecimenIcon);

    experimentalInformationBinder.forField(speciesBox)
        .asRequired("Please select at least one species")
        .bind(experimentalInformation -> new HashSet<>(experimentalInformation.getSpecies()),
            ExperimentalInformation::setSpecies);
    experimentalInformationBinder.forField(specimenBox)
        .asRequired("Please select at least one specimen")
        .bind(experimentalInformation -> new HashSet<>(experimentalInformation.getSpecimens()),
            ExperimentalInformation::setSpecimens);
    experimentalInformationBinder.forField(analyteBox)
        .asRequired("Please select at least one analyte")
        .bind(experimentalInformation -> new HashSet<>(experimentalInformation.getAnalytes()),
            ExperimentalInformation::setAnalytes);
    experimentalInformationBinder.forField(nameField)
        .withValidator(it -> !it.isBlank(), "Please provide a name for the experiment")
        .bind(ExperimentalInformation::getExperimentName,
            ExperimentalInformation::setExperimentName);

    Div speciesRow = new Div(speciesIconBox, speciesBox);
    speciesRow.addClassName("input-with-icon-selection");
    Div specimenRow = new Div(specimenIconBox, specimenBox);
    specimenRow.addClassName("input-with-icon-selection");

    add(title(),
        description(),
        nameField,
        speciesRow,
        specimenRow,
        analyteBox);
    addClassName("experiment-information-layout");
  }

  private static Component title() {
    Span experimentInformationTitle = new Span("Experimental Information");
    experimentInformationTitle.addClassName("title");
    return experimentInformationTitle;
  }

  private static Component description() {
    return new Span(
        "Specify the experiment name and sample origin information of the samples.");
  }

  private static TextField nameField() {
    TextField experimentNameField = new TextField("Experiment Name");
    experimentNameField.addClassName("experiment-name-field");
    experimentNameField.setPlaceholder("Please enter a name for your experiment");
    experimentNameField.setRequired(true);
    return experimentNameField;
  }


  public ExperimentalInformation getExperimentalInformation() {
    ExperimentalInformation experimentalInformation = new ExperimentalInformation();
    experimentalInformationBinder.writeBeanIfValid(experimentalInformation);
    return experimentalInformation;
    //TODO I am unsure whether it is intended that on invalid input, empty output is
    // provided instead of a failure.
  }


  @Override
  public Binder<ExperimentalInformation> getBinder() {
    return experimentalInformationBinder;
  }

  @Override
  public String getDefaultErrorMessage() {
    return "Invalid input found in Experiment Information";
  }


  public static class ExperimentalInformation implements Serializable {

    @Serial
    private static final long serialVersionUID = -2712521934990738542L;
    private String experimentName;
    private final List<OntologyTerm> species;
    private final List<OntologyTerm> specimen;
    private final List<OntologyTerm> analytes;
    private String speciesIconName;
    private String specimenIconName;

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

    public List<OntologyTerm> getSpecies() {
      return new ArrayList<>(species);
    }

    public void setSpecies(Collection<OntologyTerm> species) {
      this.species.clear();
      this.species.addAll(species);
    }

    public List<OntologyTerm> getSpecimens() {
      return new ArrayList<>(specimen);
    }

    public void setSpecimens(Collection<OntologyTerm> specimen) {
      this.specimen.clear();
      this.specimen.addAll(specimen);
    }

    public List<OntologyTerm> getAnalytes() {
      return new ArrayList<>(analytes);
    }

    public void setAnalytes(Collection<OntologyTerm> analytes) {
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
      if (!speciesIconName.equals(that.speciesIconName)) {
        return false;
      }
      if (!specimenIconName.equals(that.specimenIconName)) {
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
      result = 31 * result + specimenIconName.hashCode();
      result = 31 * result + speciesIconName.hashCode();
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

    public BioIcon getSpeciesIcon() {
      return BioIcon.getTypeWithNameOrDefault(SampleSourceType.SPECIES, speciesIconName);
    }

    public void setSpeciesIcon(BioIcon bioIcon) {
      this.speciesIconName = bioIcon.getLabel();
    }

    public BioIcon getSpecimenIcon() {
      return BioIcon.getTypeWithNameOrDefault(SampleSourceType.SPECIMEN, specimenIconName);
    }

    public void setSpecimenIcon(BioIcon bioIcon) {
      this.specimenIconName = bioIcon.getLabel();
    }
  }

}
