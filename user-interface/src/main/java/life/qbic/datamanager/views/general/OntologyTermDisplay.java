package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * <b>Ontology Term Display</b>
 * <p>
 * Renders an ontology term nicely as a badge with href.
 *
 * @since 1.6.0
 */
public class OntologyTermDisplay extends Div {


  public OntologyTermDisplay(String label, String curie, String reference) {
    Div ontology = new Div();
    ontology.addClassName("vertical-list");
    Span ontologyLabel = new Span(label);
    ontologyLabel.addClassName("overflow-hidden-ellipsis");
    Span ontologyLink = new Span(OboIdFormatter.render(curie));
    ontologyLink.addClassName("ontology-link");
    Anchor ontologyClassIri = new Anchor(reference, ontologyLink);
    ontologyClassIri.setTarget(AnchorTarget.BLANK);
    ontology.add(ontologyLabel, ontologyClassIri);
    ontology.addClassNames("ontology-term", "gap-small");
    add(ontology);
  }

}
