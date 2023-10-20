package life.qbic.controlling.infrastructure.sample;

import java.util.List;
import life.qbic.controlling.domain.model.project.Project;
import life.qbic.controlling.domain.model.sample.Sample;
import life.qbic.controlling.domain.model.sample.SampleCode;


/**
 * <b>Sample Data Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles the {@link Sample} data storage.
 *
 * @since 1.0.0
 */
public interface QbicSampleDataRepo {

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
   * Deletes a sample with the provided code from persistence.
   *
   * @param sampleCode the {@link SampleCode} of the sample to delete
   * @since 1.0.0
   */
  void delete(SampleCode sampleCode);

  /**
   * Searches for samples that contain the provided sample code
   *
   * @param sampleCode the {@link SampleCode} to search for in the data repository
   * @return true, if a sample with that code already exists in the system, false if not
   * @since 1.0.0
   */
  boolean sampleExists(SampleCode sampleCode);

}
