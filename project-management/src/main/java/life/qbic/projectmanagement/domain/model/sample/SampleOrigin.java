package life.qbic.projectmanagement.domain.model.sample;

import java.util.Objects;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b>Sample Origin</b>
 * <p>
 * The sample origin contains the species, specimen and analyte as {@link OntologyTerm} that
 * are associated with the {@link Sample}. They are a subset of the classes that have been selected
 * for the respective {@link Experiment}.
 * <p>
 *
 * @since 1.0.0
 */
public class SampleOrigin {

  private OntologyTerm species;
  private OntologyTerm specimen;
  private OntologyTerm analyte;

  protected SampleOrigin() {
    //needed for JPA
  }

  private SampleOrigin(OntologyTerm species, OntologyTerm specimen,
      OntologyTerm analyte) {
    this.species = Objects.requireNonNull(species);
    this.specimen = Objects.requireNonNull(specimen);
    this.analyte = Objects.requireNonNull(analyte);
  }

  public static SampleOrigin create(OntologyTerm species, OntologyTerm specimen,
      OntologyTerm analytes) {
    return new SampleOrigin(species, specimen, analytes);
  }

  public OntologyTerm getSpecies() {
    return species;
  }

  public OntologyTerm getSpecimen() {
    return specimen;
  }

  public OntologyTerm getAnalyte() {
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
