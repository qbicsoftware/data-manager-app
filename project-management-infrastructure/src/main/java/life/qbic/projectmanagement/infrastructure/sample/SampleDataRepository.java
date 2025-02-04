package life.qbic.projectmanagement.infrastructure.sample;

import java.util.Collection;
import java.util.List;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;


/**
 * <b>Sample Data Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles the {@link Sample} data storage.
 *
 * @since 1.0.0
 */
public interface SampleDataRepository {

  /**
   * Creates a reference to one or more {@link Sample}s in the data repository to connect project data.
   * A project must be provided, a unique experiment is created with each batch.
   *
   * @param project the {@link Project} for which samples should be created
   * @param samples a list of {@link Sample}s to be created in the data repo
   * @since 1.0.0
   */
  void addSamplesToProject(Project project, List<Sample> samples);

  /**
   * Removes the provided samples from persistence
   *
   * @param projectCode the {@link ProjectCode} of the project these samples belong to
   * @param samples     the {@link Sample} to be removed from the data repository
   */
  void deleteAll(ProjectCode projectCode, Collection<SampleCode> samples);

  void updateAll(Project project, Collection<Sample> samples);

  boolean canDeleteSample(SampleCode sampleCode);
}
