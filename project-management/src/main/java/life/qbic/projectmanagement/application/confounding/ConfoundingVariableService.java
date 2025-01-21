package life.qbic.projectmanagement.application.confounding;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * This is the aggregate for confounding variables. You can
 * {@link #listConfoundingVariablesForExperiment(String, ExperimentReference)} and for each
 * variable list the existing levels with
 * {@link #listLevelsForVariable(String, VariableReference)}.
 * <p>
 * Further you can get the level of a specific variable for a specific sample with
 * {@link #getVariableLevelForSample(String, SampleReference, VariableReference)} (ProjectId, ExperimentReference, SampleReference,
 * VariableReference)}
 * <p>
 * Apart from listing available information this service is the single point in the application
 * where confounding variables may be modified. Please note: variable levels do not have an
 * identifier as they are purely value objects and may be overwritten if changes occur. The service
 * deletes the old level when a new level is set for a specific sample.
 *
 * @since 1.6.0
 */
public interface ConfoundingVariableService {

  /**
   * A reference to an experiment
   *
   * @param id the identifier of the experiment
   */
  record ExperimentReference(String id) implements Serializable {

  }

  /**
   * A reference to a sample
   * @param id the identifier of the sample
   */
  record SampleReference(String id) implements Serializable {

  }

  /**
   * A reference to a variable
   * @param id the identifier of the variable
   */
  record VariableReference(long id) implements Serializable {

  }

  /**
   * Information about a confounding variable
   * @param id the identifier of the variable
   * @param variableName the name of the variable
   */
  record ConfoundingVariableInformation(VariableReference id, String variableName) implements
      Serializable {

  }

  /**
   * A level of a variable on a sample
   * @param variable the reference of the variable
   * @param sample the reference of the sample
   * @param level the value the variable has for the specific sample.
   */
  record ConfoundingVariableLevel(VariableReference variable, SampleReference sample,
                                  String level) implements Serializable {

  }

  /**
   * List all confounding variables for a given experiment.
   * @param projectId the project identifier for which to fetch data
   * @param experiment the experiment for which to list the confounding variables
   * @return a list of confounding variable information describing all confounding variables in the experiment.
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  List<ConfoundingVariableInformation> listConfoundingVariablesForExperiment(String projectId,
      ExperimentReference experiment);

  /**
   * List all confounding variables for a given experiment.
   *
   * @param projectId the project identifier for which to fetch data
   * @param variables the variables for which to load information
   * @return a list of confounding variable information describing all confounding variables in the
   * experiment.
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  List<ConfoundingVariableInformation> loadInformationForVariables(String projectId,
          List<VariableReference> variables);


  /**
   * Get the level of a variable for a specific sample. {@link Optional#empty()} if no level is set
   * for the specific variable.
   *
   * @param projectId         the identifier of the project
   * @param sampleReference   the reference of the sample
   * @param variableReference the reference to the variable
   * @return the {@link ConfoundingVariableLevel} of the sample, {@link Optional#empty()} otherwise.
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  Optional<ConfoundingVariableLevel> getVariableLevelForSample(String projectId,
      SampleReference sampleReference,
      VariableReference variableReference);

  /**
   * Lists the levels of a confounding variable
   *
   * @param projectId         the identifier of the project
   * @param variableReference the reference of the variable
   * @return a list of levels for the confounding variable. The list is empty if no levels exist.
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  List<ConfoundingVariableLevel> listLevelsForVariable(String projectId,
      VariableReference variableReference);

  /**
   * Lists the levels of a confounding variable
   *
   * @param projectId          the identifier of the project
   * @param variableReferences references of the variables
   * @return a list of levels for the confounding variable. The list is empty if no levels exist.
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  List<ConfoundingVariableLevel> listLevelsForVariables(String projectId,
      List<VariableReference> variableReferences);

  /**
   * Creates a confounding variable in an experiment. Information about the created confounding
   * variable is returned.
   *
   * @param projectId    the identifier of the project
   * @param experiment   a reference to the experiment in which to create the confounding variable.
   * @param variableName the name of the confounding variable.
   * @return information about the created confounding variable
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  ConfoundingVariableInformation createConfoundingVariable(String projectId,
      ExperimentReference experiment,
      String variableName);

  /**
   * Set the level a sample has for a confounding variable.
   * <p>
   * Overwrites an existing level of set sample in the variable, if present.
   *
   * @param projectId         the identifier of the project
   * @param experiment        the experiment containing the variable
   * @param sampleReference   the sample for which to set the level
   * @param variableReference the variable for which to set the level
   * @param level             the value of the level
   * @return the created confounding variable level
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  ConfoundingVariableLevel setVariableLevelForSample(String projectId,
      ExperimentReference experiment, SampleReference sampleReference,
      VariableReference variableReference, String level);

  /**
   * Set the level a sample has for a confounding variable.
   * <p>
   * Overwrites an existing level of set sample in the variable, if present.
   *
   * @param projectId         the identifier of the project
   * @param experiment        the experiment containing the variable
   * @param sampleReference   the sample for which to set the level
   * @param levels            a list of values as levels for the variable
   * @return the created confounding variable level
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  List<ConfoundingVariableLevel> setVariableLevelsForSample(String projectId,
      ExperimentReference experiment, SampleReference sampleReference,
      Map<VariableReference, String> levels);

  /**
   * Deletes a confounding variable and all information about the variable.
   * <p>
   * <i><b>Please note:</b> This will permanently delete all levels of the variable for all samples.</i>
   * @param projectId the identifier of the project
   * @param experiment the experiment containing the confounding variable
   * @param variableReference the confounding variable to delete.
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  void deleteConfoundingVariable(String projectId, ExperimentReference experiment,
      VariableReference variableReference);

}
