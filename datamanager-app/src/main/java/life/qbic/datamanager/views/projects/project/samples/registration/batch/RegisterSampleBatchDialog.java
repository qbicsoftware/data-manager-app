package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.AccessDeniedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SampleRegistrationInformation;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;

/**
 * Factory for the sample registration dialog.
 * <p>
 * Mirrors the edit dialog: a {@link life.qbic.datamanager.views.general.dialog.AppDialog} that
 * always offers the blank sample metadata template for download (there is no selection when
 * registering new samples) and a single Excel upload to submit the filled sheet.
 *
 * @since 1.4.0
 */
public final class RegisterSampleBatchDialog {

  private static final MimeType OPEN_XML = MimeType.valueOf(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private RegisterSampleBatchDialog() {
  }

  /**
   * Creates the sample registration dialog.
   *
   * @param onConfirm callback invoked with the validated sample registrations once the user
   *                  confirms; the dialog is already closed when the callback is invoked
   */
  public static AppDialog create(
      AsyncProjectService service,
      MessageSourceNotificationFactory messageFactory,
      String experimentId,
      String projectId,
      String projectCode,
      UploadConfiguration uploadConfiguration,
      Consumer<List<SampleRegistrationInformation>> onConfirm) {
    Objects.requireNonNull(service);
    Objects.requireNonNull(messageFactory);
    Objects.requireNonNull(uploadConfiguration);
    Objects.requireNonNull(onConfirm);

    var dialog = AppDialog.large();
    DialogHeader.with(dialog, "Register Samples");
    DialogFooter.with(dialog, "Cancel", "Register");

    var upload = new SampleRegistrationUpload(service, projectId, experimentId,
        uploadConfiguration);
    SampleTemplateComponent template = setupTemplateSection(service, messageFactory, experimentId,
        projectId, projectCode);
    var registrationComponent = new SampleRegistrationComponent(template, upload);

    DialogBody.with(dialog, registrationComponent, registrationComponent);
    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      var validatedMetadata = registrationComponent.getValidatedSampleMetadata();
      dialog.close();
      onConfirm.accept(validatedMetadata);
    });
    return dialog;
  }

  private static SampleTemplateComponent setupTemplateSection(AsyncProjectService service,
      MessageSourceNotificationFactory messageFactory,
      String experimentId,
      String projectId,
      String projectCode) {
    Mono<DigitalObject> registrationTemplate = Mono.defer(() ->
        service.sampleRegistrationTemplate(projectId, experimentId, OPEN_XML)
            .doOnError(throwable -> handleError(throwable, messageFactory)));
    // Registration always provides an empty sheet, so there is no sample count to report.
    return new SampleTemplateComponent(
        "Please download the metadata template, fill in the sample properties and upload the metadata sheet below to register the samples.",
        "Download metadata template",
        registrationTemplate,
        messageFactory,
        () -> projectCode,
        null);
  }

  private static void handleError(Throwable throwable,
      MessageSourceNotificationFactory messageFactory) {
    if (Objects.requireNonNull(throwable) instanceof AccessDeniedException) {
      handleAccessDeniedError(messageFactory);
    } else {
      handleUnexpectedError(throwable);
    }
  }

  private static void handleUnexpectedError(Throwable throwable) {
    throw new ApplicationException("We are sorry, an unexpected error occurred.", throwable);
  }

  private static void handleAccessDeniedError(MessageSourceNotificationFactory messageFactory) {
    var ui = com.vaadin.flow.component.UI.getCurrent();
    if (ui != null) {
      ui.access(() -> messageFactory.toast("access.denied.message", new Object[]{},
          ui.getLocale()).open());
    }
  }
}