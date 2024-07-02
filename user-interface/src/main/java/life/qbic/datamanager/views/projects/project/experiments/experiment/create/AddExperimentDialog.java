package life.qbic.datamanager.views.projects.project.experiments.experiment.create;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
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
import life.qbic.datamanager.views.CancelConfirmationNotificationDialog;
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.create.BioIconComboboxFactory;
import life.qbic.datamanager.views.projects.create.OntologyComboboxFactory;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent.BioIcon;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent.SampleSourceType;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b>AddExperimentDialog</b>
 *
 * <p>Dialog to create an experiment information by providing the minimal required
 * information</p>
 *
 * @since 1.0.0
 */

public class AddExperimentDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 2142928219461555700L;

  private final Binder<ExperimentDraft> binder = new Binder<>();

  public AddExperimentDialog(
      OntologyLookupService ontologyTermInformationService) {
    requireNonNull(ontologyTermInformationService,
        "ontologyTermInformationService must not be null");
    OntologyComboboxFactory ontologyComboboxFactory = new OntologyComboboxFactory(
        ontologyTermInformationService);
    final BioIconComboboxFactory bioIconComboboxFactory = new BioIconComboboxFactory();

    initCancelShortcuts(this::onCreationCanceled);

    Span experimentHeader = new Span("Experiment");
    experimentHeader.addClassName("header");

    TextField experimentNameField = new TextField("Experiment Name");
    experimentNameField.addClassName("full-width-input");
    binder.forField(experimentNameField).asRequired("Please provide a name for the experiment")
        .bind(ExperimentDraft::getExperimentName, ExperimentDraft::setExperimentName);

    Span experimentDescription = new Span(
        "Please specify the sample origin information of the samples. Multiple "
            + "values are allowed!");

    MultiSelectComboBox<OntologyTerm> speciesBox = ontologyComboboxFactory.speciesBox();
    speciesBox.addClassName("box-flexgrow");
    binder.forField(speciesBox)
        .asRequired("Please select at least one species")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getSpecies()),
            ExperimentDraft::setSpecies);

    MultiSelectComboBox<OntologyTerm> specimenBox = ontologyComboboxFactory.specimenBox();
    specimenBox.addClassName("box-flexgrow");
    binder.forField(specimenBox)
        .asRequired("Please select at least one specimen")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getSpecimens()),
            ExperimentDraft::setSpecimens);

    MultiSelectComboBox<OntologyTerm> analyteBox = ontologyComboboxFactory.analyteBox();
    binder.forField(analyteBox)
        .asRequired("Please select at least one analyte")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getAnalytes()),
            ExperimentDraft::setAnalytes);

    ComboBox<BioIcon> speciesIconBox = bioIconComboboxFactory.iconBox(SampleSourceType.SPECIES,
    "Species icon");
    binder.forField(speciesIconBox)
        .bind(ExperimentDraft::getSpeciesIcon,
            ExperimentDraft::setSpeciesIcon);


    ComboBox<BioIcon> specimenIconBox = bioIconComboboxFactory.iconBox(SampleSourceType.SPECIMEN,
    "Specimen icon");
    binder.forField(specimenIconBox)
        .bind(ExperimentDraft::getSpecimenIcon,
            ExperimentDraft::setSpecimenIcon);

    addClassName("add-experiment-dialog");
    setHeaderTitle("Experimental Design");
    setConfirmButtonLabel("Add");
    setCancelButtonLabel("Cancel");

    Div speciesRow = new Div(speciesIconBox, speciesBox);
    speciesRow.addClassName("input-with-icon-selection");
    Div specimenRow = new Div(specimenIconBox, specimenBox);
    specimenRow.addClassName("input-with-icon-selection");

    Div container = new Div(experimentHeader,
        experimentDescription,
        experimentNameField,
        experimentDescription,
        speciesRow,
        specimenRow,
        analyteBox);
    container.addClassName("add-experiment-content");

    add(container);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    ExperimentDraft experimentDraft = new ExperimentDraft();
    boolean isValid = binder.writeBeanIfValid(experimentDraft);
    if (isValid) {
      fireEvent(new ExperimentAddEvent(this, clickEvent.isFromClient(), experimentDraft));
    }
  }

  private void onCreationCanceled() {
    CancelConfirmationNotificationDialog cancelDialog = new CancelConfirmationNotificationDialog()
        .withBodyText("You will lose all the information entered for this experiment.")
        .withConfirmText("Discard experiment creation")
        .withTitle("Discard new experiment creation?");
    cancelDialog.open();
    cancelDialog.addConfirmListener(event -> {
      cancelDialog.close();
      fireEvent(new CancelEvent(this, true));
    });
    cancelDialog.addCancelListener(
        event -> cancelDialog.close());
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    onCreationCanceled();
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

  public static class CancelEvent extends UserCancelEvent<AddExperimentDialog> {

    public CancelEvent(AddExperimentDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class ExperimentAddEvent extends ComponentEvent<AddExperimentDialog> {

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
    public ExperimentAddEvent(AddExperimentDialog source, boolean fromClient,
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
    private final List<OntologyTerm> species;
    private final List<OntologyTerm> specimen;
    private final List<OntologyTerm> analytes;
    private String speciesIconName;
    private String specimenIconName;

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
