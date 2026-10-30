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
        "The data files are held by the data download service, not by this application. This list "
            + "tells you which measurements have data; to fetch the files, export their locations "
            + "and download them with a personal access token.");
    explanation.addClassName("measurement-dataset-download-guide-text");

    add(explanation, tokenStep(), exportStep(), commandStep(), downloadComponent);
  }

  private Div tokenStep() {
    Span icon = new Span(VaadinIcon.KEY.create());
    icon.addClassName("measurement-dataset-download-guide-step-icon");
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
    Div step = new Div(icon, tokenSettingsLink);
    step.addClassName("measurement-dataset-download-guide-step");
    return step;
  }

  /**
   * The export step: one baseline action and, when a selection exists, a narrower second one.
   *
   * <p>Each button carries its own icon and no separate icon element. The previous markup placed an
   * icon <em>span</em> beside a button that already had one, so the step rendered two download
   * glyphs in a row before its label.</p>
   */
  private Div exportStep() {
    Span icon = new Span(VaadinIcon.DOWNLOAD.create());
    icon.addClassName("measurement-dataset-download-guide-step-icon");

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

    Div step = new Div(icon, actions);
    step.addClassName("measurement-dataset-download-guide-step");
    return step;
  }

  private static Div actionRow(Button button, Span hint) {
    Div row = new Div(button, hint);
    row.addClassName("measurement-dataset-download-guide-action");
    return row;
  }

  /**
   * The download commands, one per tool.
   *
   * <p>Both are offered because which one a reader has is a property of their machine, not of this
   * application: a server usually has {@code curl}, a laptop often has {@code wget} only. They are
   * tabs rather than two stacked blocks so the step stays one line high, and each is a
   * {@link CodeBlock} — the same component the download guidance used before this page was rebuilt —
   * so the command can be copied rather than retyped from a screenshot.</p>
   *
   * <p>The {@code <URL>} placeholder is deliberate: the locations are exported as a file, so the
   * command is a shape to apply to each line, not a command that could be pasted verbatim.</p>
   */
  private Div commandStep() {
    Span icon = new Span(VaadinIcon.TERMINAL.create());
    icon.addClassName("measurement-dataset-download-guide-step-icon");

    TabSheet commands = new TabSheet();
    commands.addClassName("measurement-dataset-download-guide-commands");
    commands.add("curl", new CodeBlock("curl", "--parallel", "--fail", "-OJ", "-H",
        "\"Authorization: Bearer <TOKEN>\"", "<URL>"));
    commands.add("wget", new CodeBlock("wget", "--content-disposition", "--trust-server-names",
        "--header", "\"Authorization: Bearer <TOKEN>\"", "<URL>"));

    Div content = new Div(commands);
    content.addClassName("measurement-dataset-download-guide-command-wrap");
    Div step = new Div(icon, content);
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
