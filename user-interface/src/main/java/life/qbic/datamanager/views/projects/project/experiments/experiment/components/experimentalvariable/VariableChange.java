package life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable;

import java.util.List;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableAdded;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableDeleted;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableLevelsChanged;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableRenamed;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableUnitChanged;
import org.springframework.lang.Nullable;

/**
 * A change to an experimental variable. Classes extending this interface hold information about a
 * change of the experimental variable.
 */
public sealed interface VariableChange permits VariableAdded, VariableRenamed,
    VariableLevelsChanged, VariableDeleted, VariableUnitChanged {

  String affectedVariable();

  /**
   * A variable was added
   *
   * @param name   the name of the new variable
   * @param unit   the unit of the new variable
   * @param levels the levels the new variable can have
   */
  record VariableAdded(String name, String unit, List<String> levels) implements
      VariableChange {

    @Override
    public String affectedVariable() {
      return name;
    }
  }

  /**
   * A variable was renamed
   * @param oldName the name before renaming
   * @param newName the name after renaming
   */
  record VariableRenamed(String oldName, String newName) implements VariableChange {

    @Override
    public String affectedVariable() {
      return oldName;
    }
  }

  /**
   * Levels of a variable changed
   * @param name the name of the variable
   * @param levels the levels after changes applied
   */
  record VariableLevelsChanged(String name, List<String> levels) implements
      VariableChange {

    @Override
    public String affectedVariable() {
      return name;
    }
  }

  /**
   * The unit of a variable changed
   *
   * @param name    the name of the variable
   * @param oldUnit the unit before change, can be null
   * @param newUnit the unit after change, can be null
   */
  record VariableUnitChanged(String name, @Nullable String oldUnit,
                             @Nullable String newUnit) implements
      VariableChange {

    @Override
    public String affectedVariable() {
      return name;
    }
  }

  /**
   * A variable was deleted
   * @param name the name of the variable that was deleted
   */
  record VariableDeleted(String name) implements VariableChange {

    @Override
    public String affectedVariable() {
      return name;
    }
  }
}
