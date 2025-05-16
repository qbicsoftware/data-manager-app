package life.qbic.projectmanagement.domain.model;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;

public record OntologyTermV2(
    @JsonProperty("label") String label,
    @JsonProperty("description") String description,
    @JsonSerialize(using = CurieSerializer.class) @JsonDeserialize(using = CurieDeserializer.class) @JsonProperty("oboId") Curie oboId,
    @JsonProperty("iri") URI id,
    @JsonProperty("ontology_name") String ontologyName) implements Serializable {


  record Curie(@JsonProperty("prefix") String prefix,
               @JsonProperty("local_identifier") String localId) implements Serializable {

    public static Curie parse(String value) {
      requireNonNull(value);
      if (value.contains(":")) {
        var parts = value.split(":");
        String prefix = parts[0];
        String localId = parts[1];
        return new Curie(prefix, localId);
      }
      throw new IllegalArgumentException("Invalid Curie: " + value);
    }

    @Override
    public String toString() {
      return prefix + ":" + localId;
    }
  }

  public OntologyTermV2 fromOntologyTermV1(OntologyTermV1 oldTerm) {
    return new OntologyTermV2(
        oldTerm.getLabel(),
        oldTerm.getDescription(),
        new Curie(oldTerm.oboId().idSpace(), oldTerm.oboId().localId()),
        URI.create(oldTerm.getClassIri()),
        oldTerm.ontologyIdentifyingName()
    );
  }

  static class CurieSerializer extends JsonSerializer<Curie> {

    @Override
    public void serialize(Curie value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeObject(value.toString());
    }
  }

  static class CurieDeserializer extends JsonDeserializer<Curie> {

    @Override
    public Curie deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException {
      return Curie.parse(p.getValueAsString());
    }
  }

}
