package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.files.parsing.MetadataParser.ParsingException;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor.SampleInformationForNewSample;
import life.qbic.datamanager.files.parsing.xlsx.XLSXParser;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent;
import life.qbic.datamanager.views.general.upload.UploadedFilesChangeListener.FileEntry;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleUploadDisplay.InProgressDisplay;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleUploadDisplay.InvalidUploadDisplay;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleUploadDisplay.ValidUploadDisplay;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SampleRegistrationInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.util.unit.DataSize;
import reactor.core.publisher.Flux;

/**
 * <b>Sample Registration Upload</b>
 * <p>
 * A data manager component that enables users to upload a single Excel sheet to register new
 * samples. Uploaded content is parsed and validated via the {@link AsyncProjectService}; the result
 * is surfaced inline and exposed through {@link #getValidatedSampleMetadata()}.
 * <p>
 * The component only accepts Excel files (<code>.xlsx</code>).
 *
 * @since 1.11.0
 */
public class SampleRegistrationUpload extends Div implements UserInput {

  private static final Logger log = LoggerFactory.logger(SampleRegistrationUpload.class);
  private static final int MAX_FILE_SIZE = 25 * 1024 * 1024;

  private final AsyncProjectService service;
  private final String projectId;
  private final String experimentId;
  private final ContentUploadComponent contentUploadComponent;
  private final SampleUploadDisplay uploadDisplay;
  private final transient Map<String, List<SampleRegistrationInformation>> validatedSampleMetadata = new HashMap<>();

  public SampleRegistrationUpload(AsyncProjectService service,
      String projectId,
      String experimentId,
      UploadConfiguration uploadConfiguration) {
    this.service = requireNonNull(service);
    this.projectId = requireNonNull(projectId);
    this.experimentId = requireNonNull(experimentId);

    this.contentUploadComponent = new ContentUploadComponent(requireNonNull(uploadConfiguration));
    this.uploadDisplay = new SampleUploadDisplay();

    contentUploadComponent.setAcceptedMimeTypes(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    contentUploadComponent.setMaxFiles(1);
    contentUploadComponent.setMaxFileSize(DataSize.ofBytes(MAX_FILE_SIZE));
    contentUploadComponent.addUnspecificFailureListener(uploadFailed -> log.error(
        "Upload failed for project(" + projectId + ") experiment(" + experimentId + ")",
        uploadFailed.getCause()));

    Registration controllerRegistration = new SampleRegistrationUploadDisplayController(projectId,
        experimentId).control(uploadDisplay, contentUploadComponent);
    uploadDisplay.addDetachListener(it -> controllerRegistration.remove());

    Span uploadTitle = new Span("Upload the sample data");
    uploadTitle.addClassName("section-title");
    Div uploadSection = new Div(uploadTitle, contentUploadComponent, uploadDisplay);
    uploadSection.addClassName("upload-section");
    uploadSection.addClassName("section-with-title");

    add(uploadSection);
    addClassNames("flex-vertical", "gap-06");
  }

  /**
   * @return the flattened, distinct list of validated sample registrations from all uploaded files
   */
  public List<SampleRegistrationInformation> getValidatedSampleMetadata() {
    return validatedSampleMetadata.values()
        .stream().flatMap(Collection::stream)
        .distinct().toList();
  }

  @Override
  @NonNull
  public InputValidation validate() {
    if (validatedSampleMetadata.isEmpty()) {
      return InputValidation.failed();
    }
    return InputValidation.passed();
  }

  @Override
  public boolean hasChanges() {
    return !validatedSampleMetadata.isEmpty();
  }

  private static List<SampleInformationForNewSample> extractSampleInformationForNewSamples(
      InputStream inputStream) {
    ParsingResult parsingResult = XLSXParser.create().parse(inputStream);
    return new SampleInformationExtractor()
        .extractInformationForNewSamples(parsingResult);
  }

  private static SampleRegistrationInformation convertToRegistration(
      SampleInformationForNewSample information) {
    return new SampleRegistrationInformation(
        information.sampleName(),
        information.biologicalReplicate(),
        information.condition(),
        information.species(),
        information.specimen(),
        information.analyte(),
        information.analysisMethod(),
        information.comment(),
        information.confoundingVariables(),
        information.batch());
  }

  private static ValidationRequest convertToRequest(SampleRegistrationInformation registration,
      String projectId, String experimentId) {
    return new ValidationRequest(projectId, experimentId, registration);
  }

  class SampleRegistrationUploadDisplayController {

    private final String projectId;
    private final String experimentId;

    private SampleRegistrationUploadDisplayController(String projectId, String experimentId) {
      this.projectId = Objects.requireNonNull(projectId);
      this.experimentId = Objects.requireNonNull(experimentId);
    }

    Registration control(SampleUploadDisplay sampleUploadDisplay,
        ContentUploadComponent contentUploadComponent) {

      Objects.requireNonNull(sampleUploadDisplay);
      Objects.requireNonNull(contentUploadComponent);
      var changeRegistration = contentUploadComponent.addChangeListener(event -> {
        var componentUI = contentUploadComponent.getUI();
        switch (event.changeType()) {
          case FILE_ADDED -> {
            event.changedFiles().forEach(it -> sampleUploadDisplay.setDisplay(it.fileName(),
                new InProgressDisplay(it.fileName())));
            event.changedFiles().forEach(fileEntry -> {
              String fileName = fileEntry.fileName();
              contentUploadComponent.getContent(fileName).ifPresentOrElse(
                  fileContent -> {
                    List<SampleInformationForNewSample> sampleInfos;
                    try {
                      sampleInfos = new ArrayList<>(
                          extractSampleInformationForNewSamples(fileContent));
                    } catch (ParsingException parsingException) {
                      log.warn("Could not parse sample file content "
                          + parsingException.getMessage());
                      componentUI.ifPresent(ui -> ui.access(() -> sampleUploadDisplay.setDisplay(
                          fileName,
                          new InvalidUploadDisplay(fileName,
                              "Could not complete validation. "
                                  + parsingException.getMessage()))));
                      return;
                    }
                    if (sampleInfos.isEmpty()) {
                      componentUI.ifPresent(ui -> ui.access(() -> sampleUploadDisplay.setDisplay(
                          fileName,
                          new InvalidUploadDisplay(fileName, "No valid metadata provided."))));
                      return;
                    }
                    List<SampleRegistrationInformation> registrations = sampleInfos.stream()
                        .distinct()
                        .map(SampleRegistrationUpload::convertToRegistration)
                        .toList();
                    if (registrations.isEmpty()) {
                      return;
                    }
                    List<ValidationRequest> requests = registrations.stream()
                        .map(registration -> convertToRequest(registration, projectId,
                            experimentId))
                        .toList();
                    Stream<ValidationResponse> responseStream = service.validate(
                            Flux.fromStream(requests.stream()))
                        .doOnError(cause -> {
                          log.error("Validation failed.", cause);
                          InvalidUploadDisplay invalidUploadDisplay = new InvalidUploadDisplay(
                              fileName, "Apologies, the validation failed. Please try again.");
                          componentUI.ifPresent(ui -> ui.access(
                              () -> sampleUploadDisplay.setDisplay(fileName,
                                  invalidUploadDisplay)));
                        })
                        .toStream();

                    List<ValidationResult> failedValidations = new ArrayList<>();
                    List<ValidationResult> successfulValidations = new ArrayList<>();

                    responseStream.forEach(responseResult -> {
                      if (responseResult.result().containsFailures()) {
                        failedValidations.add(responseResult.result());
                      } else {
                        successfulValidations.add(responseResult.result());
                      }
                    });

                    if (!failedValidations.isEmpty()) {
                      componentUI.ifPresent(ui -> ui.access(
                          () -> sampleUploadDisplay.setDisplay(fileName,
                              new InvalidUploadDisplay(
                                  fileName,
                                  failedValidations.stream()
                                      .flatMap(res -> res.failures().stream())
                                      .toList()))));
                      validatedSampleMetadata.put(fileName, List.of());
                      return;
                    }

                    if (!successfulValidations.isEmpty()) {
                      componentUI.ifPresent(ui -> ui.access(() -> sampleUploadDisplay
                          .setDisplay(fileName, new ValidUploadDisplay(fileName,
                              successfulValidations.size()))));
                      validatedSampleMetadata.put(fileName, registrations);
                    }
                  },
                  () -> componentUI.ifPresent(ui -> ui.access(() -> sampleUploadDisplay.setDisplay(
                      fileName,
                      new InvalidUploadDisplay(fileName, "No valid sample metadata provided.")))));
            });
          }
          case FILE_REMOVED -> {
            event.changedFiles().forEach(it -> validatedSampleMetadata.remove(it.fileName()));
            sampleUploadDisplay.removeDisplay(event.changedFiles().stream().map(
                FileEntry::fileName).toList());
          }
        }
      });

      var removedRegistration = contentUploadComponent.addFileRemovedListener(
          event -> sampleUploadDisplay.removeDisplay(List.of(event.getFileName())));

      return () -> {
        changeRegistration.remove();
        removedRegistration.remove();
      };
    }
  }
}