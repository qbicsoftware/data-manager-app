package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.unit.DataSize;

class SampleUploadDisplay extends Div {

  private final Map<String, Component> displayedFiles = new HashMap<>();
  private final InvalidUploadDisplay emptyDisplay = new InvalidUploadDisplay(
      "Nothing was uploaded. Please upload the sample metadata and try again.");

  private final Div errorArea = new Div();
  private final Div displayContainer = new Div();
  private final Div fileSizeRestriction;
  private final Span displayContainerTitle = new Span("Uploaded File");
  private DataSize maxFileSize = null;

  public SampleUploadDisplay() {
    addClassName("display-contents");
    errorArea.addClassNames("error-message-box");
    displayContainer.addClassNames("uploaded-items-section");

    displayContainerTitle.addClassNames("section-title");

    var restrictions = new Div();
    restrictions.addClassNames("restrictions");
    fileSizeRestriction = new Div();
    setMaxFileSize(maxFileSize);
    restrictions.add(fileSizeRestriction);

    updateVisibility();
    displayContainer.add(emptyDisplay);
    add(displayContainerTitle, errorArea, displayContainer, restrictions);
  }

  void updateVisibility() {
    displayContainer.setVisible(hasContent());
    displayContainerTitle.setVisible(hasContent());

  }

  void setMaxFileSize(DataSize maxFileSize) {
    Optional.ofNullable(maxFileSize)
        .ifPresent(fileSize -> {
          this.maxFileSize = fileSize;
          fileSizeRestriction.setText("Maximum file size: " + fileSize);
        });
  }

  private boolean hasContent() {
    return !displayedFiles.isEmpty();
  }

  void removeDisplay(List<String> fileNames) {
    List<Component> associatedComponents = fileNames.stream()
        .map(name -> displayedFiles.getOrDefault(name, null))
        .filter(Objects::nonNull)
        .toList();
    displayContainer.remove(associatedComponents);
    fileNames.forEach(displayedFiles::remove);
    if (hasContent()) {

      displayContainer.remove(emptyDisplay);
    } else {
      displayContainer.add(emptyDisplay);
    }
    updateVisibility();
  }

  void setDisplay(String fileName, Component display) {
    Optional<Component> existingComponent = Optional.ofNullable(
        displayedFiles.getOrDefault(fileName, null));
    existingComponent.ifPresentOrElse(
        existing -> displayContainer.replace(existing, display),
        () -> displayContainer.add(display));
    displayedFiles.put(fileName, display);
    displayContainer.remove(emptyDisplay);
    updateVisibility();
  }

  public void setErrorText(String errorMessage) {
    this.errorArea.setText(errorMessage);
    this.errorArea.setVisible(errorMessage != null && !errorMessage.isBlank());

  }

  static class InProgressDisplay extends Div implements Focusable<InProgressDisplay> {

    InProgressDisplay(String fileName) {
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

  static class InvalidUploadDisplay extends Div implements Focusable<InvalidUploadDisplay> {

    public InvalidUploadDisplay(String error) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Div validationBox = new Div();
      validationBox.addClassName("validation-display-box");
      var box = new Div();
      var failuresTitle = new Span(error);
      var errorIcon = VaadinIcon.CLOSE_CIRCLE.create();
      errorIcon.addClassName("error");
      var header = new Span(errorIcon, failuresTitle);
      header.addClassName("header");
      box.add(header);
      validationBox.add(box);
      add(validationBox);
    }

    public InvalidUploadDisplay(String fileName, List<String> failureReasons) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName("file-name");
      Div validationBox = new Div();
      validationBox.addClassName("validation-display-box");
      var box = new Div();
      var failuresTitle = new Span("Invalid sample metadata");
      var errorIcon = VaadinIcon.CLOSE_CIRCLE.create();
      errorIcon.addClassName("error");
      var header = new Span(errorIcon, failuresTitle);
      header.addClassName("header");
      var instruction = new Span(
          "Please correct the entries in the uploaded file and re-upload the file.");
      instruction.addClassName("secondary");
      Div validationDetails = new Div();

      Map<String, Integer> frequencyMap = failureReasons.stream()
          .distinct()
          .collect(Collectors.toMap(
              Function.identity(),
              v -> Collections.frequency(failureReasons, v)
          ));
      frequencyMap.forEach(
          (key, frequency) -> {
            String s = frequency + " sample" + ((frequency > 1) ? "s." : ".");
            Span span = new Span(s);
            span.addClassName("bold");
            validationDetails.add(new Div(new Span(key + " for "), span));
          });
      box.add(header, validationDetails, instruction);
      validationBox.add(box);
      add(fileNameLabel, validationBox);
    }

    public InvalidUploadDisplay(String fileName, String failureReason) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName("file-name");
      Div validationBox = new Div();
      validationBox.addClassName("validation-display-box");
      var box = new Div();
      var failuresTitle = new Span("Invalid sample metadata");
      var errorIcon = VaadinIcon.CLOSE_CIRCLE.create();
      errorIcon.addClassName("error");
      var header = new Span(errorIcon, failuresTitle);
      header.addClassName("header");
      var instruction = new Span(
          "Please correct the entries in the uploaded file and re-upload the file.");
      instruction.addClassName("secondary");
      Div validationDetails = new Div();

      validationDetails.add(new Div(failureReason));
      box.add(header, validationDetails, instruction);
      validationBox.add(box);
      add(fileNameLabel, validationBox);
    }
  }

  static class ValidUploadDisplay extends Div implements Focusable<ValidUploadDisplay> {

    ValidUploadDisplay(String fileName, int count) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName("file-name");
      Div validationBox = new Div();
      validationBox.addClassName("validation-display-box");
      var box = new Div();
      var approvedTitle = new Span("Your data has been approved");
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

    ValidUploadDisplay(String fileName) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName("file-name");
      Div validationBox = new Div();
      validationBox.addClassName("validation-display-box");
      var box = new Div();
      var approvedTitle = new Span("Your data has been approved");
      var validIcon = VaadinIcon.CHECK_CIRCLE_O.create();
      validIcon.addClassName("success");
      var header = new Span(validIcon, approvedTitle);
      header.addClassName("header");
      var instruction = new Span("Please click Register to register your samples");
      instruction.addClassName("secondary");
      Div validationDetails = new Div();
      validationDetails.add(new Span("Sample data is now ready to be registered."));
      box.add(header, validationDetails, instruction);
      validationBox.add(box);
      add(fileNameLabel, validationBox);
    }
  }

}
