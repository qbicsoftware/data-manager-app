package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.io.Serializable;

/**
 * Explains how to obtain the data files of a measurement, and offers the export of their locations.
 *
 * <p>The application does not serve the data. The files are produced by the facility, held by a
 * separate download service, and this application only knows that they exist and what they contain.
 * The panel therefore does not pretend to offer a download: it offers the <em>locations</em> and the
 * three facts a user needs to use them — a personal access token, the exported list of URLs, and a
 * command to fetch them.</p>
 *
 * <p>The guidance is shown on request, in a dialog opened from the list's actions. It is
 * deliberately not a permanent band: rendered at the bottom of a full grid it read as a footnote
 * and sat below the fold, and the retired raw-data page spent a fifth of the screen on the same
 * content in a side rail that went stale whenever the download service changed. A reader who needs
 * it asks for it; everybody else never sees it.</p>
 *
 * @since 1.21.0
 */
public class MeasurementDataDownloadGuide extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = -1L;

  private final Button exportLocationsButton =
      new Button("Export dataset locations", VaadinIcon.DOWNLOAD.create());
  private final Anchor tokenSettingsLink = new Anchor();

  public MeasurementDataDownloadGuide() {
    addClassName("measurement-dataset-download-guide");

    Span title = new Span("Getting the data");
    title.addClassName("measurement-dataset-download-guide-title");

    Span explanation = new Span(
        "Data files are held by the data download service, not by this application. This list tells "
            + "you which files exist and where they are; to fetch them, export the locations and "
            + "download them with a personal access token.");
    explanation.addClassName("measurement-dataset-download-guide-text");

    Span tokenLabel = new Span(VaadinIcon.KEY.create());
    tokenLabel.addClassName("measurement-dataset-download-guide-step-icon");
    tokenSettingsLink.setText("Create a personal access token");
    tokenSettingsLink.setTarget(AnchorTarget.BLANK);
    tokenSettingsLink.addClassName("measurement-dataset-download-guide-link");
    Div tokenStep = new Div(tokenLabel, tokenSettingsLink);
    tokenStep.addClassName("measurement-dataset-download-guide-step");

    Span exportLabel = new Span(VaadinIcon.DOWNLOAD.create());
    exportLabel.addClassName("measurement-dataset-download-guide-step-icon");
    exportLocationsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    exportLocationsButton.addClassName("measurement-dataset-download-guide-button");
    // A real button, not link-styled text welded to a sentence: the previous markup rendered the
    // icon on top of the label and produced an unreadable "Export dataset locations of the
    // measurements you selected."
    Span exportHint = new Span("for the measurements you selected");
    exportHint.addClassName("measurement-dataset-download-guide-hint");
    Div exportStep = new Div(exportLabel, exportLocationsButton, exportHint);
    exportStep.addClassName("measurement-dataset-download-guide-step");

    Span commandLabel = new Span(VaadinIcon.TERMINAL.create());
    commandLabel.addClassName("measurement-dataset-download-guide-step-icon");
    Span command = new Span("curl --parallel --fail -OJ -H \"Authorization: Bearer <TOKEN>\" <URL>");
    command.addClassNames("measurement-dataset-download-guide-command", "identifier");
    Div commandStep = new Div(commandLabel, command);
    commandStep.addClassName("measurement-dataset-download-guide-step");

    add(title, explanation, tokenStep, exportStep, commandStep);
  }

  /**
   * Points the token step at the place a token is created.
   *
   * @param href the settings route, as an href
   */
  public void setTokenSettingsHref(String href) {
    tokenSettingsLink.setHref(href);
  }

  /**
   * Registers the handler for the location export.
   *
   * @param listener invoked when the user asks for the locations
   */
  public void addExportLocationsListener(
      com.vaadin.flow.component.ComponentEventListener<com.vaadin.flow.component.ClickEvent<Button>> listener) {
    exportLocationsButton.addClickListener(listener);
  }

  /**
   * Enables or disables the export, which needs a selection to act on.
   *
   * @param enabled whether a selection exists
   */
  public void setExportEnabled(boolean enabled) {
    exportLocationsButton.setEnabled(enabled);
  }
}
