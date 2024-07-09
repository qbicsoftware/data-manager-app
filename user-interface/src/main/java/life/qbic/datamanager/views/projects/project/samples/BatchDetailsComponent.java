package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.SortDirection;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.ClientDetailsProvider.ClientDetails;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.projectmanagement.application.batch.BatchInformationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Batch Details Component
 * <p>
 * Component embedded within the {@link SampleInformationMain} It allows the user to see the
 * information associated for each {@link Batch} of each
 * {@link Experiment within a {@link Project} Additionally it enables the user to trigger the edit
 * and deletion of the {@link Batch}
 */
@SpringComponent
@UIScope
@PermitAll
public class BatchDetailsComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 4047815658668024042L;
  private final Span titleAndControls = new Span();
  private final Span title = new Span("Batches");
  private final Div content = new Div();
  private final Grid<BatchPreview> batchGrid = new Grid<>();
  private final transient BatchInformationService batchInformationService;
  private final ClientDetailsProvider clientDetailsProvider;
  private Context context;

  public BatchDetailsComponent(@Autowired BatchInformationService batchInformationService,
      ClientDetailsProvider clientDetailsProvider) {
    this.batchInformationService = Objects.requireNonNull(batchInformationService,
        "BatchInformationService cannot be null");
    this.clientDetailsProvider = Objects.requireNonNull(clientDetailsProvider,
        "ClientDetailsProvider cannot be null");
    addClassName("batch-details-component");
    createTitleAndControls();
    createBatchGrid();
    add(content);
  }

  private void createTitleAndControls() {
    title.addClassName("title");
    titleAndControls.addClassName("title-and-controls");
    Button registerButton = new Button("Register");
    registerButton.addClickListener(event -> fireEvent(new CreateBatchEvent(this,
        event.isFromClient())));
    titleAndControls.add(title, registerButton);
    add(titleAndControls);
  }

  private void createBatchGrid() {
    content.add(batchGrid);
    content.addClassName("content");
    batchGrid.addColumn(BatchPreview::batchLabel)
        .setHeader("Name").setSortable(true)
        .setTooltipGenerator(BatchPreview::batchLabel)
        .setAutoWidth(true)
        .setResizable(true);
    batchGrid.addColumn(new LocalDateTimeRenderer<>(
            batchPreview -> asClientLocalDateTime(batchPreview.createdOn()),
            "yyyy-MM-dd"))
        .setKey("createdOn")
        .setHeader("Date Created")
        .setSortable(true)
        .setComparator(BatchPreview::createdOn)
        .setAutoWidth(true);
    batchGrid.addColumn(new LocalDateTimeRenderer<>(
            batchPreview -> asClientLocalDateTime(batchPreview.lastModified()),
            "yyyy-MM-dd"))
        .setKey("lastModified")
        .setHeader("Date Modified")
        .setSortable(true)
        .setComparator(BatchPreview::lastModified)
        .setAutoWidth(true);
    batchGrid.addColumn(batchPreview -> batchPreview.samples.size())
        .setKey("samples")
        .setHeader("Samples")
        .setSortable(true)
        .setAutoWidth(true);
    batchGrid.addComponentColumn(this::generateEditorButtons)
        .setAutoWidth(true)
        .setHeader("Action")
        .setFrozenToEnd(true);
    batchGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
    batchGrid.addClassName("batch-grid");
    batchGrid.setAllRowsVisible(true);
  }

  public void setContext(Context context) {
    if (context.experimentId().isEmpty()) {
      throw new ApplicationException("no experiment id in context " + context);
    }
    if (context.projectId().isEmpty()) {
      throw new ApplicationException("no project id in context " + context);
    }
    this.context = context;
    updateBatchGridDataProvider(context.experimentId().get());
  }

  private Span generateEditorButtons(BatchPreview batchPreview) {
    Icon editIcon = LumoIcon.EDIT.create();
    Icon deleteIcon = VaadinIcon.TRASH.create();
    Button editButton = new Button(editIcon);
    Button deleteButton = new Button(deleteIcon);
    editButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE);
    deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE,
        ButtonVariant.LUMO_ERROR);
    deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE);
    deleteButton.addClickListener(e -> fireEvent(new DeleteBatchEvent(this, batchPreview.batchId(),
        e.isFromClient())));
    editButton.addClickListener(
        e -> fireEvent(new EditBatchEvent(this, batchPreview, e.isFromClient())));
    editButton.setTooltipText("Edit Batch");
    deleteButton.setTooltipText("Delete Batch");
    Span buttons = new Span(editButton, deleteButton);
    buttons.addClassName("editor-buttons");
    return buttons;
  }

  private BatchPreview generatePreviewFromBatch(Batch batch) {
    return new BatchPreview(batch.batchId(), batch.label(), batch.samples(), batch.createdOn(),
        batch.lastModified());
  }


  private void updateBatchGridDataProvider(ExperimentId experimentId) {
    List<BatchPreview> experimentBatches = batchInformationService.retrieveBatchesForExperiment(
            experimentId)
        .map(Collection::stream)
        .map(batchStream -> batchStream.map(this::generatePreviewFromBatch))
        .map(Stream::toList).getValue();
    batchGrid.setItems(experimentBatches);
    batchGrid.getListDataView().setSortOrder(BatchPreview::batchLabel, SortDirection.DESCENDING);
    batchGrid.recalculateColumnWidths();
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

  public record BatchPreview(BatchId batchId, String batchLabel, List<SampleId> samples,
                             Instant createdOn,
                             Instant lastModified) implements Serializable {

    @Serial
    private static final long serialVersionUID = 5781276711398861714L;

    public BatchPreview {
      Objects.requireNonNull(batchId);
      Objects.requireNonNull(batchLabel);
      Objects.requireNonNull(samples);
      Objects.requireNonNull(createdOn);
      Objects.requireNonNull(lastModified);
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
    private final BatchId batchId;

    public DeleteBatchEvent(BatchDetailsComponent source, BatchId batchId,
        boolean fromClient) {
      super(source, fromClient);
      this.batchId = batchId;
    }

    public BatchId batchId() {
      return batchId;
    }
  }
}
