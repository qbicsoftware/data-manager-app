package life.qbic.datamanager.views.projects.quality_control;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.FailedEvent;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.Upload;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.FileSizeFormatter;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.views.general.AllowedFileExtension;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent;
import life.qbic.datamanager.views.general.upload.UploadedFilesChangeListener.FileEntry;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.quality_control.QualityControlItem.ExperimentItem;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.sample.qualitycontrol.QualityControlReport;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.util.unit.DataSize;

/**
 * <b>Upload Quality Control Dialog</b>
 * <p>
 * A dialog window that enables uploads of sample quality control reports.
 */
public class UploadQualityControlDialog extends DialogWindow {

  private static final DataSize MAX_FILE_SIZE = DataSize.ofMegabytes(16); // 16 MiB
  @Serial
  private static final long serialVersionUID = 6602134795666762831L;
  private final ContentUploadComponent contentUploadComponent;
  private final Div uploadedQualityControlItems;
  private final List<QualityControlItem> qualityControlItemsCache = new ArrayList<>();
  private final Div uploadedItemsSectionContent;
  private final List<ExperimentItem> selectableExperimentsForProject;

  public UploadQualityControlDialog(ProjectId projectId,
      ExperimentInformationService experimentInformationService,
      UploadConfiguration uploadConfiguration) {
    Objects.requireNonNull(experimentInformationService,
        "experiment information service must not be null");

    //Load selectable Experiments for Sample quality control items;
    selectableExperimentsForProject = experimentInformationService.findAllForProject(projectId)
        .stream()
        .map(experiment -> new ExperimentItem(experiment.experimentId(), experiment.getName()))
        .toList();

    contentUploadComponent = new ContentUploadComponent(
        Objects.requireNonNull(uploadConfiguration));
    contentUploadComponent.setMaxFileSize(Math.toIntExact(MAX_FILE_SIZE.toBytes()));

    // Vaadin's upload component setup
    contentUploadComponent.setAcceptedFileTypes(
        AllowedFileExtension.PDF.mimetype(),
        AllowedFileExtension.EXCEL.mimetype(),
        AllowedFileExtension.WORD.mimetype()
    );


    setHeaderTitle("Upload a Sample QC Report");
    // Title box configuration
    Span uploadSectionTitle = new Span("Upload the report");
    uploadSectionTitle.addClassName("section-title");

    Div uploadSection = new Div();
    uploadSection.add(uploadSectionTitle, contentUploadComponent);

    // Uploaded quality_control items display configuration
    uploadedQualityControlItems = new Div();
    uploadedQualityControlItems.addClassName("uploaded-quality-control-items");
    uploadedItemsSectionContent = new Div();
    Span uploadedItemsSectionTitle = new Span("Link to an experiment (optional)");
    uploadedItemsSectionTitle.addClassName("section-title");
    uploadedItemsSectionContent.add(uploadedItemsSectionTitle);
    Paragraph uploadedItemsDescription = new Paragraph(
        "Please select the experiment for which the report was generated.");
    uploadedItemsDescription.addClassName("uploaded-items-description");
    uploadedItemsSectionContent.add(uploadedItemsDescription);

    Div uploadedItemsSection = new Div();
    uploadedItemsSection.add(uploadedItemsSectionContent, uploadedQualityControlItems);

    var changeRegistration = contentUploadComponent.addChangeListener(event -> {
      var componentUI = event.getSource().getUI();
      switch (event.changeType()) {
        case FILE_ADDED -> {
          for (FileEntry changedFile : event.changedFiles()) {
            var qualityControl = new QualityControlItem(changedFile.fileName(),
                selectableExperimentsForProject);
            qualityControlItemsCache.add(qualityControl);
            componentUI.ifPresent(
                ui -> ui.access(() -> uploadedQualityControlItems.add(qualityControl)));
          }
        }
        case FILE_REMOVED -> {
          var toBeRemoved = qualityControlItemsCache.stream()
              .filter(Objects::nonNull)
              .filter(qcItem -> event.changedFiles().stream().map(FileEntry::fileName)
                  .anyMatch(it -> it.equals(qcItem.fileName())))
              .toList();
          toBeRemoved
              .forEach(qcItem -> {
                qualityControlItemsCache.remove(qcItem);
                componentUI.ifPresent(
                    ui -> ui.access(() -> uploadedQualityControlItems.remove(qcItem)));
              });
        }
      }
      componentUI.ifPresent(ui -> ui.access(this::toggleFileSectionIfEmpty));
    });
    var rejectionRegistration = contentUploadComponent.addFileRejectedListener(
        this::onUploadFailure);
    addDetachListener(event -> {
      changeRegistration.remove();
      rejectionRegistration.remove();
    });


    // Put the elements together
    add(uploadSection, uploadedItemsSection);
    addClassName("quality-control-upload");
    confirmButton.setText("Save");
    // Init the visibility rendering once
    toggleFileSectionIfEmpty();
  }

  private void onUploadFailure(ComponentEvent<Upload> event) {
    ErrorMessage errorMessage = new ErrorMessage("Quality Control upload failed",
        "An unknown exception has occurred");
    if (event instanceof FileRejectedEvent) {
      errorMessage.descriptionTextSpan.setText(
          "Please provide a file within the file limit of " + FileSizeFormatter.formatBytes(
              contentUploadComponent.getMaxFileSize()));
    } else if (event instanceof FailedEvent) {
      errorMessage.descriptionTextSpan.setText(
          "Quality control upload was interrupted, please try again");
    }
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new ConfirmEvent(this, true));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }



  private void toggleFileSectionIfEmpty() {
    boolean filesUploaded = !uploadedQualityControlItems.getChildren().toList().isEmpty();
    uploadedQualityControlItems.setVisible(filesUploaded);
    uploadedItemsSectionContent.setVisible(filesUploaded);
  }

  public List<QualityControlReport> qualityControlItems() {
    return qualityControlItemsCache.stream().map(this::convertToQualityControl).toList();
  }

  private QualityControlReport convertToQualityControl(QualityControlItem qualityControlItem) {
    try {
      var fileName = qualityControlItem.fileName();
      var experimentId = qualityControlItem.experimentId();
      var content = contentUploadComponent.getContent(fileName)
          .orElse(new ByteArrayInputStream(new byte[]{})).readAllBytes();
      return new QualityControlReport(fileName, experimentId, content);
    } catch (IOException e) {
      throw new ApplicationException("Failed to read quality control content", e);
    }
  }

  public void addConfirmListener(
      ComponentEventListener<ConfirmEvent> listener) {
    addListener(ConfirmEvent.class, listener);
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public static class ConfirmEvent extends ComponentEvent<UploadQualityControlDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ConfirmEvent(UploadQualityControlDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class CancelEvent extends ComponentEvent<UploadQualityControlDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(UploadQualityControlDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }


}
