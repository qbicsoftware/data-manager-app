package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoIcon;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Random;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.ClientDetailsProvider.ClientDetails;
import life.qbic.projectmanagement.application.batch.BatchInformationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.Project;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Batch Details Component
 * <p>
 * Component embedded within the
 * {@link SampleDetailsComponent} in the
 * {@link SampleInformationMain} It allows the
 * user to see the information associated for each {@link Batch} of each
 * {@link Experiment within a {@link Project}
 * Additionally it enables the user to trigger the edit and deletion of the {@link Batch}
 */
@SpringComponent
@UIScope
@PermitAll
public class BatchDetailsComponent extends Div implements Serializable {

  private final Span titleAndControls = new Span();
  private final Span title = new Span("Batches");
  private final Span controls = new Span();
  @Serial
  private static final long serialVersionUID = 4047815658668024042L;
  private final Div content = new Div();
  private final Grid<BatchPreview> batchGrid = new Grid<>();
  private final transient BatchInformationService batchInformationService;
  private final Collection<BatchPreview> batchPreviews = new LinkedHashSet<>();
  private final ClientDetailsProvider clientDetailsProvider;

  public BatchDetailsComponent(ClientDetailsProvider clientDetailsProvider,
      @Autowired BatchInformationService batchInformationService) {
    Objects.requireNonNull(batchInformationService);
    addClassName("batch-details-component");
    layoutComponent();
    this.clientDetailsProvider = clientDetailsProvider;
    this.batchInformationService = batchInformationService;
  }

  private void layoutComponent() {
    title.addClassName("title");
    createLayoutControls();
    add(controls);
    titleAndControls.addClassName("title-and-controls");
    titleAndControls.add(title, controls);
    add(titleAndControls);
    createBatchGrid();
    content.add(batchGrid);
    content.addClassName("content");
    add(content);
  }

  private void createLayoutControls() {
    controls.addClassName("controls");
    Button registerButton = new Button("Register");
    registerButton.addClickListener(event -> createBatch(event.isFromClient()));
    controls.add(registerButton);
  }

  private void createBatchGrid() {
    batchGrid.addColumn(BatchPreview::batchLabel)
        .setHeader("Name").setSortable(true)
        .setTooltipGenerator(BatchPreview::batchLabel).setFlexGrow(1).setAutoWidth(true);
    batchGrid.addColumn(new LocalDateTimeRenderer<>(
            batchPreview -> asClientLocalDateTime(batchPreview.creationDate()),
            "yyyy-MM-dd")).setKey("creationDate").setHeader("Date Created").setSortable(true)
        .setSortProperty("creationDate").setAutoWidth(true);
    batchGrid.addColumn(new LocalDateTimeRenderer<>(
            batchPreview -> asClientLocalDateTime(batchPreview.modificationDate()),
            "yyyy-MM-dd")).setKey("modificationDate").setHeader("Date Modified").setSortable(true)
        .setSortProperty("modificationDate").setAutoWidth(true);
    batchGrid.addColumn(BatchPreview::sampleCount).setHeader("Samples")
        .setSortable(true)
        .setTooltipGenerator(batchPreview -> String.valueOf(batchPreview.sampleCount()))
        .setAutoWidth(true);
    batchGrid.addComponentColumn(this::generateEditorButtons).setAutoWidth(true);
    batchGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
    batchGrid.addClassName("batch-grid");
  }

  public void setExperiment(Experiment experiment) {
    batchPreviews.clear();
    loadBatchesForExperiment(experiment);
    batchGrid.setItems(batchPreviews);
  }

  private Span generateEditorButtons(BatchPreview batchPreview) {
    Icon viewIcon = LumoIcon.EYE.create();
    Icon editIcon = LumoIcon.EDIT.create();
    Icon deleteIcon = VaadinIcon.TRASH.create();
    Button viewButton = new Button(viewIcon);
    Button editButton = new Button(editIcon);
    Button deleteButton = new Button(deleteIcon);
    viewButton.addClickListener(e -> viewBatch(batchPreview, e.isFromClient()));
    deleteButton.addClickListener(e -> removeBatch(batchPreview, e.isFromClient()));
    editButton.addClickListener(e -> updateBatch(batchPreview, e.isFromClient()));
    viewButton.setTooltipText("View Samples for Batch");
    editButton.setTooltipText("Edit Batch");
    deleteButton.setTooltipText("Delete Batch");
    Span buttons = new Span(viewButton, editButton, deleteButton);
    buttons.addClassName("editor-buttons");
    return buttons;
  }

  private BatchPreview generatePreviewFromBatch(Batch batch) {
    //Todo get Dates from Batch
    Instant modificationDate = Instant.now();
    Instant creationDate = Instant.now();
    //Todo Get SampleCount from Batch
    Random rand = new Random();
    int sampleCount = rand.nextInt(999);
    return new BatchPreview(batch.batchId(), batch.label(), creationDate, modificationDate,
        sampleCount);
  }

  private void viewBatch(BatchPreview batchPreview, boolean fromClient) {
    ViewBatchEvent viewBatchEvent = new ViewBatchEvent(this, batchPreview, fromClient);
    fireEvent(viewBatchEvent);
  }

  private void createBatch(boolean fromClient) {
    CreateBatchEvent createBatchEvent = new CreateBatchEvent(this, fromClient);
    fireEvent(createBatchEvent);
  }

  private void updateBatch(BatchPreview batchPreview, boolean fromClient) {
    EditBatchEvent editBatchEvent = new EditBatchEvent(this, batchPreview, fromClient);
    fireEvent(editBatchEvent);
  }

  private void removeBatch(BatchPreview batchPreview, boolean fromClient) {
    DeleteBatchEvent deleteBatchEvent = new DeleteBatchEvent(this, batchPreview, fromClient);
    fireEvent(deleteBatchEvent);
  }


  private void loadBatchesForExperiment(Experiment experiment) {
    batchInformationService.retrieveBatchesForExperiment(experiment.experimentId())
        .onValue(batches -> batchPreviews.addAll(
            batches.stream().map(this::generatePreviewFromBatch).toList()));
  }

  /**
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link ViewBatchEvent}, as soon as a user wants to view a {@link Batch}
   *
   * @param batchViewListener a listener on the batch view trigger
   */
  public void addBatchViewListener(
      ComponentEventListener<ViewBatchEvent> batchViewListener) {
    addListener(ViewBatchEvent.class, batchViewListener);
  }

  /**
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link CreateBatchEvent}, as soon as a user wants to create a {@link Batch}
   *
   * @param batchCreationListener a listener on the batch creation trigger
   */
  public void addBatchCreationListener(
      ComponentEventListener<CreateBatchEvent> batchCreationListener) {
    addListener(CreateBatchEvent.class, batchCreationListener);
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a
   * {@link EditBatchEvent}, as soon as a user wants to edit batch Information.
   *
   * @param batchEditListener a listener on the batch edit trigger
   */
  public void addBatchEditListener(ComponentEventListener<EditBatchEvent> batchEditListener) {
    addListener(EditBatchEvent.class, batchEditListener);
  }

  /**
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link DeleteBatchEvent}, as soon as a user wants to delete a {@link Batch}
   *
   * @param batchDeletionListener a listener on the batch deletion trigger
   */
  public void addBatchDeletionListener(
      ComponentEventListener<DeleteBatchEvent> batchDeletionListener) {
    addListener(DeleteBatchEvent.class, batchDeletionListener);
  }

  private LocalDateTime asClientLocalDateTime(Instant instant) {
    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(
        this.clientDetailsProvider.latestDetails().map(ClientDetails::timeZoneId).orElse("UTC")));
    return zonedDateTime.toLocalDateTime();
  }

  public static class BatchPreview implements Serializable {

    @Serial
    private static final long serialVersionUID = 5781276711398861714L;
    private BatchId batchId;
    private String batchLabel;
    private Instant creationDate;
    private Instant modificationDate;

    //Todo replace with collection of samples?
    private int sampleCount;

    public BatchPreview(BatchId batchId, String batchLabel, Instant creationDate,
        Instant modificationDate, int sampleCount) {
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

  /**
   * <b>View Batch Event</b>
   *
   * <p>Indicates that a user wants to view a {@link Batch}
   * within the {@link BatchDetailsComponent} of a project</p>
   */
  public static class ViewBatchEvent extends ComponentEvent<BatchDetailsComponent> {

    @Serial
    private static final long serialVersionUID = -5108638994476271770L;

    private final BatchPreview batchPreview;

    public ViewBatchEvent(BatchDetailsComponent source, BatchPreview batchPreview,
        boolean fromClient) {
      super(source, fromClient);
      this.batchPreview = batchPreview;
    }

    public BatchPreview batchPreview() {
      return batchPreview;
    }
  }

  /**
   * <b>Create Batch Event</b>
   *
   * <p>Indicates that a user wants to create a {@link Batch}
   * within the {@link BatchDetailsComponent} of a project</p>
   */
  public static class CreateBatchEvent extends ComponentEvent<BatchDetailsComponent> {

    @Serial
    private static final long serialVersionUID = -3348802351594777933L;

    public CreateBatchEvent(BatchDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * <b>Edit Batch Event</b>
   *
   * <p>Indicates that a user wants to edit {@link Batch} information
   * within the {@link BatchDetailsComponent} of a project</p>
   */
  public static class EditBatchEvent extends ComponentEvent<BatchDetailsComponent> {

    @Serial
    private static final long serialVersionUID = -5424056755722207848L;
    private final BatchPreview batchPreview;

    public EditBatchEvent(BatchDetailsComponent source, BatchPreview batchPreview,
        boolean fromClient) {
      super(source, fromClient);
      this.batchPreview = batchPreview;
    }

    public BatchPreview batchPreview() {
      return batchPreview;
    }
  }

  /**
   * <b>Delete BatchEvent</b>
   *
   * <p>Indicates that a user wants to delete a {@link Batch}
   * within the {@link BatchDetailsComponent} of a project</p>
   */
  public static class DeleteBatchEvent extends ComponentEvent<BatchDetailsComponent> {

    @Serial
    private static final long serialVersionUID = -5424056755722207848L;
    private final BatchPreview batchPreview;

    public DeleteBatchEvent(BatchDetailsComponent source, BatchPreview batchPreview,
        boolean fromClient) {
      super(source, fromClient);
      this.batchPreview = batchPreview;
    }

    public BatchPreview batchPreview() {
      return batchPreview;
    }
  }

}
