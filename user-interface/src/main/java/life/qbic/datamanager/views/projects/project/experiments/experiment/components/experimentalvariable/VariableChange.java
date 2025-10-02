package life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable;

import java.util.List;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableAdded;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableDeleted;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableLevelsChanged;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableRenamed;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableUnitChanged;

public sealed interface VariableChange permits VariableAdded, VariableRenamed,
    VariableLevelsChanged, VariableDeleted, VariableUnitChanged {

  String affectedVariable();

  record VariableAdded(String name, String unit, List<String> levels) implements
      VariableChange {

    @Override
    public String affectedVariable() {
      return name;
    }
  }

  record VariableRenamed(String oldName, String newName) implements VariableChange {

    @Override
    public String affectedVariable() {
      return oldName;
    }
  }

  record VariableLevelsChanged(String name, List<String> levels) implements
      VariableChange {

    @Override
    public String affectedVariable() {
      return name;
    }
  }

  record VariableUnitChanged(String name, String oldUnit, String newUnit) implements
      VariableChange {

    @Override
    public String affectedVariable() {
      return name;
    }
  }

  record VariableDeleted(String name) implements VariableChange {

    @Override
    public String affectedVariable() {
      return name;
    }
  }
}
