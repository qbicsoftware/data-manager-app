package life.qbic.datamanager.views.general;

import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b>Enforce harmonised CURIE formatting</b>
 * <p>
 * Enforced format: <code>ontology-name:id</code>
 *
 * @since 1.6.0
 */
public class OboIdFormatter {

  public static String render(OntologyTerm term) {
    return enforceColonSeparator(term.getOboId());
  }

  public static String render(String oboId) {
    return enforceColonSeparator(oboId);
  }

  private static String enforceColonSeparator(String term) {
    return term.replace("_", ":");
  }
}
