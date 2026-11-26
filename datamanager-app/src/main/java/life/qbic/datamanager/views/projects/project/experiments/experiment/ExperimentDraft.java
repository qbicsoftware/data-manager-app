package life.qbic.datamanager.views.projects.project.experiments.experiment;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * Mutable value object bound to {@link ExperimentDesignForm}: the experiment name and the sample
 * origins (species, specimen, analyte) it scopes.
 *
 * <p>Extracted from the former {@code AddExperimentDialog}/{@code EditExperimentDialog} nested drafts
 * so the create and design routes share one value type (project owner, 2026-10-19: editing is inline,
 * not in dialogs).</p>
 *
 * @since 1.19.0
 */
public class ExperimentDraft implements Serializable {

  @Serial
  private static final long serialVersionUID = 5584396740927480418L;

  private final List<OntologyTerm> species = new ArrayList<>();
  private final List<OntologyTerm> specimen = new ArrayList<>();
  private final List<OntologyTerm> analytes = new ArrayList<>();
  private String experimentName;

  public String getExperimentName() {
    return experimentName;
  }

  public void setExperimentName(String experimentName) {
    this.experimentName = experimentName;
  }

  public List<OntologyTerm> getSpecies() {
    return new ArrayList<>(species);
  }

  public void setSpecies(Collection<OntologyTerm> species) {
    this.species.clear();
    this.species.addAll(species);
  }

  public List<OntologyTerm> getSpecimens() {
    return new ArrayList<>(specimen);
  }

  public void setSpecimens(Collection<OntologyTerm> specimen) {
    this.specimen.clear();
    this.specimen.addAll(specimen);
  }

  public List<OntologyTerm> getAnalytes() {
    return new ArrayList<>(analytes);
  }

  public void setAnalytes(Collection<OntologyTerm> analytes) {
    this.analytes.clear();
    this.analytes.addAll(analytes);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    ExperimentDraft that = (ExperimentDraft) object;
    return Objects.equals(experimentName, that.experimentName)
        && species.equals(that.species)
        && specimen.equals(that.specimen)
        && analytes.equals(that.analytes);
  }

  @Override
  public int hashCode() {
    int result = experimentName != null ? experimentName.hashCode() : 0;
    result = 31 * result + species.hashCode();
    result = 31 * result + specimen.hashCode();
    result = 31 * result + analytes.hashCode();
    return result;
  }
}
