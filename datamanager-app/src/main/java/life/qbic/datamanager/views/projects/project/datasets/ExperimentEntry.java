package life.qbic.datamanager.views.projects.project.datasets;

import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import org.jspecify.annotations.NonNull;

/**
 * A lightweight mapping of an {@link ExperimentId} to its display name, used to
 * populate the "link to experiment" selector in {@link ConnectDatasetSidebar}.
 *
 * @see ConnectDatasetSidebar
 */
record ExperimentEntry(ExperimentId id, String label) {

  @Override
  public @NonNull String toString() {
    return label;
  }
}