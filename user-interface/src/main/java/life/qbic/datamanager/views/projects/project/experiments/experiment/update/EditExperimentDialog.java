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
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.events.UserCancelEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.create.OntologyComboboxFactory;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b>EditExperimentDialog</b>
 *
 * <p>Dialog to edit experiment information by providing the minimal required
 * information</p>
 *
 * @since 1.0.0
 */

public class EditExperimentDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 2142928219461555700L;
  private final Binder<ExperimentDraft> binder = new Binder<>();

  public EditExperimentDialog(OntologyLookupService ontologyTermInformationService) {
    OntologyComboboxFactory ontologyComboboxFactory = new OntologyComboboxFactory(
        ontologyTermInformationService);

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
    binder.forField(speciesBox)
        .asRequired("Please select at least one species")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getSpecies()),
            ExperimentDraft::setSpecies);

    MultiSelectComboBox<OntologyTerm> specimenBox = ontologyComboboxFactory.specimenBox();
    binder.forField(specimenBox)
        .asRequired("Please select at least one specimen")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getSpecimens()),
            ExperimentDraft::setSpecimens);

    MultiSelectComboBox<OntologyTerm> analyteBox = ontologyComboboxFactory.analyteBox();
    binder.forField(analyteBox)
        .asRequired("Please select at least one analyte")
        .bind(experimentDraft -> new HashSet<>(experimentDraft.getAnalytes()),
            ExperimentDraft::setAnalytes);

    addClassName("edit-experiment-dialog");
    setHeaderTitle("Experimental Design");
    setConfirmButtonLabel("Save");
    setCancelButtonLabel("Cancel");

    Div editExperimentContent = new Div();
    editExperimentContent.addClassName("edit-experiment-content");
    editExperimentContent.add(experimentHeader,
        experimentDescription,
        experimentNameField,
        experimentDescription,
        speciesBox,
        specimenBox,
        analyteBox);
    add(editExperimentContent);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    ExperimentDraft experimentDraft = new ExperimentDraft();
    ExperimentDraft oldDraft = binder.getBean();
    boolean isValid = binder.writeBeanIfValid(experimentDraft);
    if (isValid) {
      fireEvent(
          new ExperimentUpdateEvent(this, clickEvent.isFromClient(), oldDraft, experimentDraft));
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

  public void addExperimentUpdateEventListener(
      ComponentEventListener<ExperimentUpdateEvent> listener) {
    addListener(ExperimentUpdateEvent.class, listener);
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public static class CancelEvent extends UserCancelEvent<EditExperimentDialog> {

    public CancelEvent(EditExperimentDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class ExperimentUpdateEvent extends ComponentEvent<EditExperimentDialog> {

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
    public ExperimentUpdateEvent(EditExperimentDialog source, boolean fromClient,
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

  public static class ExperimentDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = 5584396740927480418L;

    private String experimentName;
    private final List<OntologyTerm> species;
    private final List<OntologyTerm> specimen;
    private final List<OntologyTerm> analytes;

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
