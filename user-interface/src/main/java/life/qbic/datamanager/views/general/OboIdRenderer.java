package life.qbic.datamanager.views.general;

import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class OboIdRenderer {

  public static String render(OntologyTerm term) {
    return enforceColonSeparator(term.getOboId());
  }

  public static String render(String oboId) {
    return enforceColonSeparator(oboId);
  }

  private static String enforceColonSeparator(String term) {
    return  term.replace("_", ":");
  }
}
