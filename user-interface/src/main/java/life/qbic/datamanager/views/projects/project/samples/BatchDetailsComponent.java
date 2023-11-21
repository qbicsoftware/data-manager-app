package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.ClientDetailsProvider.ClientDetails;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.projectmanagement.application.batch.BatchInformationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Batch Details Component
 * <p>
 * Component embedded within the
 * {@link life.qbic.datamanager.views.projects.project.samples.SampleDetailsComponent} in the
 * {@link life.qbic.datamanager.views.projects.project.samples.SampleInformationMain} It allows the
 * user to see the information associated for each {@link Batch} of each
 * {@link Experiment within a {@link life.qbic.projectmanagement.domain.model.project.Project}
 * Additionally it enables the user to edit and delete a {@link Batch} and propagates successful
 * deletion and editing to the registered {@link BatchDeletionListener} within this component.
 */
@SpringComponent
@UIScope
@PermitAll
public class BatchDetailsComponent extends PageArea implements Serializable {

  private final Span title = new Span("Batches");
  @Serial
  private static final long serialVersionUID = 4047815658668024042L;
  private final Div content = new Div();
  private final Grid<BatchPreview> batchGrid = new Grid<>();
  private final transient BatchRegistrationService batchRegistrationService;
  private final transient BatchInformationService batchInformationService;
  private final Collection<BatchPreview> batchPreviews = new LinkedHashSet<>();
  private final List<BatchDeletionListener> deletionListener = new ArrayList<>();
  private final List<BatchEditListener> editListener = new ArrayList<>();
  private final ClientDetailsProvider clientDetailsProvider;

  public BatchDetailsComponent(ClientDetailsProvider clientDetailsProvider,
      @Autowired BatchRegistrationService batchRegistrationService,
      @Autowired BatchInformationService batchInformationService) {
    Objects.requireNonNull(batchRegistrationService);
    Objects.requireNonNull(batchInformationService);
    addClassName("batch-details-component");
    layoutComponent();
    this.clientDetailsProvider = clientDetailsProvider;
    this.batchRegistrationService = batchRegistrationService;
    this.batchInformationService = batchInformationService;
  }

  private void layoutComponent() {
    this.add(title);
    title.addClassName("title");
    this.add(content);
    content.addClassName("content");
    createBatchGrid();
    content.add(batchGrid);
  }

  private void createBatchGrid() {
    //Todo Replace with SampleSheetEditor
    Editor<BatchPreview> editor = batchGrid.getEditor();
    Grid.Column<BatchPreview> nameColumn = batchGrid.addColumn(BatchPreview::batchLabel)
        .setHeader("Name").setResizable(true).setSortable(true)
        .setTooltipGenerator(BatchPreview::batchLabel);
    batchGrid.addColumn(new LocalDateTimeRenderer<>(
            batchPreview -> asClientLocalDateTime(batchPreview.creationDate()),
            "yyyy-MM-dd HH:mm:ss")).setKey("creationDate").setHeader("Date Created").setSortable(true)
        .setSortProperty("creationDate");
    batchGrid.addColumn(new LocalDateTimeRenderer<>(
            batchPreview -> asClientLocalDateTime(batchPreview.modificationDate()),
            "yyyy-MM-dd HH:mm:ss")).setKey("modificationDate").setHeader("Date Modified").setSortable(true)
        .setSortProperty("modificationDate");
    batchGrid.addColumn(BatchPreview::sampleCount).setHeader("Samples")
        .setResizable(true).setSortable(true)
        .setTooltipGenerator(batchPreview -> String.valueOf(batchPreview.sampleCount()));
    Grid.Column<BatchPreview> editColumn = batchGrid.addComponentColumn(batchPreview -> {
      Icon viewIcon = LumoIcon.EYE.create();
      Icon editIcon = LumoIcon.EDIT.create();
      Icon deleteIcon = VaadinIcon.TRASH.create();
      Button viewButton = new Button(viewIcon);
      Button editButton = new Button(editIcon);
      Button deleteButton = new Button(deleteIcon);
      viewButton.addClickListener(e -> System.out.println("I will show the grid"));
      //Todo replace with Notification
      deleteButton.addClickListener(e -> removeBatch(batchPreview.batchId()));
      //Todo Replace with SampleSheetEditor
      editButton.addClickListener(e -> {
        if (editor.isOpen()) {
          editor.cancel();
        }
        batchGrid.getEditor().editItem(batchPreview);
      });
      viewButton.setTooltipText("View Samples for Batch");
      editButton.setTooltipText("Edit Batch");
      deleteButton.setTooltipText("Delete Batch");
      Span buttons = new Span();
      buttons.add(viewButton, editButton, deleteButton);
      buttons.addClassName(Display.FLEX);
      buttons.addClassName(Gap.SMALL);
      return buttons;
    }).setFlexGrow(0).setAutoWidth(true);
    editor.setBuffered(true);
    Binder<BatchPreview> batchPreviewBinder = new Binder<>(BatchPreview.class);
    batchGrid.getEditor().setBinder(batchPreviewBinder);
    TextField batchLabelField = new TextField();
    batchPreviewBinder.forField(batchLabelField)
        .asRequired("Batch label must not be empty")
        .bind(BatchPreview::batchLabel, BatchPreview::setBatchLabel);
    nameColumn.setEditorComponent(batchLabelField);
    Icon saveIcon = LumoIcon.CHECKMARK.create();
    Button saveButton = new Button(saveIcon, event -> {
      this.updateBatch(editor.getItem().batchId(), batchLabelField.getValue());
      editor.save();
    });
    saveButton.setTooltipText("Save Edit");
    Button cancelButton = new Button(LumoIcon.CROSS.create(),
        e -> editor.cancel());
    cancelButton.addThemeVariants(ButtonVariant.LUMO_ICON,
        ButtonVariant.LUMO_ERROR);
    cancelButton.setTooltipText("Cancel Edit");
    Span editorButtons = new Span(saveButton, cancelButton);
    editorButtons.addClassName(Display.FLEX);
    editorButtons.addClassName(Gap.SMALL);
    editColumn.setEditorComponent(editorButtons);
    batchGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
  }

  public void setExperiments(Collection<Experiment> experiments) {
    batchPreviews.clear();
    experiments.forEach(this::loadBatchesForExperiment);
    batchGrid.setItems(batchPreviews);
  }


  private BatchPreview generatePreviewFromBatch(Batch batch) {
    //Todo get Dates from Batch
    Instant modificationDate = Instant.now();
    Instant creationDate = Instant.now();
    //Todo Get SampleCount from Batch
    Random rand = new Random();
    int sampleCount = rand.nextInt(999);
    return new BatchPreview(batch.batchId(), batch.label(), creationDate, modificationDate, sampleCount);
  }

  //Todo Replace with Events
  private void removeBatch(BatchId batchId) {
    var result = batchRegistrationService.deleteBatch(batchId);
    result.onValue(deletedBatchId -> fireBatchDeletionEvent(new BatchDeletionEvent(
        this, true)));
  }

  private void updateBatch(BatchId batchId, String batchLabel) {
    var result = batchRegistrationService.updateBatch(batchId, batchLabel);
    result.onValue(editedBatchId -> fireBatchEditEvent(new BatchEditEvent(
        this, true)));

  }

  private void loadBatchesForExperiment(Experiment experiment) {
    batchInformationService.retrieveBatchesForExperiment(experiment.experimentId())
        .onValue(batches -> batchPreviews.addAll(
            batches.stream().map(this::generatePreviewFromBatch).toList()));
  }

  /**
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link BatchDeletionEvent}, as soon as a user wants to delete a {@link Batch}
   *
   * @param batchDeletionListener a listener for adding variables events
   * @since 1.0.0
   */
  public void addBatchDeletionListener(BatchDeletionListener batchDeletionListener) {
    deletionListener.add(batchDeletionListener);
  }

  private void fireBatchDeletionEvent(BatchDeletionEvent event) {
    deletionListener.forEach(it -> it.handle(event));
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a
   * {@link BatchEditEvent}, as soon as a user wants to edit batch Information.
   *
   * @param batchEditListener a listener for batch edit events
   * @since 1.0.0
   */
  public void addBatchEditEventListener(BatchEditListener batchEditListener) {
    editListener.add(batchEditListener);
  }

  private void fireBatchEditEvent(BatchEditEvent event) {
    editListener.forEach(it -> it.handle(event));
  }

  private LocalDateTime asClientLocalDateTime(Instant instant) {
    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(
        this.clientDetailsProvider.latestDetails().map(ClientDetails::timeZoneId).orElse("UTC")));
    return zonedDateTime.toLocalDateTime();
  }

  private class BatchPreview {

    private BatchId batchId;
    private String batchLabel;
    private Instant creationDate;
    private Instant modificationDate;

    //Todo replace with collection of samples?
    private int sampleCount;

    public BatchPreview(BatchId batchId, String batchLabel, Instant creationDate, Instant modificationDate, int sampleCount) {
      Objects.requireNonNull(batchId);
      Objects.requireNonNull(batchLabel);
      Objects.requireNonNull(creationDate);
      Objects.requireNonNull(modificationDate);
      this.batchId = batchId;
      this.batchLabel = batchLabel;
      this.creationDate = creationDate;
      this.modificationDate = modificationDate;
      this.sampleCount = sampleCount;
    }

    public BatchId batchId() {
      return batchId;
    }

    public void setBatchId(BatchId batchId) {
      this.batchId = batchId;
    }

    public String batchLabel() {
      return batchLabel;
    }

    public void setBatchLabel(String batchLabel) {
      this.batchLabel = batchLabel;
    }

    public Instant modificationDate() {
      return modificationDate;
    }

    public void setModificationDate(Instant modificationDate) {
      this.modificationDate = modificationDate;
    }

    public Instant creationDate() {
      return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
      this.creationDate = creationDate;
    }

    public int sampleCount() {
      return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
      this.sampleCount = sampleCount;
    }
  }

  @FunctionalInterface
  public interface BatchDeletionListener {

    void handle(BatchDeletionEvent event);
  }

  @FunctionalInterface
  public interface BatchEditListener {

    void handle(BatchEditEvent event);
  }

  /**
   * <b>Batch Edit Event</b>
   *
   * <p>Indicates that a user wants to edit {@link Batch} information
   * within the {@link BatchDetailsComponent} of a project</p>
   */
  public class BatchEditEvent extends ComponentEvent<BatchDetailsComponent> {

    @Serial
    private static final long serialVersionUID = -5424056755722207848L;

    public BatchEditEvent(BatchDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * <b>Batch Edit Event</b>
   *
   * <p>Indicates that a user wants to edit {@link Batch} information
   * within the {@link BatchDetailsComponent} of a project</p>
   */
  public class BatchDeletionEvent extends ComponentEvent<BatchDetailsComponent> {

    @Serial
    private static final long serialVersionUID = -5424056755722207848L;

    public BatchDeletionEvent(BatchDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

}
