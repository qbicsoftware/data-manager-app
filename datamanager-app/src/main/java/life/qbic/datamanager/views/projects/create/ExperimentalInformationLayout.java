package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
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
import life.qbic.datamanager.views.general.HasBinderValidation;
import life.qbic.datamanager.views.projects.create.ExperimentalInformationLayout.ExperimentalInformation;
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
  private final TextField nameField;
  private final MultiSelectComboBox<OntologyTerm> speciesBox;
  private final MultiSelectComboBox<OntologyTerm> specimenBox;
  private final MultiSelectComboBox<OntologyTerm> analyteBox;

  public ExperimentalInformationLayout(
      SpeciesLookupService ontologyTermInformationService, TerminologyService terminologyService) {
    requireNonNull(ontologyTermInformationService,
        "ontologyTermInformationService must not be null");
    requireNonNull(terminologyService);
    OntologyComboboxFactory ontologyComboboxFactory = new OntologyComboboxFactory(
        ontologyTermInformationService, terminologyService);

    speciesBox = ontologyComboboxFactory.speciesBox();
    speciesBox.addClassName("box-flexgrow");
    specimenBox = ontologyComboboxFactory.specimenBox();
    specimenBox.addClassName("box-flexgrow");
    analyteBox = ontologyComboboxFactory.analyteBox();
    nameField = nameField();

    experimentalInformationBinder = new Binder<>(ExperimentalInformation.class);

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

    Div speciesRow = new Div(speciesBox);
    speciesRow.addClassName("input-with-icon-selection");
    Div specimenRow = new Div(specimenBox);
    specimenRow.addClassName("input-with-icon-selection");

    add(title(),
        description(),
        nameField,
        speciesRow,
        specimenRow,
        analyteBox);
    addClassName("experiment-information-layout");

    // This step is optional and the wizard reads a completely empty step as "no experiment", so
    // the fields must not advertise themselves as required (the combo boxes set the indicator in
    // OntologyComboboxFactory). Validation is deliberately left alone: the bindings keep their
    // asRequired rules, which is what makes a *partly* filled step fail instead of silently
    // creating an experiment with no species.
    nameField.setRequired(false);
    speciesBox.setRequired(false);
    specimenBox.setRequired(false);
    analyteBox.setRequired(false);
  }

  /**
   * Adds a short note directly under the section title, above the description. The project
   * creation wizard uses it to mark the step as optional: a qualifier belongs below the heading it
   * qualifies, not above it.
   *
   * @param hint the note to display
   */
  public void setStepHint(String hint) {
    Span hintSpan = new Span(hint);
    hintSpan.addClassName("step-hint");
    // Index 1 is directly after the title, which the constructor adds first.
    getElement().insertChild(1, hintSpan.getElement());
  }

  private static Component title() {
    Span experimentInformationTitle = new Span("Experimental Information");
    experimentInformationTitle.addClassName("title");
    return experimentInformationTitle;
  }

  private static Component description() {
    return new Span(
        "Species, specimen and analyte become the controlled options for sample origin in the "
            + "sample-registration template, so sample annotations stay consistent and use public "
            + "ontology terms.");
  }

  private static TextField nameField() {
    TextField experimentNameField = new TextField("Experiment Name");
    experimentNameField.addClassName("experiment-name-field");
    experimentNameField.setPlaceholder("Please enter a name for your experiment");
    return experimentNameField;
  }


  public ExperimentalInformation getExperimentalInformation() {
    ExperimentalInformation experimentalInformation = new ExperimentalInformation();
    experimentalInformationBinder.writeBeanIfValid(experimentalInformation);
    return experimentalInformation;
  }

  /**
   * Whether nothing has been entered at all. An empty optional step means "no experiment", so the
   * wizard can skip it without failing its required-field validation.
   */
  public boolean isEmpty() {
    return (nameField.getValue() == null || nameField.getValue().isBlank())
        && speciesBox.getValue().isEmpty()
        && specimenBox.getValue().isEmpty()
        && analyteBox.getValue().isEmpty();
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
