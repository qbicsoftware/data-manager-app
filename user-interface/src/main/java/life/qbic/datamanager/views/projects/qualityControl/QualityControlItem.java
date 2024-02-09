package life.qbic.datamanager.views.projects.qualityControl;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

/**
 * <b>QualityControl Item</b>
 *
 * <p>Describes an uploaded QualityControl Item) and a combobox which allows the user to associate
 * the quality control with an experiment within the project.</p>
 */
public class QualityControlItem extends Div {

  @Serial
  private static final long serialVersionUID = -1266444866470199274L;
  private final String fileName;
  private final Span fileNameLabel;
  private final ComboBox<ExperimentId> experiments;

  public QualityControlItem(String fileName) {
    this.fileName = fileName;
    var fileIcon = VaadinIcon.FILE.create();
    fileIcon.addClassName("file-icon");
    fileNameLabel = new Span(fileIcon, new Span(fileName));
    fileNameLabel.addClassName("file-name");
    experiments = new ComboBox<>();
    experiments.setLabel("Experiment Name");
    experiments.setPlaceholder("Please select an experiment");
    add(fileNameLabel, experiments);
    addClassName("quality-control-item");
  }

  public String fileName() {
    return fileName;
  }

  public ExperimentId experimentId() {
    return experiments.getValue();
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
}
