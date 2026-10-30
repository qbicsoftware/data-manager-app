package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.measurement.MeasurementService.MeasurementDeletionException;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;

/**
 * The actions of the Dataset page: export the metadata, edit it, delete it.
 *
 * <p>All three act on the cross-page selection owned by the grid. Registration is deliberately not
 * here: it creates measurements rather than acting on existing ones, and it stays a page-level
 * action.</p>
 *
 * <p><b>Deletion is restricted to measurements without data.</b> The service refuses to delete a
 * measurement whose data is attached ({@code DATA_ATTACHED}), and removing that data is owned by
 * the data management team, not by this application. The page therefore refuses the whole action up
 * front and says who can resolve it, rather than letting a partial deletion succeed and reporting a
 * confusing half-result.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>This class subscribes to reactive sources and its callbacks therefore run on the shared
 * scheduler thread, never on a Vaadin UI thread. <b>It must not touch the UI</b>: its callbacks
 * hand their result to the caller, and the caller re-enters the UI thread through the page's
 * {@code UiHandle} adapter. Opening a toast or triggering a download here would fail at runtime
 * with "No currently active UI found", and only when the path is exercised.</p>
 *
 * @since 1.21.0
 */
public class MeasurementDatasetActions implements Serializable {

  /**
   * Marks this class as a reactive consumer that deliberately performs no UI work.
   *
   * <p>Referenced by {@code MeasurementDatasetThreadingSpec}: the spec requires every file that
   * subscribes to a reactive source to either route UI work through the shared adapter or to
   * declare that it does none. Declaring it here is cheaper and more honest than adding an adapter
   * this class does not need — and it keeps the invariant visible to whoever edits it next.</p>
   */
  static final boolean PERFORMS_NO_UI_WORK = true;

  @Serial
  private static final long serialVersionUID = -1L;

  private static final Logger log = LoggerFactory.logger(MeasurementDatasetActions.class);

  private static final MimeType OPEN_XML = MimeType.valueOf(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final transient AsyncProjectService asyncService;
  private final transient MeasurementService measurementService;
  private final String projectId;

  private final Button exportButton = new Button("Export", VaadinIcon.DOWNLOAD.create());
  private final Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
  private final Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());

  /**
   * Opens the guidance on how to fetch the data. It sits with the other actions because that is
   * where a reader looks for it; a band below the list was neither findable nor in place.
   */
  // LINK, not another download glyph: "Get data" sits beside "Export", and two adjacent download
  // arrows read as the same action twice. This one opens the guidance on *where* the files are,
  // so it points at a destination rather than promising a download this page does not perform.
  private final Button getDataButton = new Button("Get data", VaadinIcon.LINK.create());

  /**
   * @param asyncService       the service the export and update calls go through
   * @param measurementService the service deletions go through
   * @param projectId          the project whose measurements are acted on
   */
  public MeasurementDatasetActions(AsyncProjectService asyncService,
      MeasurementService measurementService, String projectId) {
    this.asyncService = Objects.requireNonNull(asyncService);
    this.measurementService = Objects.requireNonNull(measurementService);
    this.projectId = Objects.requireNonNull(projectId);

    exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
  }

  /** @return the export action, for the page's toolbar */
  public Button exportButton() {
    return exportButton;
  }

  /** @return the edit action, for the page's toolbar */
  public Button editButton() {
    return editButton;
  }

  /** @return the delete action, for the page's toolbar */
  public Button deleteButton() {
    return deleteButton;
  }

  /** @return the action that explains how to obtain the data files */
  public Button getDataButton() {
    return getDataButton;
  }

  /**
   * Applies the current selection to the action buttons.
   *
   * <p>Edit stays enabled without a selection: its dialog offers the metadata of the whole dataset
   * as well as the selection, exactly as the measurement edit dialog always has. Export and delete
   * need something to act on.</p>
   *
   * @param selection the current selection
   * @param canWrite  whether the user may mutate this project
   */
  public void applySelection(MeasurementSelection selection, boolean canWrite) {
    boolean hasSelection = !selection.isEmpty();
    exportButton.setVisible(canWrite);
    exportButton.setEnabled(hasSelection);
    editButton.setVisible(canWrite);
    editButton.setEnabled(canWrite);
    deleteButton.setVisible(canWrite);
    deleteButton.setEnabled(hasSelection);
    // Reading the data is not a mutation, so it is offered to every reader of the project, with or
    // without a selection: the guidance itself is useful before anything is selected.
    getDataButton.setVisible(true);
    getDataButton.setEnabled(true);
  }

  /**
   * Downloads the metadata of the selection as an edit workbook.
   *
   * <p>Produced by the same service call the retired measurement page used, so the sheet layout
   * users already know is unchanged.</p>
   *
   * @param selection the selection to export
   * @param download  receives the produced document
   * @param onFailure called when the export could not be produced
   */
  public void exportMetadata(MeasurementSelection selection, Consumer<DigitalObject> download,
      Runnable onFailure) {
    Mono<DigitalObject> request = exportRequestFor(selection);
    if (request == null) {
      return;
    }
    request.subscribe(download, error -> {
      log.error("Could not export measurement metadata", error);
      onFailure.run();
    });
  }

  /**
   * Builds the export request for a selection.
   *
   * <p>The service produces one workbook per measurement method, and the page shows one dataset at
   * a time, so a single-method selection is the normal case. A mixed selection exports the first
   * method present rather than merging two different sheet layouts into one file, which would be a
   * workbook no template describes.</p>
   *
   * @return the request, or {@code null} when nothing is selected
   */
  private Mono<DigitalObject> exportRequestFor(MeasurementSelection selection) {
    if (!selection.genomics().isEmpty()) {
      return asyncService.measurementUpdateNGS(projectId, selection.genomics(), OPEN_XML);
    }
    if (!selection.proteomics().isEmpty()) {
      return asyncService.measurementUpdatePxP(projectId, selection.proteomics(), OPEN_XML);
    }
    if (!selection.immunopeptidomics().isEmpty()) {
      return asyncService.measurementUpdateIP(projectId, selection.immunopeptidomics(), OPEN_XML);
    }
    return null;
  }

  /**
   * Opens the delete confirmation, refusing up front when any selected measurement has data.
   *
   * @param selection the selection to delete
   * @param onConfirm invoked with the selection when the user confirms
   * @return the dialog to open, or {@code null} when nothing is selected
   */
  public AppDialog deleteDialog(Component parent, MeasurementSelection selection,
      Consumer<MeasurementSelection> onConfirm) {
    if (selection.isEmpty()) {
      return null;
    }
    if (!selection.isDeletable()) {
      return blockedDeleteDialog(selection);
    }
    // AlertDialog.danger, not a plain AppDialog: deletion is destructive and irreversible, and the
    // shared alert gives it a warning icon and a red confirm button. Assembling an AppDialog by hand
    // here produced a dialog that looked like any other confirmation, with a primary-coloured
    // "Delete" — the one action on this page that must not look ordinary.
    return AlertDialog.danger(parent,
        selection.total() == 1
            ? "Delete measurement"
            : "Delete %d measurements".formatted(selection.total()),
        selection.total() == 1
            ? "This measurement's metadata will be deleted and its measurement code released. "
                + "This cannot be undone."
            : "The metadata of these %d measurements will be deleted and their measurement codes "
                + "released. This cannot be undone.".formatted(selection.total()),
        selection.total() == 1 ? "Delete measurement" : "Delete %d measurements".formatted(
            selection.total()),
        "Keep measurements",
        () -> onConfirm.accept(selection)).dialog();
  }

  /**
   * Explains why a selection with attached data cannot be deleted.
   *
   * <p>The previous copy told users to remove the data first — a step this application cannot
   * perform. Naming who owns that step is more useful than pointing at a button that does not
   * exist.</p>
   */
  /**
   * Explains why a selection with attached data cannot be deleted.
   *
   * <p>Uses the shared alert with a warning intent: the reader asked for a destructive action and
   * is being told it is blocked, so the dialog belongs to the same family as the confirmation —
   * but with a single acknowledgement instead of a red confirm, because there is nothing to
   * confirm.</p>
   *
   * <p>The previous copy told users to remove the data first — a step this application cannot
   * perform. Naming who owns that step is more useful than pointing at a button that does not
   * exist.</p>
   */
  private AppDialog blockedDeleteDialog(MeasurementSelection selection) {
    int blocked = selection.withData();
    String message = (blocked == 1
        ? "1 of the selected measurements already has data registered. Measurement metadata can "
            + "only be deleted while no data is attached to it. "
        : "%d of the selected measurements already have data registered. Measurement metadata can "
            + "only be deleted while no data is attached to it. ".formatted(blocked))
        + "Data removal is handled by the data management team, so please contact your data "
        + "manager to have the data removed first.";
    return AlertDialog.alert(null)
        .warning()
        .title("These measurements cannot be deleted")
        .message(message)
        .confirmButton("Close", () -> {
          // acknowledge only
        })
        .build()
        .dialog();
  }

  /**
   * Deletes the selection, dispatching each measurement to the deletion of its own method.
   *
   * <p>A selection can span methods and the service deletes per method, so this makes one call per
   * method present and reports a single aggregate outcome to the view.</p>
   *
   * @param selection the measurements to delete
   * @param onSuccess invoked with the number of measurements deleted
   * @param onFailure invoked when any part of the deletion failed
   */
  public void delete(MeasurementSelection selection, IntConsumer onSuccess, Runnable onFailure) {
    if (selection.isEmpty()) {
      return;
    }
    ProjectId parsedProjectId = ProjectId.parse(projectId);
    int requested = selection.total();
    int failedCalls = 0;
    if (!selection.genomics().isEmpty()) {
      failedCalls += deleteAndCountFailures(
          measurementService.deleteNgsMeasurements(parsedProjectId,
              Set.copyOf(selection.genomics())));
    }
    if (!selection.proteomics().isEmpty()) {
      failedCalls += deleteAndCountFailures(
          measurementService.deletePxpMeasurements(parsedProjectId,
              Set.copyOf(selection.proteomics())));
    }
    if (!selection.immunopeptidomics().isEmpty()) {
      failedCalls += deleteAndCountFailures(
          measurementService.deleteIpMeasurements(parsedProjectId,
              Set.copyOf(selection.immunopeptidomics())));
    }
    if (failedCalls > 0) {
      onFailure.run();
      return;
    }
    onSuccess.accept(requested);
  }

  /**
   * Reports whether one method's deletion failed.
   *
   * <p>Called only for a non-empty id set, because the service dispatches a project-change event
   * even for an empty one — a write with nothing written.</p>
   */
  private int deleteAndCountFailures(Result<Void, MeasurementDeletionException> result) {
    if (result.isError()) {
      log.error("Measurement deletion failed: " + result.getError().reason());
      return 1;
    }
    return 0;
  }
}
