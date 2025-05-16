package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Span;
import life.qbic.projectmanagement.domain.model.OntologyTermV1;

@Tag(Tag.DIV)
public class OntologyComponent extends Component implements HasComponents {

  public OntologyComponent(OntologyTermV1 contentDTO) {
    Span ontologyLabel = new Span(contentDTO.getLabel());
    ontologyLabel.addClassName("bold");
    /* Ontology terms are delimited by a column, the underscore is only used in the web environment*/
    String ontologyNameContent = contentDTO.getOboId().replace("_", ":");
    Span ontologyName = new Span(ontologyNameContent);
    /*Clicking the Link should open the origin page in a new tab*/
    ontologyName.addClassName("ontology-name");
    addClassName("ontology-component");
    add(ontologyLabel, ontologyName);
  }
}
