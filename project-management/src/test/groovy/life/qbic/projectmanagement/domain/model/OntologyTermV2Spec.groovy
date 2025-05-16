package life.qbic.projectmanagement.domain.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import spock.lang.Specification

class OntologyTermV2Spec extends Specification {
    private final OntologyTermV2 exampleOntologyTerm = new OntologyTermV2("colorectal adenocarcinoma cell", "Adenocarcinoma cell related to the colon and/or rectum.", OntologyTermV2.Curie.parse("BTO:0000035"), URI.create("http://purl.obolibrary.org/obo/BTO_0000035"), "bto")
    private final String exampleSerializedOntologyTerm = """\
        {
          "label" : "colorectal adenocarcinoma cell",
          "description" : "Adenocarcinoma cell related to the colon and/or rectum.",
          "oboId" : "BTO:0000035",
          "iri" : "http://purl.obolibrary.org/obo/BTO_0000035",
          "ontology_name" : "bto"
        }\
        """.stripIndent()
    private static final ObjectMapper objectMapper = new ObjectMapper()

    void setup() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    def "serialization produces expected string"() {
        expect:
        objectMapper.writeValueAsString(exampleOntologyTerm) == exampleSerializedOntologyTerm
    }

    def "deserialization works as expected"() {
        expect:
        new ObjectMapper().readValue(exampleSerializedOntologyTerm, OntologyTermV2.class) == exampleOntologyTerm
    }
}
