package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static java.util.Objects.requireNonNull;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.ANALYTE;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.SPECIES;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.SPECIMEN;

import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.projects.create.OntologyComboboxFactory;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * Inline experiment design form: the experiment name and its sample origins (species, specimen,
 * analyte).
 *
 * <p>Extracted from the former {@code AddExperimentDialog} / {@code EditExperimentDialog} so the
 * create route and the design route can host it directly. The binding and the used-origin
 * protection are identical; the only behaviour change is that a used origin that the user tries to
 * remove now surfaces as an inline field error instead of an {@code AlertDialog} (project owner,
 * 2026-10-19: editing is inline, not in dialogs).</p>
 *
 * <p><b>Transparency.</b> The form states what the sample origins are for and what editing them
 * does: they become the controlled options in the sample-registration template, a newly added term
 * applies to new samples, and a term already referenced by a registered sample cannot be removed.
 * The terms in use are also named per field, with how many samples reference them, so the lock is a
 * stated dependency rather than an unexplained padlock.</p>
 *
 * @since 1.19.0
 */
public class ExperimentDesignForm extends Div {

  private final Binder<ExperimentDraft> binder = new Binder<>();
  private final TextField experimentNameField = new TextField("Experiment Name");
  private final MultiSelectComboBox<OntologyTerm> speciesBox;
  private final MultiSelectComboBox<OntologyTerm> specimenBox;
  private final MultiSelectComboBox<OntologyTerm> analyteBox;
  private final Span speciesUsage = originUsageNote();
  private final Span specimenUsage = originUsageNote();
  private final Span analyteUsage = originUsageNote();
  /**
   * Per origin type, the terms that registered samples already reference and how many samples
   * reference each. Empty when the experiment has no samples.
   */
  private final Map<SampleOriginType, Map<OntologyTerm, Long>> usedOriginCounts =
      new EnumMap<>(SampleOriginType.class);

  public ExperimentDesignForm(SpeciesLookupService speciesLookupService,
      TerminologyService terminologyService) {
    var factory = new OntologyComboboxFactory(requireNonNull(speciesLookupService),
        requireNonNull(terminologyService));
    speciesBox = factory.speciesBox();
    specimenBox = factory.specimenBox();
    analyteBox = factory.analyteBox();

    addClassName("experiment-design-form");
    experimentNameField.addClassName("full-width-input");
    binder.forField(experimentNameField)
        .asRequired("Please provide a name for the experiment")
        .bind(ExperimentDraft::getExperimentName, ExperimentDraft::setExperimentName);

    speciesBox.addClassName("box-flexgrow");
    binder.forField(speciesBox)
        .asRequired("Please select at least one species")
        .bind(draft -> new HashSet<>(draft.getSpecies()), ExperimentDraft::setSpecies);

    specimenBox.addClassName("box-flexgrow");
    binder.forField(specimenBox)
        .asRequired("Please select at least one specimen")
        .bind(draft -> new HashSet<>(draft.getSpecimens()), ExperimentDraft::setSpecimens);

    binder.forField(analyteBox)
        .asRequired("Please select at least one analyte")
        .bind(draft -> new HashSet<>(draft.getAnalytes()), ExperimentDraft::setAnalytes);

    guardUsedOriginRemoval(speciesBox, SPECIES);
    guardUsedOriginRemoval(specimenBox, SPECIMEN);
    guardUsedOriginRemoval(analyteBox, ANALYTE);

    Span contract = new Span(
        "Adding a term makes it available for new samples. Terms already used by a registered "
            + "sample are locked, and existing samples keep their annotation.");
    contract.addClassName("experiment-design-contract");

    Div speciesRow = new Div(speciesBox);
    speciesRow.addClassName("input-with-icon-selection");
    Div specimenRow = new Div(specimenBox);
    specimenRow.addClassName("input-with-icon-selection");

    add(experimentNameField,
        speciesRow, speciesUsage,
        specimenRow, specimenUsage,
        analyteBox, analyteUsage,
        contract);
    setDraft(new ExperimentDraft());
  }

  /** Loads a draft into the form. */
  public void setDraft(ExperimentDraft draft) {
    binder.setBean(requireNonNull(draft, "draft must not be null"));
  }

  /** Clears the form back to an empty draft. */
  public void reset() {
    binder.setBean(new ExperimentDraft());
  }

  /**
   * Writes the form into a new draft, or returns {@link Optional#empty()} if the form is invalid.
   *
   * <p>On failure the invalid fields are left marked (that is what {@code writeBeanIfValid} does), so
   * the caller can surface the problem — see {@link #focusFirstInvalidField()}.</p>
   */
  public Optional<ExperimentDraft> writeDraftIfValid() {
    ExperimentDraft draft = new ExperimentDraft();
    return binder.writeBeanIfValid(draft) ? Optional.of(draft) : Optional.empty();
  }

  /**
   * Moves focus to the first field that is currently invalid, so a refused submit takes the user to
   * the field that needs attention instead of leaving them to find it.
   */
  public void focusFirstInvalidField() {
    binder.validate().getFieldValidationErrors().stream()
        .map(BindingValidationStatus::getField)
        .filter(Focusable.class::isInstance)
        .findFirst()
        .ifPresent(field -> ((Focusable<?>) field).focus());
  }

  /**
   * Marks the sample origins already referenced by registered samples, with the number of samples
   * behind each term, so they cannot be removed and the UI can say why.
   */
  public void setUsedSampleOrigins(
      Map<SampleOriginType, Map<OntologyTerm, Long>> usedOriginCounts) {
    this.usedOriginCounts.clear();
    if (usedOriginCounts != null) {
      this.usedOriginCounts.putAll(usedOriginCounts);
    }
    renderUsage(speciesUsage, SPECIES);
    renderUsage(specimenUsage, SPECIMEN);
    renderUsage(analyteUsage, ANALYTE);
  }

  private void renderUsage(Span note, SampleOriginType type) {
    Map<OntologyTerm, Long> counts = usedOriginCounts.getOrDefault(type, Map.of());
    if (counts.isEmpty()) {
      note.setText("");
      note.setVisible(false);
      return;
    }
    String terms = counts.entrySet().stream()
        .sorted(Map.Entry.comparingByKey(Comparator.comparing(OntologyTerm::getLabel)))
        .map(entry -> entry.getKey().getLabel() + " (" + entry.getValue()
            + (entry.getValue() == 1 ? " sample)" : " samples)"))
        .collect(Collectors.joining(", "));
    note.setText("In use: " + terms);
    note.setVisible(true);
  }

  private Set<OntologyTerm> usedTerms(SampleOriginType type) {
    return usedOriginCounts.getOrDefault(type, Map.of()).keySet();
  }

  private void guardUsedOriginRemoval(MultiSelectComboBox<OntologyTerm> box,
      SampleOriginType type) {
    box.addValueChangeListener(event -> {
      if (!event.isFromClient()) {
        return;
      }
      Set<OntologyTerm> missing = usedTerms(type).stream()
          .filter(term -> !event.getValue().contains(term))
          .collect(Collectors.toSet());
      if (missing.isEmpty()) {
        box.setInvalid(false);
        box.setErrorMessage(null);
        return;
      }
      Set<OntologyTerm> restored = new HashSet<>(event.getValue());
      restored.addAll(missing);
      box.setValue(restored);
      box.setInvalid(true);
      box.setErrorMessage("Cannot remove %s: samples of this experiment use it.".formatted(
          missing.stream().map(OntologyTerm::getLabel).collect(Collectors.joining(", "))));
    });
  }

  private static Span originUsageNote() {
    Span note = new Span();
    note.addClassName("experiment-design-usage");
    note.setVisible(false);
    return note;
  }
}
