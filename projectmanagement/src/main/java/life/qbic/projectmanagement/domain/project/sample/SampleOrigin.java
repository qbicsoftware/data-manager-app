package life.qbic.projectmanagement.domain.project.sample;

import java.util.Objects;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

/**
 * <b>Sample Origin</b>
 * <p>
 * The sample origin contains the {@link Species}, {@link Specimen} and {@link Analyte} specified
 * for an {@link Experiment}. It's information is associated with the information stored within the
 * {@link Sample}
 * <p>
 *
 * @since 1.0.0
 */
public class SampleOrigin {

  private Species species;
  private Specimen specimen;
  private Analyte analyte;

  protected SampleOrigin() {
    //needed for JPA
  }

  private SampleOrigin(Species species, Specimen specimen, Analyte analyte) {
    this.species = Objects.requireNonNull(species);
    this.specimen = Objects.requireNonNull(specimen);
    this.analyte = Objects.requireNonNull(analyte);
  }

  public static SampleOrigin create(Species species, Specimen specimen, Analyte analytes) {
    return new SampleOrigin(species, specimen, analytes);
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
