package life.qbic.datamanager.views.projects.project.measurements;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.function.Supplier;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import reactor.core.publisher.Mono;

/**
 * Measurement Template Component
 * <p>
 * A simple component that can be configured with a {@link Mono} of
 * {@link life.qbic.projectmanagement.application.api.fair.DigitalObject} to download measurement
 * information.
 * <p>
 * The component comes with a simple description displayed to the user to provide context for the
 * download action.
 * <p>
 * The component also provides a button for the user to interact with, and a click will trigger the
 * download of the {@link life.qbic.projectmanagement.application.api.fair.DigitalObject} by calling
 * {@link Mono#subscribe()}.
 * <p>
 * <strong>Note: </strong> The download in the client will only work, if the
 * {@link MeasurementTemplateComponent} is attached to a {@link com.vaadin.flow.component.UI}.
 *
 * @since 1.11.0
 */
public class MeasurementTemplateComponent extends Div {

  private final MessageSourceNotificationFactory messageFactory;

  private final DownloadComponent downloadComponent;
  private final Supplier<String> projectIdSupplier;

  public MeasurementTemplateComponent(
      String description, String buttonText,
      Supplier<Mono<DigitalObject>> templateSupplier,
      MessageSourceNotificationFactory messageFactory,
      Supplier<String> projectIdSupplier
      ) {
    requireNonNull(description);
    requireNonNull(buttonText);
    requireNonNull(templateSupplier);
    requireNonNull(messageFactory);
    requireNonNull(projectIdSupplier);
    this.messageFactory = messageFactory;
    this.downloadComponent = new DownloadComponent();
    this.projectIdSupplier = projectIdSupplier;

    addClassNames("padding-left-right-05", "padding-top-bottom-05", "choice-box", "flex-vertical",
        "gap-03");
    var descriptionElement = new Div(description);
    descriptionElement.addClassNames("normal-body-text");

    var inProgressToast = messageFactory.pendingTaskToast("measurement.preparing-download",
        MessageSourceNotificationFactory.EMPTY_PARAMETERS, getLocale());
    var downloadButton = new Button(buttonText, e -> { // TODO implement
      templateSupplier.get()
          .doOnSubscribe(ignored -> openToast(inProgressToast))
          .doOnSuccess(this::triggerDownload).doOnTerminate(() -> closeToast(inProgressToast))
          .subscribe();
    });
    var buttonElement = new Div(downloadButton);

    add(descriptionElement, buttonElement, downloadComponent);
  }

  private void closeToast(Toast toast) {
    getUI().ifPresent(ui -> ui.access(toast::close));
  }

  private void openToast(Toast toast) {
    getUI().ifPresent(ui -> ui.access(toast::open));
  }

  private void triggerDownload(DigitalObject digitalObject) {
    getUI().ifPresent(ui -> ui.access(() -> {
      downloadComponent.trigger(new DownloadStreamProvider() {
        @Override
        public String getFilename() {
          var projectId = projectIdSupplier.get();
          return FileNameFormatter.formatWithTimestampedSimple(LocalDate.now(), projectId, "measurements", "xlsx");
        }

        @Override
        public InputStream getStream() {
          return digitalObject.content();
        }
      });
    }));
  }

}
