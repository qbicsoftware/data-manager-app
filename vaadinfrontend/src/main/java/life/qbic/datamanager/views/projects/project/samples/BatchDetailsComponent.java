package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
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
    Grid.Column<BatchPreview> nameColumn = batchGrid.addColumn(BatchPreview::getName)
        .setHeader("Name").setResizable(true);
    batchGrid.addColumn(
            batchPreview -> batchPreview.experiment.getName()).setHeader("Experiment")
        .setResizable(true);
    Grid.Column<BatchPreview> dateColumn = batchGrid.addColumn(BatchPreview::getDate)
        .setHeader("Date")
        .setResizable(true);
    Grid.Column<BatchPreview> editColumn = batchGrid.addComponentColumn(batchPreview -> {
      Icon editIcon = VaadinIcon.EDIT.create();
      editIcon.addClassName(IconSize.SMALL);
      Icon deleteIcon = VaadinIcon.TRASH.create();
      deleteIcon.addClassName(IconSize.SMALL);
      Button editButton = new Button(editIcon);
      Button deleteButton = new Button(deleteIcon);
      editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      deleteButton.addClickListener(e -> this.removeBatch(batchPreview));
      editButton.addClickListener(e -> {
        if (editor.isOpen()) {
          editor.cancel();
        }
        batchGrid.getEditor().editItem(batchPreview);
      });
      Span buttons = new Span();
      buttons.add(editButton, deleteButton);
      return buttons;
    }).setFlexGrow(0);
    editor.setBuffered(true);

    Binder<BatchPreview> batchPreviewBinder = new Binder<>(BatchPreview.class);
    batchGrid.getEditor().setBinder(batchPreviewBinder);
    TextField nameField = new TextField();
    batchPreviewBinder.forField(nameField)
        .asRequired("First name must not be empty")
        .bind(BatchPreview::getName, BatchPreview::setName);
    nameColumn.setEditorComponent(nameField);
    DatePicker datePicker = new DatePicker();
    batchPreviewBinder.forField(datePicker).asRequired("Last name must not be empty")
        .bind(BatchPreview::getDate, BatchPreview::setDate);
    dateColumn.setEditorComponent(datePicker);
    Button saveButton = new Button("Save", event -> editor.save());
    editColumn.setEditorComponent(saveButton);
  }

  private void removeBatch(BatchPreview batchPreview) {
    //Todo Remove Batch
  }

  private Collection<BatchPreview> createDummyBatchPreviews() {
    return List.of(new BatchPreview("Batch 1", LocalDate.parse("2023-05-16"),
            Experiment.create("Experiment 1")),
        new BatchPreview("Batch 2", LocalDate.parse("2023-05-17"),
            Experiment.create("Experiment 2")),
        new BatchPreview("Batch 3", LocalDate.parse("2023-05-18"),
            Experiment.create("Experiment 3")),
        new BatchPreview("Batch 4", LocalDate.parse("2023-05-19"),
            Experiment.create("Experiment 4")));
  }

  private void setGridItems() {
    batchGrid.setItems(createDummyBatchPreviews());
  }

  private class BatchPreview {

    private String name;
    private LocalDate date;
    private Experiment experiment;

    public BatchPreview(String name, LocalDate date, Experiment experiment) {
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

    public LocalDate getDate() {
      return date;
    }

    public void setDate(LocalDate date) {
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
