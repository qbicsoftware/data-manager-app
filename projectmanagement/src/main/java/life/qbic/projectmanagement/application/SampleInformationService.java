package life.qbic.projectmanagement.application;

import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * SampleInformationService
 * <p>
 * Service that provides an API to query sample information
 */
@Service
public class SampleInformationService {

  public SampleInformationService() {
  }

  //ToDo Load Sample Information based on experiment Id?
  public Collection<Sample> retrieveSamplesForExperiment(ExperimentId experimentId) {
    Objects.requireNonNull(experimentId);
    return generateSamples();
  }

  private Collection<Sample> generateSamples() {
    Collection<Sample> samples = new ArrayList<>();
    samples.add(
        Sample.create("QABCD4A5DG", "TÜ_2019_0161_1", "Batch 1", "Received", "First Experiment",
            "Patient1", "", "Colgate", "Homo Sapiens", "Mucous Membrane"));
    samples.add(
        Sample.create("QABCD466DG", "TÜ_2019_0161_2", "Batch 1", "Received", "First Experiment",
            "Patient1", "", "Colgate", "Homo Sapiens", "Mucous Membrane"));
    samples.add(
        Sample.create("QABCD4FJDG", "TÜ_2019_0162_1", "Batch 2", "Received", "First Experiment",
            "Patient2", "", "Colgate", "Homo Sapiens", "Mucous Membrane"));
    samples.add(
        Sample.create("QABCD4A5BB", "TÜ_2019_0162_2", "Batch 2", "Received", "First Experiment",
            "Patient2", "", "Colgate", "Homo Sapiens", "Mucous Membrane"));
    samples.add(
        Sample.create("QABCD89UAE", "TÜ_2019_0163_2", "Pilot 2", "QC Passed", "Pilot Experiment",
            "Patient3", "60 sec", "Colgate", "Homo Sapiens", "Mucous Membrane"));
    samples.add(Sample.create("QABCD589AA", "TÜ_2019_0164_1", "Pilot 1", "Data Available",
        "Pilot Experiment", "Patient4", "30 sec", "Colgate", "Homo Sapiens", "Mucous Membrane"));
    samples.add(
        Sample.create("QABCD389LG", "TÜ_2019_0164_1", "Pilot 2", "QC Passed", "Pilot Experiment",
            "Patient5", "60 sec", "Colgate", "Homo Sapiens", "Mucous Membrane"));
    samples.add(Sample.create("QABCDD45L6", "TÜ_2019_0165_1", "Pilot 1", "Data Available",
        "Pilot Experiment", "Patient5", "30 sec", "Colgate", "Homo Sapiens", "Mucous Membrane"));
    samples.add(Sample.create("QABCDDABL3", "TÜ_2019_0165_2", "Pilot 1", "QC Failed",
        "Pilot Experiment", "Patient6", "30 sec", "Colgate", "Homo Sapiens", "Mucous Membrane"));
    return samples;
  }

  public record Sample(String id, String label, String batch, String status, String experiment,
                       String source, String condition1, String condition2, String species,
                       String specimen) {

    public Sample {
      Objects.requireNonNull(id);
      Objects.requireNonNull(label);
      Objects.requireNonNull(batch);
      Objects.requireNonNull(status);
    }

    public static Sample create(String id, String label, String batch, String status,
                                String experiment, String source, String condition1, String condition2, String species,
                                String specimen) {
      return new Sample(id, label, batch, status, experiment, source, condition1, condition2,
          species, specimen);
    }

    public String id() {
      return this.id;
    }

    public String label() {
      return this.label;
    }

    public String batch() {
      return this.batch;
    }

    public String status() {
      return this.status;
    }

    public String experiment() {
      return this.experiment;
    }

    public String source() {
      return this.source;
    }

    public String condition1() {
      return this.condition1;
    }

    public String condition2() {
      return this.condition2;
    }

    public String species() {
      return this.species;
    }

    public String specimen() {
      return this.specimen;
    }
  }
}
