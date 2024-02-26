package life.qbic.projectmanagement.domain.model.measurement;

import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record ProteomicsMethodMetadata(OntologyTerm instrument, String pooledSampleLabel, String fractionName,
                                       String fractionationType, String digestionMethod,
                                       String digestionEnzyme, String enrichmentMethod,
                                       int injectionVolume, String lcColumn, String lcmsMethod) {

}
