package life.qbic.datamanager.views.projects.project.samples.registration.batch;


import com.vaadin.flow.component.UI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.AccessDeniedException;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.sample.SampleMetadata;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;

/**
 * Factory for the sample edit dialog.
 * <p>
 * Mirrors the measurement edit dialog: it is scope-agnostic towards the (session-only) row
 * selection. Updates are identified per row via the sample code in the uploaded sheet, so the
 * dialog always offers to download the edit template for <em>all</em> samples of the current
 * experiment. When a selection exists, it is additionally offered as a convenience to download a
 * smaller, targeted template for exactly the selected samples.
 *
 * @since 1.4.0
 */
public final class EditSampleBatchDialog {

  private static final MimeType OPEN_XML = MimeType.valueOf(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private EditSampleBatchDialog() {
  }

  /**
   * Creates the sample edit dialog.
   *
   * @param onConfirm callback invoked with the validated sample metadata once the user confirms the
   *                  edit; the dialog is already closed when the callback is invoked
   */
  public static AppDialog create(
      AsyncProjectService service,
      MessageSourceNotificationFactory messageFactory,
      Set<String> sampleIds,
      String experimentId,
      String projectId,
      String projectCode,
      SampleValidationService sampleValidationService,
      UploadConfiguration uploadConfiguration,
      Consumer<List<SampleMetadata>> onConfirm) {
    Objects.requireNonNull(service);
    Objects.requireNonNull(messageFactory);
    Objects.requireNonNull(sampleValidationService);
    Objects.requireNonNull(uploadConfiguration);
    Objects.requireNonNull(onConfirm);

    var dialog = AppDialog.large();
    DialogHeader.with(dialog, "Edit Samples");
    DialogFooter.with(dialog, "Cancel", "Edit Samples");

    var upload = new SampleUpdateUpload(sampleValidationService, projectId, experimentId,
        uploadConfiguration);
    SampleTemplateComponent template = setupTemplateSection(service, messageFactory, sampleIds,
        experimentId, projectId, projectCode);
    var updateComponent = new SampleUpdateComponent(template, upload);

    DialogBody.with(dialog, updateComponent, updateComponent);
    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      var validatedMetadata = updateComponent.getValidatedSampleMetadata();
      dialog.close();
      onConfirm.accept(validatedMetadata);
    });
    return dialog;
  }

  private static SampleTemplateComponent setupTemplateSection(AsyncProjectService service,
      MessageSourceNotificationFactory messageFactory,
      Set<String> sampleIds,
      String experimentId,
      String projectId,
      String projectCode) {
    // The "download all" template is lazy: sample IDs are resolved only when the user clicks the
    // button, so opening the dialog stays instant and the workbook reflects the current state of
    // the experiment at export time. This is the default export and rescues users whose session
    // selection was lost.
    Mono<DigitalObject> allSamplesTemplate = Mono.defer(() ->
        service.sampleUpdateTemplate(projectId, experimentId, OPEN_XML)
            .doOnError(throwable -> handleError(throwable, messageFactory)));
    Supplier<Integer> allSamplesCount = () ->
        service.countSamples(projectId, experimentId).blockOptional().orElse(0);
    SampleTemplateComponent template = new SampleTemplateComponent(
        "Export the sample metadata you want to edit. You can modify the properties in the sheet and upload it below to save the changes.",
        "Export all samples",
        allSamplesTemplate,
        messageFactory,
        () -> projectCode,
        allSamplesCount);
    if (!sampleIds.isEmpty()) {
      // the selected set is already resolved (dialog-open time); just wrap it in a deferred
      // mono so the service call happens on click
      template.addTemplateExport(
          "Export selected (%d)".formatted(sampleIds.size()),
          Mono.defer(() -> service.sampleUpdateTemplate(projectId, experimentId, sampleIds,
              OPEN_XML).doOnError(throwable -> handleError(throwable, messageFactory))),
          sampleIds::size);
    }
    return template;
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
    var ui = UI.getCurrent();
    if (ui != null) {
      ui.access(() -> messageFactory.toast("access.denied.message", new Object[]{},
          ui.getLocale()).open());
    }
  }
}