package life.qbic.projectmanagement.domain.model.measurement;

import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b>Proteomics Method Metadata</b>
 * <p>
 * A logical container aggregating business concepts of measurement method details together.
 *
 * @since 1.0.0
 */
public record ProteomicsMethodMetadata(OntologyTerm instrument, String pooledSampleLabel,
                                       String fractionName,
                                       String fractionationType, String digestionMethod,
                                       String digestionEnzyme, String enrichmentMethod,
                                       int injectionVolume, String lcColumn, String lcmsMethod) {

}
