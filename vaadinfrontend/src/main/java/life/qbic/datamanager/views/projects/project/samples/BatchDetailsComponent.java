package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.gridpro.ItemUpdater;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
  GridPro<BatchPreview> batchGrid = new GridPro<>();

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
    Binder<BatchPreview> batchPreviewBinder = new Binder<>(BatchPreview.class);
    editor.setBinder(batchPreviewBinder);
    batchGrid.addEditColumn(BatchPreview::getName).text(BatchPreview::setName)
        .setHeader("Name").setResizable(true);
    batchGrid.addEditColumn(batchPreview -> batchPreview.experiment.getName())
        .select(styleExperimentValue, createExperimentList()).setHeader("Experiment")
        .setResizable(true);
    batchGrid.addEditColumn(BatchPreview::getDate).text(BatchPreview::setDate).setHeader("Date")
        .setResizable(true);
    batchGrid.addComponentColumn(batchPreview -> {
      Icon editIcon = VaadinIcon.EDIT.create();
      Button editButton = new Button(editIcon);
      editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      editIcon.addClassName(IconSize.SMALL);
      editButton.addClickListener(e -> {
        if (editor.isOpen()) {
          editor.cancel();
        }
        batchGrid.getEditor().editItem(batchPreview);
      });
      return editButton;
    }).setWidth("3em").setFlexGrow(0);
    batchGrid.setEditOnClick(true);

  }

  private static final ItemUpdater<BatchPreview, String> styleExperimentValue = (batchPreview, string) -> string = batchPreview.experiment.getName();

  private List<String> createExperimentList() {
    return List.of("Experiment 1", "Experiment 2",
        "Experiment 3", "Experiment 4");
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

  private class BatchPreview {

    private String name;
    private String date;
    private Experiment experiment;

    public BatchPreview(String name, String date, Experiment experiment) {
      Objects.requireNonNull(name);
      Objects.requireNonNull(date);
      Objects.requireNonNull(experiment);
      this.name = name;
      this.date = date;
      this.experiment = experiment;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }

    public Experiment getExperiment() {
      return experiment;
    }

    public void setExperiment(Experiment experiment) {
      this.experiment = experiment;
    }
  }

  @FunctionalInterface
  public interface BatchDeletionListener {

    void handle(BatchDeletionEvent event);
  }

}
