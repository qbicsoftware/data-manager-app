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
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchDeletionEvent;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.sample.Batch;

/**
 * Batch Details Component
 * <p>
 * Component embedded within the {@link SampleMainComponent} in the {@link ProjectViewPage}. It
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

  public BatchDetailsComponent() {
    this.addClassName("batch-details-component");
    layoutComponent();
  }

  private void layoutComponent() {
    this.add(title);
    title.addClassName("title");
    this.add(content);
    content.addClassName("content");
    createBatchGrid();
    content.add(batchGrid);
    setGridItems();
  }

  private void createBatchGrid() {
    Editor<BatchPreview> editor = batchGrid.getEditor();
    batchGrid.addColumn(BatchPreview::name).setHeader("Name");
    batchGrid.addColumn(batchPreview -> batchPreview.experiment.getName()).setHeader("Experiment");
    batchGrid.addColumn(BatchPreview::date).setHeader("Date");
    batchGrid.addComponentColumn(batchPreview -> {
      Icon editIcon = VaadinIcon.EDIT.create();
      Button editButton = new Button(editIcon);
      editButton.addThemeVariants(ButtonVariant.LUMO_ICON);
      editIcon.addClassName(IconSize.SMALL);
      editButton.addClickListener(e -> {
        if (editor.isOpen()) {
          editor.cancel();
        }
        batchGrid.getEditor().editItem(batchPreview);
      });
      return editIcon;
    });
    batchGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
  }

  private Collection<BatchPreview> createDummyBatchPreviews() {
    return List.of(new BatchPreview("Batch 1", "2023-05-16", Experiment.create("Experiment 1")),
        new BatchPreview("Batch 2", "2023-05-17", Experiment.create("Experiment 2")),
        new BatchPreview("Batch 3", "2023-05-18", Experiment.create("Experiment 3")),
        new BatchPreview("Batch 4", "2023-05-19", Experiment.create("Experiment 4")));
  }

  private void setGridItems() {
    batchGrid.setItems(createDummyBatchPreviews());
  }

  private record BatchPreview(String name, String date, Experiment experiment) {

  }

  @FunctionalInterface
  public interface BatchDeletionListener {

    void handle(BatchDeletionEvent event);
  }

}
