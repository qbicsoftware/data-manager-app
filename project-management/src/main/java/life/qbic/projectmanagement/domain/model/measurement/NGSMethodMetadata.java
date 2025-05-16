package life.qbic.projectmanagement.domain.model.measurement;

import life.qbic.projectmanagement.domain.model.OntologyTermV1;

/**
 * <b>NGS Method Metadata</b>
 * <p>
 * A logical container aggregating business concepts of measurement method details together.
 *
 * @since 1.0.0
 */
public record NGSMethodMetadata(OntologyTermV1 instrument, String facility,
                                String sequencingReadType,
                                String libraryKit,
                                String flowCell, String sequencingRunProtocol) {

}
