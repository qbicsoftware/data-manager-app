package life.qbic.projectmanagement.domain.model.measurement;

import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b>Proteomics Method Metadata</b>
 * <p>
 * A logical container aggregating business concepts of measurement method details together.
 *
 * @since 1.0.0
 */
public record ProteomicsMethodMetadata(OntologyTerm instrument, String facility,
                                       String digestionMethod,
                                       String digestionEnzyme, String enrichmentMethod,
                                       String lcColumn, String lcmsMethod, int injectionVolume,
                                       String labelType) {

}
