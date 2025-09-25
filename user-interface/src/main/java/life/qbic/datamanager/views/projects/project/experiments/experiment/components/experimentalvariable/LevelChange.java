package life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable;

public sealed interface LevelChange {

  record LevelAdded(int position, String value) implements LevelChange {

  }

  record LevelDeleted(int position, String value) implements LevelChange {

  }

  record LevelMoved(int oldPosition, int newPosition) implements LevelChange {

  }
}
