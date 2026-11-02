package life.qbic.projectmanagement.infrastructure.query;

/**
 * Project-scoped SQL for the project's assay standing: one row per primary dataset.
 *
 * <p><b>The derivation lives in the view, not here.</b> {@code v_project_assay_standing}
 * ({@code sql/migrations/create-assay-standing-view.sql}) is the single derivation of a project's
 * primary datasets, shared with every other surface that needs it. This class only applies the
 * project filter.</p>
 *
 * <p>It is written as a project-scoped query rather than a direct view read for the same reason
 * {@link ExperimentProgressQuery} is: filtering a view on an outer column the database cannot push
 * into its grouped subqueries would aggregate every project and throw all but one away.</p>
 *
 * <p><b>Grain.</b> One row per {@code (experiment, analysis method)}. A primary dataset is the data
 * of one experiment for one analysis method, so two experiments measured as WES are two datasets and
 * one experiment measured as WES and RNA-Seq is two as well. That pair is the identity; an analysis
 * method on its own is not, which is why this query does not aggregate them together.</p>
 *
 * <p><b>On the analysis method's source.</b> The view resolves it from {@code sample.analysis_method}
 * -- the current annotation and a known grandfather relation. See ADR 0010 before changing anything
 * about it; in particular, do not re-derive the analysis method here or anywhere else.</p>
 *
 * @see ExperimentProgressQuery
 * @since 1.22.0
 */
final class AssayStandingQuery {

  /**
   * The project-scoped form of {@code v_project_assay_standing}, parameterised by
   * {@code :projectId}.
   */
  static final String BY_PROJECT = """
      SELECT project_id,
             experiment_id,
             experiment_name,
             analysis_method,
             measurements,
             measurements_with_primary_data,
             primary_data_bytes
      FROM v_project_assay_standing
      WHERE project_id = :projectId
      ORDER BY experiment_name, analysis_method
      """;

  private AssayStandingQuery() {
    // static SQL holder
  }
}
