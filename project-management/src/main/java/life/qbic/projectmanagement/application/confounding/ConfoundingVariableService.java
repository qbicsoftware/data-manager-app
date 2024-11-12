package life.qbic.projectmanagement.application.confounding;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is the aggregate for confounding variables. You can
 * {@link #listConfoundingVariablesForExperiment(ProjectId, ExperimentReference)} and for each
 * variable list the existing levels with
 * {@link #listLevelsForVariable(ProjectId, ExperimentReference, VariableReference)}.
 * <br>
 * Further you can get the level of a specific variable for a specific sample with
 * {@link #getVariableLevelForSample(ProjectId, ExperimentReference, SampleReference,
 * VariableReference)}
 * <br>
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
  record ExperimentReference(String id) {

  }

  /**
   * A reference to a sample
   * @param id the identifier of the sample
   */
  record SampleReference(String id) {

  }

  /**
   * A reference to a variable
   * @param id the identifier of the variable
   */
  record VariableReference(long id) {

  }

  /**
   * Information about a confounding variable
   * @param id the identifier of the variable
   * @param variableName the name of the variable
   */
  record ConfoundingVariableInformation(VariableReference id, String variableName) {

  }

  /**
   * A level of a variable on a sample
   * @param variable the reference of the variable
   * @param sample the reference of the sample
   * @param level the value the variable has for the specific sample.
   */
  record ConfoundingVariableLevel(VariableReference variable, SampleReference sample,
                                  String level) {

  }

  /**
   * List all confounding variables for a given experiment.
   * @param projectId the project identifier for which to fetch data
   * @param experiment the experiment for which to list the confounding variables
   * @return a list of confounding variable information describing all confounding variables in the experiment.
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  @Transactional(readOnly = true)
  List<ConfoundingVariableInformation> listConfoundingVariablesForExperiment(ProjectId projectId,
      ExperimentReference experiment);

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
  @Transactional(readOnly = true)
  Optional<ConfoundingVariableLevel> getVariableLevelForSample(ProjectId projectId,
      SampleReference sampleReference,
      VariableReference variableReference);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  @Transactional(readOnly = true)
  List<ConfoundingVariableLevel> listLevelsForVariable(ProjectId projectId,
      ExperimentReference experiment,
      VariableReference variableReference);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  ConfoundingVariableInformation createConfoundingVariable(ProjectId projectId,
      ExperimentReference experiment,
      String variableName);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  ConfoundingVariableLevel setVariableLevelForSample(ProjectId projectId,
      ExperimentReference experiment, SampleReference sampleReference,
      VariableReference variableReference, String level);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  VariableReference deleteConfoundingVariable(ProjectId projectId, ExperimentReference experiment,
      VariableReference variableReference);

}
