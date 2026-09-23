package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.files.parsing.MetadataParser.ParsingException;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor.SampleInformationForExistingSample;
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
import life.qbic.projectmanagement.application.ValidationResultWithPayload;
import life.qbic.projectmanagement.application.sample.SampleMetadata;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Sample Update Upload</b>
 * <p>
 * A data manager component that enables users to upload a single Excel sheet to update existing
 * sample metadata. Uploaded content is parsed and validated against the registered samples via the
 * {@link SampleValidationService}; the result is surfaced inline and exposed through
 * {@link #getValidatedSampleMetadata()}.
 * <p>
 * The component only accepts Excel files (<code>.xlsx</code>).
 *
 * @since 1.11.0
 */
public class SampleUpdateUpload extends Div implements UserInput {

  private static final Logger log = LoggerFactory.logger(SampleUpdateUpload.class);

  private final SampleValidationService sampleValidationService;
  private final String experimentId;
  private final String projectId;
  private final ContentUploadComponent contentUploadComponent;
  private final SampleUploadDisplay uploadDisplay;
  private final transient Map<String, List<SampleMetadata>> validatedSampleMetadata = new HashMap<>();

  public SampleUpdateUpload(SampleValidationService sampleValidationService,
      String projectId,
      String experimentId,
      UploadConfiguration uploadConfiguration) {
    this.sampleValidationService = requireNonNull(sampleValidationService);
    this.projectId = requireNonNull(projectId);
    this.experimentId = requireNonNull(experimentId);

    this.contentUploadComponent = new ContentUploadComponent(requireNonNull(uploadConfiguration));
    this.uploadDisplay = new SampleUploadDisplay();

    contentUploadComponent.setMaxFiles(1);
    contentUploadComponent.setAcceptedMimeTypes(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    contentUploadComponent.addUnspecificFailureListener(uploadFailed -> log.error(
        "Upload failed for project(" + projectId + ") experiment(" + experimentId + ")",
        uploadFailed.getCause()));

    Registration controllerRegistration = new SampleEditUploadDisplayController(projectId,
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
   * @return the flattened, distinct list of validated sample metadata from all uploaded files
   */
  public List<SampleMetadata> getValidatedSampleMetadata() {
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

  private List<SampleInformationForExistingSample> extractSampleInformationForExistingSamples(
      InputStream inputStream) {
    ParsingResult parsingResult = XLSXParser.create().parse(inputStream);
    return new SampleInformationExtractor()
        .extractInformationForExistingSamples(parsingResult);
  }

  class SampleEditUploadDisplayController {

    private final String projectId;
    private final String experimentId;

    private SampleEditUploadDisplayController(String projectId, String experimentId) {
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
              var fileName = fileEntry.fileName();
              contentUploadComponent.getContent(fileName).ifPresentOrElse(
                  inputStream -> {
                    List<SampleInformationForExistingSample> sampleInfos;
                    try {
                      sampleInfos = new ArrayList<>(
                          extractSampleInformationForExistingSamples(inputStream));
                    } catch (ParsingException e) {
                      InvalidUploadDisplay display = new InvalidUploadDisplay(fileName,
                          "Parsing failed " + e.getMessage());
                      componentUI.ifPresent(ui -> ui.access(() -> {
                        sampleUploadDisplay.setDisplay(fileName, display);
                        display.focus();
                      }));
                      return;
                    }
                    if (sampleInfos.isEmpty()) {
                      InvalidUploadDisplay display = new InvalidUploadDisplay(
                          fileName, "No valid metadata provided");
                      componentUI.ifPresent(ui -> ui.access(() -> {
                        sampleUploadDisplay.setDisplay(fileName, display);
                        display.focus();
                      }));
                      return;
                    }
                    runValidation(sampleInfos, fileName, sampleUploadDisplay,
                        componentUI.orElse(null));
                  },
                  () -> {
                    InvalidUploadDisplay display = new InvalidUploadDisplay(fileName,
                        "Content extraction failed.");
                    componentUI.ifPresent(ui -> ui.access(() -> {
                      sampleUploadDisplay.setDisplay(fileName, display);
                      display.focus();
                    }));
                  });
            });
          }
          case FILE_REMOVED -> event.changedFiles().stream()
              .map(FileEntry::fileName)
              .forEach(validatedSampleMetadata::remove);
        }
      });

      var removedRegistration = contentUploadComponent.addFileRemovedListener(event -> {
        validatedSampleMetadata.remove(event.getFileName());
        sampleUploadDisplay.removeDisplay(List.of(event.getFileName()));
      });

      return () -> {
        changeRegistration.remove();
        removedRegistration.remove();
      };
    }

    private void runValidation(List<SampleInformationForExistingSample> sampleInfos,
        String fileName, SampleUploadDisplay sampleUploadDisplay, @Nullable UI componentUI) {
      var securityContext = SecurityContextHolder.getContext();
      List<CompletableFuture<ValidationResultWithPayload<SampleMetadata>>> validations = sampleInfos.stream()
          .map(info -> CompletableFuture.supplyAsync(
                  () -> {
                    SecurityContextHolder.setContext(securityContext);
                    return sampleValidationService.validateExistingSample(
                        info.sampleCode(),
                        info.sampleName(),
                        info.biologicalReplicate(),
                        info.condition(),
                        info.species(),
                        info.specimen(),
                        info.analyte(),
                        info.analysisMethod(),
                        info.comment(),
                        info.confoundingVariables(),
                        info.batch(),
                        experimentId, projectId);
                  })
              .orTimeout(1, TimeUnit.MINUTES))
          .toList();
      CompletableFuture.allOf(validations.toArray(new CompletableFuture[0]))
          .thenApply(v -> validations.stream().map(CompletableFuture::join).toList())
          .orTimeout(5, TimeUnit.MINUTES)
          .thenAccept(results -> {
            List<ValidationResultWithPayload<SampleMetadata>> failed = results.stream().filter(
                result -> result.validationResult().containsFailures()).toList();
            List<ValidationResultWithPayload<SampleMetadata>> succeeded = results.stream().filter(
                result -> result.validationResult().allPassed()).toList();
            if (!failed.isEmpty()) {
              validatedSampleMetadata.remove(fileName);
              InvalidUploadDisplay display = new InvalidUploadDisplay(fileName,
                  failed.stream()
                      .flatMap(r -> r.validationResult().failures().stream())
                      .toList());
              Optional.ofNullable(componentUI).ifPresent(ui -> ui.access(() -> {
                sampleUploadDisplay.setDisplay(fileName, display);
                display.focus();
              }));
            } else if (!succeeded.isEmpty()) {
              List<SampleMetadata> successMetadata = succeeded.stream().map(
                  ValidationResultWithPayload::payload).toList();
              validatedSampleMetadata.put(fileName, successMetadata);
              ValidUploadDisplay display = new ValidUploadDisplay(fileName, successMetadata.size());
              Optional.ofNullable(componentUI).ifPresent(ui -> ui.access(() -> {
                sampleUploadDisplay.setDisplay(fileName, display);
                display.focus();
              }));
            }
          })
          .exceptionally(e -> {
            validatedSampleMetadata.remove(fileName);
            log.error("Validation failed for file: " + fileName, e);
            InvalidUploadDisplay display = new InvalidUploadDisplay(fileName,
                "Validation failed. Please try again.");
            Optional.ofNullable(componentUI).ifPresent(ui -> ui.access(() -> {
              sampleUploadDisplay.setDisplay(fileName, display);
              display.focus();
            }));
            return null;
          });
    }
  }
}