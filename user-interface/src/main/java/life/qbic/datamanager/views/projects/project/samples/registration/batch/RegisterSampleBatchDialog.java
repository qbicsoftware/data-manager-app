package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import static java.util.function.Predicate.not;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import life.qbic.datamanager.parser.ParsingResult;
import life.qbic.datamanager.parser.SampleInformationExtractor;
import life.qbic.datamanager.parser.SampleInformationExtractor.SampleInformationForNewSample;
import life.qbic.datamanager.parser.xlsx.XLSXParser;
import life.qbic.datamanager.views.general.WizardDialogWindow;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.FileType;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.UploadedData;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.ValidationResultWithPayload;
import life.qbic.projectmanagement.application.sample.SampleMetadata;
import life.qbic.projectmanagement.application.sample.SampleValidationService;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class RegisterSampleBatchDialog extends WizardDialogWindow {

  private final List<SampleMetadata> validatedSampleMetadata;
  private final TextField batchNameField;
  private final Checkbox pilotCheck;
  private static final Logger log = LoggerFactory.logger(RegisterSampleBatchDialog.class);

  private void setValidatedSampleMetadata(List<SampleMetadata> validatedSampleMetadata) {
    this.validatedSampleMetadata.clear();
    this.validatedSampleMetadata.addAll(validatedSampleMetadata);
  }

  public RegisterSampleBatchDialog(SampleValidationService sampleValidationService,
      String experimentId, String projectId) {
    addClassName("register-samples-dialog");
    batchNameField = new TextField("Batch name");
    batchNameField.setRequired(true);
    batchNameField.setPlaceholder("Please enter a name for your batch");
    pilotCheck = new Checkbox("this batch is a pilot");

    validatedSampleMetadata = new ArrayList<>();
    UploadWithDisplay uploadWithDisplay = new UploadWithDisplay(25 * 1024 * 1024, new FileType[]{
        new FileType(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    });
    uploadWithDisplay.addFailureListener(
        uploadFailed -> {/* display of the error is handled by the uploadWithDisplay component. So nothing to do here.*/});

    uploadWithDisplay.addSuccessListener(uploadSucceeded -> {
      UploadWithDisplay component = uploadSucceeded.getSource();
      UI ui = component.getUI().orElseThrow();
      UploadedData uploadedData = component.getUploadedData().orElseThrow();
      
      
      InProgressDisplay uploadProgressDisplay = new InProgressDisplay(
          component.getUploadedData().get().fileName());
      component.setDisplay(uploadProgressDisplay);

      List<SampleInformationForNewSample> sampleInformationForNewSamples = extractSampleInformationForNewSamples(
          uploadedData);

      List<CompletableFuture<ValidationResultWithPayload<SampleMetadata>>> validations = new ArrayList<>();
      for (SampleInformationForNewSample sampleInformationForNewSample : sampleInformationForNewSamples) {
        CompletableFuture<ValidationResultWithPayload<SampleMetadata>> validation = sampleValidationService.validateNewSampleAsync(
            sampleInformationForNewSample.sampleName(),
            sampleInformationForNewSample.condition(),
            sampleInformationForNewSample.species(),
            sampleInformationForNewSample.specimen(),
            sampleInformationForNewSample.analyte(),
            sampleInformationForNewSample.analysisMethod(),
            sampleInformationForNewSample.comment(),
            experimentId,
            projectId
        ).orTimeout(1, TimeUnit.MINUTES);
        validations.add(validation);
      }
      CompletableFuture<Void> validationTasks = CompletableFuture
          //allOf makes sure exceptional state is transferred to outer completable future.
          .allOf(validations.toArray(new CompletableFuture[0]))
          .orTimeout(5, TimeUnit.MINUTES);

      validationTasks
          .thenAccept(ignored -> {
            if (validations.stream().anyMatch(not(CompletableFuture::isDone))) {
              throw new IllegalStateException(
                  "validation task still in execution although expected to be done");
            }
            ValidationResultWithPayload<SampleMetadata> valueIfAbsent = new ValidationResultWithPayload<>(
                ValidationResult.withFailures(
                    List.of("Validation could not complete normally.")),
                new SampleMetadata("", "", null, "", "", "", 0, null, null, null,
                    "")); //not expected to occur

            List<ValidationResultWithPayload<SampleMetadata>> validationResults = validations.stream()
                .filter(result ->
                    result.isDone() && !result.isCancelled() && !result.isCompletedExceptionally())
                .map(future -> future.getNow(valueIfAbsent))
                .toList();
            List<ValidationResultWithPayload<SampleMetadata>> failedValidations = validationResults.stream()
                .filter(validation -> validation.validationResult().containsFailures())
                .toList();
            List<ValidationResultWithPayload<SampleMetadata>> succeededValidations = validationResults.stream()
                .filter(validation -> validation.validationResult().allPassed())
                .toList();

            if (!failedValidations.isEmpty()) {
              ui.access(() -> component.setDisplay(invalidDisplay(
                  failedValidations.stream().map(ValidationResultWithPayload::validationResult)
                      .toList())));
              setValidatedSampleMetadata(List.of());
              return;
            }
            if (!succeededValidations.isEmpty()) {
              ui.access(() -> component
                  .setDisplay(new ValidUploadDisplay(uploadedData.fileName(),
                      succeededValidations.size())));
              setValidatedSampleMetadata(
                  succeededValidations.stream().map(ValidationResultWithPayload::payload).toList());
            }
          })
          .exceptionally(e -> {
                RuntimeException runtimeException = new RuntimeException(
                    "At least one validation task could not complete.", e);
            log.error("Could not complete validation. Please try again.", runtimeException);
            InvalidUploadDisplay invalidUploadDisplay = invalidDisplay(List.of(
                ValidationResult.withFailures(
                    List.of("Could not complete validation. Please try again."))));
            ui.access(() -> component.setDisplay(invalidUploadDisplay));
                throw runtimeException;
              }
          );
    });

    add(batchNameField, pilotCheck, uploadWithDisplay);
  }

  private static List<SampleInformationForNewSample> extractSampleInformationForNewSamples(
      UploadedData uploadedData) {
    ParsingResult parsingResult = XLSXParser.create().parse(uploadedData.inputStream());
    return new SampleInformationExtractor()
        .extractInformationForNewSamples(parsingResult);
  }

  private static InvalidUploadDisplay invalidDisplay(
      List<ValidationResult> validationResults) {
    InvalidUploadDisplay invalidUploadDisplay = new InvalidUploadDisplay();
    List<String> failureReasons = validationResults.stream()
        .flatMap(res -> res.failures().stream()).toList();
    for (String failureReason : failureReasons) {
      invalidUploadDisplay.add(new Span(failureReason));
    }
    return invalidUploadDisplay;
  }

  @Override
  public void taskFailed(String label, String description) {

  }

  @Override
  public void taskSucceeded(String label, String description) {

  }

  @Override
  public void taskInProgress(String label, String description) {

  }

  static class InvalidUploadDisplay extends Div {

  }

  static class ValidUploadDisplay extends Div {

    private ValidUploadDisplay(String fileName, int count) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName("file-name");
      Div validationBox = new Div();
      validationBox.addClassName("validation-display-box");
      var box = new Div();
      var approvedTitle = new Span("Approved");
      var validIcon = VaadinIcon.CHECK_CIRCLE_O.create();
      validIcon.addClassName("success");
      var header = new Span(validIcon, approvedTitle);
      header.addClassName("header");
      var instruction = new Span("Please click Register to register your samples");
      instruction.addClassName("secondary");
      Div validationDetails = new Div();
      var approvedSamples = new Span("%d samples".formatted(count));
      approvedSamples.addClassName("bold");
      validationDetails.add(new Span("Sample data for "), approvedSamples,
          new Span(" is now ready to be registered."));
      box.add(header, validationDetails, instruction);
      validationBox.add(box);
      add(fileNameLabel, validationBox);
    }
  }

  private static class InProgressDisplay extends Div {

    private InProgressDisplay(String fileName) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName("file-name");
      ProgressBar progressBar = new ProgressBar();
      progressBar.setIndeterminate(true);
      add(fileNameLabel, new Div("Validating file..."), progressBar);
    }
  }

  public Registration addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    return addListener(ConfirmEvent.class, listener);
  }

  public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
    return addListener(CancelEvent.class, listener);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), pilotCheck.getValue(),
        batchNameField.getValue(), validatedSampleMetadata));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  public static class ConfirmEvent extends ComponentEvent<RegisterSampleBatchDialog> {

    private final boolean pilot;
    private final String batchName;
    private final List<SampleMetadata> validatedSampleMetadata;


    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source                  the source component
     * @param fromClient              <code>true</code> if the event originated from the client
     *                                side, <code>false</code> otherwise
     * @param pilot
     * @param batchName
     * @param validatedSampleMetadata
     */
    public ConfirmEvent(RegisterSampleBatchDialog source, boolean fromClient, boolean pilot,
        String batchName,
        List<SampleMetadata> validatedSampleMetadata) {
      super(source, fromClient);
      this.pilot = pilot;
      this.batchName = batchName;
      this.validatedSampleMetadata = validatedSampleMetadata;
    }

    public List<SampleMetadata> validatedSampleMetadata() {
      return Collections.unmodifiableList(validatedSampleMetadata);
    }
  }

  public static class CancelEvent extends ComponentEvent<RegisterSampleBatchDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(RegisterSampleBatchDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
