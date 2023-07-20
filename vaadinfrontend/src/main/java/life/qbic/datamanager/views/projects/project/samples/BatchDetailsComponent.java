package life.qbic.datamanager.views.projects.project.samples;

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
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchDeletionEvent;
import life.qbic.projectmanagement.application.batch.BatchInformationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Batch Details Component
 * <p>
 * Component embedded within the {@link SampleSupportComponent} in the {@link ProjectViewPage}. It
 * allows the user to see the information associated for each {@link Batch} of each
 * {@link Experiment within a {@link life.qbic.projectmanagement.domain.project.Project}
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
  Grid<BatchPreview> batchGrid = new Grid<>();
  private final transient BatchRegistrationService batchRegistrationService;
  private final transient BatchInformationService batchInformationService;
  private final Collection<BatchPreview> batchPreviews = new LinkedHashSet<>();

  public BatchDetailsComponent(@Autowired BatchRegistrationService batchRegistrationService,
      @Autowired BatchInformationService batchInformationService) {
    Objects.requireNonNull(batchRegistrationService);
    Objects.requireNonNull(batchInformationService);
    addClassName("batch-details-component");
    layoutComponent();
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
    Editor<BatchPreview> editor = batchGrid.getEditor();
    Grid.Column<BatchPreview> nameColumn = batchGrid.addColumn(BatchPreview::batchLabel)
        .setHeader("Name").setResizable(true).setSortable(true)
        .setTooltipGenerator(BatchPreview::batchLabel);
    batchGrid.addColumn(
            batchPreview -> batchPreview.experimentLabel).setHeader("Experiment")
        .setResizable(true).setSortable(true).setTooltipGenerator(BatchPreview::experimentLabel);
    Grid.Column<BatchPreview> editColumn = batchGrid.addComponentColumn(batchPreview -> {
      Icon editIcon = VaadinIcon.EDIT.create();
      editIcon.addClassName(IconSize.SMALL);
      Icon deleteIcon = VaadinIcon.TRASH.create();
      deleteIcon.addClassName(IconSize.SMALL);
      Button editButton = new Button(editIcon);
      Button deleteButton = new Button(deleteIcon);
      editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      deleteButton.addClickListener(e -> this.removeBatch(batchPreview.batchId()));
      editButton.addClickListener(e -> {
        if (editor.isOpen()) {
          editor.cancel();
        }
        batchGrid.getEditor().editItem(batchPreview);
      });
      editButton.setTooltipText("Edit Batch");
      deleteButton.setTooltipText("Delete Batch");
      Span buttons = new Span();
      buttons.add(editButton, deleteButton);
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
    saveIcon.addClassName(IconSize.SMALL);
    Button saveButton = new Button(saveIcon, event -> {
      this.updateBatch(editor.getItem().batchId(), batchLabelField.getValue());
      editor.save();
    });
    saveButton.setTooltipText("Save Edit");
    saveButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    Button cancelButton = new Button(LumoIcon.CROSS.create(),
        e -> editor.cancel());
    cancelButton.addThemeVariants(ButtonVariant.LUMO_ICON,
        ButtonVariant.LUMO_ERROR);
    cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    cancelButton.addClassName(IconSize.SMALL);
    cancelButton.setTooltipText("Cancel Edit");
    Div editorButtons = new Div(saveButton, cancelButton);
    editColumn.setEditorComponent(editorButtons);
    batchGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
  }

  public void setExperiments(Collection<Experiment> experiments) {
    batchPreviews.clear();
    experiments.forEach(this::loadBatchesForExperiment);
    batchGrid.setItems(batchPreviews);
  }

  private BatchPreview generatePreviewFromBatch(Batch batch, Experiment experiment) {
    return new BatchPreview(batch.batchId(), batch.label(), experiment.getName());
  }

  private void removeBatch(BatchId batchId) {
    var result = batchRegistrationService.deleteBatch(batchId);
  }

  private void updateBatch(BatchId batchId, String batchLabel) {
    var result = batchRegistrationService.updateBatch(batchId, batchLabel);
  }

  private void loadBatchesForExperiment(Experiment experiment) {
    batchInformationService.retrieveBatchesForExperiment(experiment.experimentId())
        .onValue(batches -> batchPreviews.addAll(
            batches.stream().map(batch -> generatePreviewFromBatch(batch, experiment)).toList()));
  }

  private class BatchPreview {

    private BatchId batchId;
    private String batchLabel;
    private String experimentLabel;

    public BatchPreview(BatchId batchId, String batchLabel,
        String experimentLabel) {
      Objects.requireNonNull(batchId);
      Objects.requireNonNull(batchLabel);
      Objects.requireNonNull(experimentLabel);
      this.batchId = batchId;
      this.batchLabel = batchLabel;
      this.experimentLabel = experimentLabel;
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

    public String experimentLabel() {
      return experimentLabel;
    }

    public void setExperimentLabel(String experimentLabel) {
      this.experimentLabel = experimentLabel;
    }
  }

  @FunctionalInterface
  public interface BatchDeletionListener {

    void handle(BatchDeletionEvent event);
  }

}
