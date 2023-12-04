package life.qbic.projectmanagement.domain.model.sample;

import jakarta.persistence.Convert;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.repository.jpa.OntologyClassAttributeConverter;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;

/**
 * <b>Sample Origin</b>
 * <p>
 * The sample origin contains the species, specimen and analyte as {@link OntologyClassDTO} that
 * are associated with the {@link Sample}. They are a subset of the classes that have been selected
 * for the respective {@link Experiment}.
 * <p>
 *
 * @since 1.0.0
 */
public class SampleOrigin {

  @Convert(converter = OntologyClassAttributeConverter.class)
  private OntologyClassDTO species;
  @Convert(converter = OntologyClassAttributeConverter.class)
  private OntologyClassDTO specimen;
  @Convert(converter = OntologyClassAttributeConverter.class)
  private OntologyClassDTO analyte;

  protected SampleOrigin() {
    //needed for JPA
  }

  private SampleOrigin(OntologyClassDTO species, OntologyClassDTO specimen,
      OntologyClassDTO analyte) {
    this.species = Objects.requireNonNull(species);
    this.specimen = Objects.requireNonNull(specimen);
    this.analyte = Objects.requireNonNull(analyte);
  }

  public static SampleOrigin create(OntologyClassDTO species, OntologyClassDTO specimen,
      OntologyClassDTO analytes) {
    return new SampleOrigin(species, specimen, analytes);
  }

  public OntologyClassDTO getSpecies() {
    return species;
  }

  public OntologyClassDTO getSpecimen() {
    return specimen;
  }

  public OntologyClassDTO getAnalyte() {
    return analyte;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SampleOrigin sampleOrigin)) {
      return false;
    }

    return this.species.equals(sampleOrigin.species) && this.specimen.equals(sampleOrigin.specimen)
        && this.analyte.equals(sampleOrigin.analyte);
  }

  @Override
  public int hashCode() {
    return Objects.hash(species, specimen, analyte);
  }
}
