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
    String ontologyName = Ontology.findOntologyByAbbreviation(contentDTO.getOntology()).getName();
    styleLayout(contentDTO.getLabel(), contentDTO.getName(), ontologyName);
  }

  private void styleLayout(String label, String id, String ontology) {
    addClassName("ontology-component");

    var upperDiv = new Div();
    upperDiv.add(new Span(label+ " ("+id+")"));
    var lowerDiv = new Div();
    Span subtitle = new Span(ontology);
    subtitle.addClassNames("subtitle");
    lowerDiv.add(subtitle);
    upperDiv.add(lowerDiv);

    add(upperDiv);
  }
}
