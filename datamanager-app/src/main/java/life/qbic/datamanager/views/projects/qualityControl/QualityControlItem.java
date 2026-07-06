package life.qbic.datamanager.views.projects.qualityControl;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

/**
 * <b>QualityControl Item</b>
 *
 * <p>Describes an uploaded QualityControl Item and a combobox which allows the user to
 * associate the quality control with an experiment within the project.</p>
 */
public class QualityControlItem extends Div {

  @Serial
  private static final long serialVersionUID = -1266444866470199274L;
  private final String fileName;
  private final Span fileNameLabel;
  private final ComboBox<ExperimentItem> experiments;

  public QualityControlItem(String fileName, List<ExperimentItem> experimentLists) {
    this.fileName = fileName;
    var fileIcon = VaadinIcon.FILE.create();
    fileIcon.addClassName("file-icon");
    fileNameLabel = new Span(fileIcon, new Span(fileName));
    fileNameLabel.addClassName("file-name");
    experiments = new ComboBox<>();
    experiments.setLabel("Experiment Name");
    experiments.setPlaceholder("Please select an experiment");
    experiments.setItems(experimentLists);
    experiments.setRenderer(
        new ComponentRenderer<>(experimentItem -> new Span(experimentItem.experimentName)));
    experiments.setItemLabelGenerator(ExperimentItem::experimentName);
    add(fileNameLabel, experiments);
    addClassName("quality-control-item");
  }

  public String fileName() {
    return fileName;
  }

  public ExperimentId experimentId() {
    if (experiments.isEmpty()) {
      return null;
    }
    return experiments.getValue().experimentId();

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualityControlItem that = (QualityControlItem) o;
    return Objects.equals(fileName, that.fileName) && Objects.equals(
        fileNameLabel, that.fileNameLabel) && Objects.equals(experiments,
        that.experiments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, fileNameLabel, experiments);
  }

  /**
   * An ExperimentItem record to be used in the {@link QualityControlItem} within the
   * {@link UploadQualityControlDialog}, contains the experimentId and the name of an experiment to
   * be selected by the user after quality control file upload
   *
   * @param experimentId   The {@link ExperimentId} with which the experiment is identifiable
   * @param experimentName the descriptive name of the experiment
   */
  public record ExperimentItem(ExperimentId experimentId, String experimentName) {

  }
}
