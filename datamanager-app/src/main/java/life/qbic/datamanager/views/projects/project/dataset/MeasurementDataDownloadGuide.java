package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.TabSheet;
import life.qbic.datamanager.views.general.CodeBlock;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.io.Serializable;

/**
 * Explains how to obtain the data files, and offers the export of their locations.
 *
 * <p>The application does not serve the data. The files are produced by the facility, held by a
 * separate download service, and this application only knows that they exist and what they contain.
 * The dialog therefore does not pretend to offer a download: it offers the <em>locations</em> and
 * the three facts a user needs to use them — a personal access token, the exported list of URLs, and
 * a command to fetch them.</p>
 *
 * <p><b>Two scopes, one primary.</b> The dialog always offers the whole dataset, because a reader
 * who has selected nothing still needs a way to get all of the data — requiring a selection first
 * would be a dead end for the most common case ("I just want my files"). When a selection exists it
 * is offered <em>in addition</em>, as the secondary action, mirroring the measurement edit dialog:
 * a baseline that always works, plus the narrower convenience scope.</p>
 *
 * @since 1.21.0
 */
public class MeasurementDataDownloadGuide extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = -1L;

  private final Button exportAllButton = new Button("Export locations of all measurements");
  private final Button exportSelectedButton = new Button();
  /**
   * A real {@link RouterLink}, not an {@link com.vaadin.flow.component.html.Anchor} with a
   * hand-built href.
   *
   * <p>The previous version concatenated {@code "../settings/api-tokens"}, which produced a 404:
   * the application is deployed behind a servlet context path
   * ({@code server.servlet.context-path}), and a relative href resolved against the current route
   * knows nothing about it. A RouterLink asks the router for the URL, so the context path, the
   * route's own layout and any future route change are all handled for us.</p>
   */
  private final RouterLink tokenSettingsLink = new RouterLink();
  private final Span exportAllHint = new Span();
  private final Span exportSelectedHint = new Span();

  /**
   * The hidden anchor that performs the download.
   *
   * <p>It lives <em>inside</em> this dialog content on purpose. {@code AppDialog} is strictly
   * modal, so an anchor rendered in the page behind it is inert: the synthetic click the trigger
   * performs never reaches it and nothing downloads. Every working download-from-dialog component
   * in this application keeps its anchor in the dialog for the same reason.</p>
   */
  private final life.qbic.datamanager.views.general.download.DownloadComponent downloadComponent =
      new life.qbic.datamanager.views.general.download.DownloadComponent();

  public MeasurementDataDownloadGuide() {
    addClassName("measurement-dataset-download-guide");

    Span explanation = new Span(
        "The data files are held by the data download service, not by this application. Fetching "
            + "them takes three steps.");
    explanation.addClassName("measurement-dataset-download-guide-text");

    add(explanation, tokenStep(), exportStep(), commandStep(), downloadComponent);
  }

  /**
   * A step's headline: the ordinal, then what to do, in that order.
   *
   * <p>The steps are numbered because they are a genuine sequence: a token has to exist before the
   * locations are worth anything, and the locations have to be exported before the command has a
   * URL to fetch. Bare glyphs did not say that — a key, a download arrow and a terminal prompt are
   * only self-evident to someone who already knows the workflow, and the terminal icon in particular
   * reads as "developer only", which is the opposite of what a step-by-step guide is for.</p>
   *
   * <p>The ordinal is passed in rather than counted, so the numbering is stated where the step is
   * written and cannot drift if a step is ever moved or inserted.</p>
   *
   * @param ordinal the step's position in the sequence, starting at 1
   * @param title   what the reader has to do
   */
  private static Div stepHead(int ordinal, String title) {
    Div head = new Div();
    head.addClassName("measurement-dataset-download-guide-step-head");
    Span marker = new Span(Integer.toString(ordinal));
    marker.addClassName("measurement-dataset-download-guide-step-marker");
    Span titleSpan = new Span(title);
    titleSpan.addClassName("measurement-dataset-download-guide-step-title");
    head.add(marker, titleSpan);
    return head;
  }

  /**
   * Step 1: get a token. Without one the exported URLs cannot be fetched.
   *
   * <p>The step states the reason, not only the action: "create a personal access token" alone does
   * not tell a reader why a download needs an identity.</p>
   */
  private Div tokenStep() {
    tokenSettingsLink.setText("Create a personal access token");
    // Opens in a new tab, deliberately. The token is created elsewhere and the reader comes back
    // to finish the export; navigating this tab would discard the open dialog, and with it the
    // selection the export is scoped to. A new tab keeps the dialog exactly as it was.
    //
    // The target is set on the element rather than through an Anchor's href, so the URL is still
    // resolved by the router and therefore honours the servlet context path — an Anchor with a
    // hand-built href was what produced the original 404.
    tokenSettingsLink.getElement().setAttribute("target", "_blank");
    // Without this, the new tab can reach back into this window through window.opener.
    tokenSettingsLink.getElement().setAttribute("rel", "noopener noreferrer");
    tokenSettingsLink.addClassName("measurement-dataset-download-guide-link");

    Span explain = new Span("The download service asks for one, so the exported links work.");
    explain.addClassName("measurement-dataset-download-guide-step-detail");

    Div step = new Div(stepHead(1, "Get access"),
        explain, tokenSettingsLink);
    step.addClassName("measurement-dataset-download-guide-step");
    return step;
  }

  /**
   * Step 2: export the locations. One URL per measurement, as a text file.
   *
   * <p>Each button carries its own icon and no separate icon element. An earlier markup placed an
   * icon <em>span</em> beside a button that already had one, so the step rendered two download
   * glyphs in a row before its label.</p>
   */
  private Div exportStep() {
    Div actions = new Div();
    actions.addClassName("measurement-dataset-download-guide-actions");

    exportAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    exportAllButton.setIcon(VaadinIcon.DOWNLOAD.create());
    exportAllHint.setText("One URL per measurement in this dataset.");
    exportAllHint.addClassName("measurement-dataset-download-guide-hint");

    exportSelectedButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    exportSelectedButton.setIcon(VaadinIcon.DOWNLOAD.create());
    exportSelectedHint.addClassName("measurement-dataset-download-guide-hint");

    exportAllButton.addClickListener(event -> fireEvent(
        new ExportRequested(this, ExportScope.ALL, event.isFromClient())));
    exportSelectedButton.addClickListener(event -> fireEvent(
        new ExportRequested(this, ExportScope.SELECTED, event.isFromClient())));

    actions.add(actionRow(exportAllButton, exportAllHint),
        actionRow(exportSelectedButton, exportSelectedHint));

    Span explain = new Span("This writes a text file listing where each file lives.");
    explain.addClassName("measurement-dataset-download-guide-step-detail");

    Div step = new Div(stepHead(2, "Export the locations"), explain, actions);
    step.addClassName("measurement-dataset-download-guide-step");
    return step;
  }

  private static Div actionRow(Button button, Span hint) {
    Div row = new Div(button, hint);
    row.addClassName("measurement-dataset-download-guide-action");
    return row;
  }

  /**
   * Step 3: fetch the files, with the exported list.
   *
   * <p>Both tools are offered because which one a reader has is a property of their machine, not of
   * this application: a server usually has {@code curl}, a laptop often has {@code wget} only. They
   * are tabs rather than two stacked blocks so the step stays one line high, and each is a
   * {@link CodeBlock} — the same component the guidance used before this page was rebuilt — so the
   * command can be copied rather than retyped from a screenshot.</p>
   *
   * <p>The sentence above the tabs names the requirement the command encodes (a URL and a token),
   * because the command itself is a shape to apply to each line of the exported file rather than
   * something to paste blank.</p>
   */
  private Div commandStep() {
    TabSheet commands = new TabSheet();
    commands.addClassName("measurement-dataset-download-guide-commands");
    commands.add("curl", new CodeBlock("curl", "--parallel", "--fail", "-OJ", "-H",
        "\"Authorization: Bearer <TOKEN>\"", "<URL>"));
    commands.add("wget", new CodeBlock("wget", "--content-disposition", "--trust-server-names",
        "--header", "\"Authorization: Bearer <TOKEN>\"", "<URL>"));

    Div content = new Div(commands);
    content.addClassName("measurement-dataset-download-guide-command-wrap");

    Span explain = new Span("Run this once for each line of the exported file, substituting "
        + "<URL> and your token. Either tool works; pick the one you have.");
    explain.addClassName("measurement-dataset-download-guide-step-detail");

    Div step = new Div(stepHead(3, "Fetch the files"), explain, content);
    step.addClassName("measurement-dataset-download-guide-step");
    return step;
  }

  /**
   * Points the token step at the view where a token is created.
   *
   * <p>Takes the navigation target rather than a URL: the router resolves the path, so the link
   * works regardless of the context path the application is deployed under.</p>
   *
   * @param navigationTarget the route to open
   */
  public void setTokenSettingsTarget(
      Class<? extends com.vaadin.flow.component.Component> navigationTarget) {
    tokenSettingsLink.setRoute(navigationTarget);
  }

  /**
   * States what the selection-scoped export would cover, and hides it when there is no selection.
   *
   * <p>The whole-dataset export is always offered: a reader who selected nothing still needs a way
   * to get all of the data, and making that conditional would leave the dialog with no working
   * action in its most common state.</p>
   *
   * @param selectedCount the number of selected measurements, {@code 0} when none
   */
  /**
   * Renames the whole-dataset export, for a surface that already scopes it to one dataset.
   *
   * <p>On the experiment's Datasets page "all measurements" means all of the experiment's — the
   * dataset there is a filter the reader may have widened. On the project page the guide is opened
   * for a <em>single row</em>, so "all measurements" would describe a scope wider than the one the
   * dialog actually offers. The caller names its own scope; the default stays correct for the page
   * this dialog was written for.</p>
   *
   * @param label the button text
   * @param hint  the line under it, or {@code null} to leave the default
   */
  public void setScopeLabel(String label, String hint) {
    if (label != null && !label.isBlank()) {
      exportAllButton.setText(label);
    }
    if (hint != null) {
      exportAllHint.setText(hint);
    }
  }

  public void setSelectionCount(int selectedCount) {
    if (selectedCount <= 0) {
      exportSelectedButton.setVisible(false);
      exportSelectedHint.setVisible(false);
      exportSelectedHint.setText("");
      return;
    }
    exportSelectedButton.setVisible(true);
    exportSelectedButton.setText(selectedCount == 1
        ? "Export location of the selected measurement"
        : "Export locations of the %d selected measurements".formatted(selectedCount));
    exportSelectedHint.setVisible(true);
    exportSelectedHint.setText("Only the measurements you selected.");
  }

  /**
   * Registers the handler for a location export.
   *
   * @param listener invoked with the scope the reader asked for
   * @return a registration to remove it again
   */
  public Registration addExportRequestedListener(
      ComponentEventListener<ExportRequested> listener) {
    return addListener(ExportRequested.class, listener);
  }

  /**
   * Triggers a download from within the open dialog.
   *
   * @param provider the content to download
   */
  public void triggerDownload(
      life.qbic.datamanager.files.export.download.DownloadStreamProvider provider) {
    downloadComponent.trigger(provider);
  }

  /** Which measurements a location export covers. */
  public enum ExportScope {
    /** Every measurement in the dataset currently shown, independent of any selection. */
    ALL,
    /** Exactly the selected measurements. */
    SELECTED
  }

  /** Fired when the reader asks for a set of locations. */
  public static class ExportRequested extends ComponentEvent<MeasurementDataDownloadGuide> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ExportScope scope;

    public ExportRequested(MeasurementDataDownloadGuide source, ExportScope scope,
        boolean fromClient) {
      super(source, fromClient);
      this.scope = scope;
    }

    /** @return the scope the reader asked for */
    public ExportScope scope() {
      return scope;
    }
  }
}
