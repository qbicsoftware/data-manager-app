package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;

@Tag(Tag.DIV)
public class OntologyComponent extends Component implements HasComponents {
  public OntologyComponent(OntologyClassDTO contentDTO) {
    String ontologyName = Ontology.findOntologyByAbbreviation(contentDTO.getOntologyAbbreviation())
        .getName();

    addClassName("ontology-component");

    var upperDiv = new Div();
    // creates a line with label and ontology name (id), e.g. "Homo sapiens (NCBITaxon_9606)"
    upperDiv.add(new Span(contentDTO.getLabel() + " (" + contentDTO.getName() + ")"));
    var lowerDiv = new Div();
    Span subtitle = new Span(ontologyName);
    subtitle.addClassNames("subtitle");
    lowerDiv.add(subtitle);
    upperDiv.add(lowerDiv);

    add(upperDiv);
  }
}
