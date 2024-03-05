package life.qbic.projectmanagement.domain.model.measurement;

/**
 * <b>Proteomics Sample Preparation</b>
 *
 * <p>Another proteomics measurement metadata container, that aggregates business concepts that
 * belong to sample preparation</p>
 *
 * @since 1.0.0
 */
public record ProteomicsSamplePreparation(String preparation, String cleanupProtein,
                                          String cleanupPeptide, String note) {

}
