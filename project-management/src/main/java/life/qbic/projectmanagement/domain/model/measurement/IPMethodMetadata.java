package life.qbic.projectmanagement.domain.model.measurement;

import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b>IP Method Metadata</b>
 * <p>
 * A logical container aggregating business concepts of immunopeptidomics measurement method
 * details together.
 *
 * @since 1.11.0
 */
public record IPMethodMetadata(OntologyTerm instrument, String instrumentName,
                                String facility) {

}
