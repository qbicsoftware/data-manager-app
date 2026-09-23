package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import org.jspecify.annotations.Nullable;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

/**
 * <b>Sample Template Component</b>
 * <p>
 * A component that offers one or more sample metadata export actions, each backed by a lazy
 * {@link Mono} of a {@link DigitalObject}. The primary export is configured in the constructor
 * (e.g. "Download all samples"); additional scoped exports (e.g. "Download selected (N)") can be
 * appended via {@link #addTemplateExport(String, Mono, Supplier)}.
 * <p>
 * Each export may carry a {@link Supplier} of the number of samples it will export, used to populate
 * the "fetching metadata for {0} samples" progress toast. When the supplier is <code>null</code> a
 * generic "collecting information" toast is shown instead (e.g. for a blank registration template).
 * <p>
 * <strong>Note:</strong> the download in the client will only work if the component is attached to a
 * {@link com.vaadin.flow.component.UI}.
 *
 * @since 1.11.0
 */
public class SampleTemplateComponent extends Div {

  private final DownloadComponent downloadComponent;
  private final Supplier<String> projectCodeSupplier;
  private final transient MessageSourceNotificationFactory messageFactory;
  private final Div buttonContainer = new Div();

  public SampleTemplateComponent(
      String description, String buttonText,
      Mono<DigitalObject> templateMono,
      MessageSourceNotificationFactory messageFactory,
      Supplier<String> projectCodeSupplier,
      @Nullable Supplier<Integer> sampleCountSupplier
  ) {
    requireNonNull(description);
    requireNonNull(buttonText);
    requireNonNull(templateMono);
    requireNonNull(messageFactory);
    requireNonNull(projectCodeSupplier);
    this.downloadComponent = new DownloadComponent();
    this.projectCodeSupplier = projectCodeSupplier;
    this.messageFactory = messageFactory;

    addClassNames("padding-horizontal-05", "padding-vertical-05", "border", "rounded-02",
        "flex-vertical", "gap-03");
    var descriptionElement = new Div(description);
    descriptionElement.addClassNames("normal-body-text");
    var downloadButton = new Button(buttonText, e -> subscribe(
        templateMono, sampleCountSupplier));
    buttonContainer.addClassName("sample-template-actions");
    buttonContainer.add(stylePrimary(downloadButton));

    add(descriptionElement, buttonContainer, downloadComponent);
  }

  /**
   * Adds an additional template export action to this component. Useful when a dialog should offer
   * multiple scopes for the template export (e.g. "all samples" vs "selected samples"). The first
   * (primary) export remains the one configured in the constructor.
   *
   * @param buttonText         the label for the additional export button
   * @param templateMono       a {@link Mono} resolving to the template to export
   * @param sampleCountSupplier the number of samples this export covers
   */
  public void addTemplateExport(String buttonText, Mono<DigitalObject> templateMono,
      Supplier<Integer> sampleCountSupplier) {
    requireNonNull(buttonText);
    requireNonNull(templateMono);
    requireNonNull(sampleCountSupplier);
    var exportButton = new Button(buttonText, e -> subscribe(
        templateMono, sampleCountSupplier));
    buttonContainer.add(styleSecondary(exportButton));
  }

  private void subscribe(Mono<DigitalObject> templateMono,
      @Nullable Supplier<Integer> sampleCountSupplier) {
    var inProgressToast = sampleCountSupplier != null
        ? messageFactory.pendingTaskToast("sample.fetching-metadata",
            new Object[]{sampleCountSupplier.get()}, getLocale())
        : messageFactory.pendingTaskToast("sample.preparing-download",
            MessageSourceNotificationFactory.EMPTY_PARAMETERS, getLocale());
    var failureToast = messageFactory.toast("task.failed", new Object[]{"Template generation"},
        getLocale());
    templateMono
        .doOnSubscribe(ignored -> openToast(inProgressToast))
        .doOnSuccess(this::triggerDownload).doOnTerminate(() -> closeToast(inProgressToast))
        .doOnError(throwable -> {
          closeToast(inProgressToast);
          openToast(failureToast);
        })
        .subscribe();
  }

  private static Button stylePrimary(Button button) {
    button.setIcon(VaadinIcon.DOWNLOAD.create());
    button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    return button;
  }

  private static Button styleSecondary(Button button) {
    button.setIcon(VaadinIcon.DOWNLOAD.create());
    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    return button;
  }

  private void closeToast(Toast toast) {
    getUI().ifPresent(ui -> ui.access(toast::close));
  }

  private void openToast(Toast toast) {
    getUI().ifPresent(ui -> ui.access(toast::open));
  }

  private void triggerDownload(DigitalObject digitalObject) {
    getUI().ifPresent(ui -> ui.access(() -> downloadComponent.trigger(new DownloadStreamProvider() {
      @Override
      public String getFilename() {
        var projectCode = projectCodeSupplier.get();
        return FileNameFormatter.formatWithTimestampedSimple(LocalDate.now(), projectCode,
            "sample metadata update template", "xlsx");
      }

      @Override
      public InputStream getStream() {
        return digitalObject.content();
      }

      @Override
      public String getContentType() {
        return MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
      }

      @Override
      public Optional<Long> contentLength() {
        return Optional.empty();
      }
    })));
  }

}