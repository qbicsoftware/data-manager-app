package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Span;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;

@Tag(Tag.DIV)
public class OntologyComponent extends Component implements HasComponents {
  public OntologyComponent(OntologyClassDTO contentDTO) {
    Span ontologyLabel = new Span(contentDTO.getLabel());
    ontologyLabel.addClassName("bold");
    String ontologyLinkName = contentDTO.getName().replace("_", ":");
    Anchor ontologyLink = new Anchor(contentDTO.getClassIri(), ontologyLinkName);
    /*Clicking the Link should open the origin page in a new tab*/
    ontologyLink.setTarget(AnchorTarget.BLANK);
    ontologyLink.addClassName("ontology-link");
    addClassName("ontology-component");
    add(ontologyLabel, ontologyLink);
  }
}
