package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.general.OboIdRenderer;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class OntologyTermDisplay extends Div {

  private Span link;

  private Span label;

  private String curie;

  private OntologyTermDisplay() {};

  public OntologyTermDisplay(String label, String curie, String reference) {
    Div ontology = new Div();
    ontology.addClassName("vertical-list");
    Span ontologyLabel = new Span(label);
    ontologyLabel.addClassName("overflow-hidden-ellipsis");
    Span ontologyLink = new Span(OboIdRenderer.render(curie));
    ontologyLink.addClassName("ontology-link");
    Anchor ontologyClassIri = new Anchor(reference, ontologyLink);
    ontologyClassIri.setTarget(AnchorTarget.BLANK);
    ontology.add(ontologyLabel, ontologyClassIri);
    ontology.addClassNames("ontology-term", "gap-small");
    add(ontology);
  }

}
