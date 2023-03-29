package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.stereotype.Service;

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
    samples.add(Sample.create("QABCD4A5DG", "TÜ_2019_0161_1", "Batch 1", "Received"));
    samples.add(Sample.create("QABCD466DG", "TÜ_2019_0161_2", "Batch 1", "Received"));
    samples.add(Sample.create("QABCD4FJDG", "TÜ_2019_0162_1", "Batch 2", "QC Passed"));
    samples.add(Sample.create("QABCD4A5BB", "TÜ_2019_0162_2", "Batch 2", "Data Available"));
    return samples;
  }

  public record Sample(String id, String label, String batch, String status) {

    public Sample {
      Objects.requireNonNull(id);
      Objects.requireNonNull(label);
      Objects.requireNonNull(batch);
      Objects.requireNonNull(status);
    }

    public static Sample create(String id, String label, String batch, String status) {
      return new Sample(id, label, batch, status);
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
  }


}
