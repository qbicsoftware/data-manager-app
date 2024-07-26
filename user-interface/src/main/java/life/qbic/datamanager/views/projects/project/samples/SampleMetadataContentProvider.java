package life.qbic.datamanager.views.projects.project.samples;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import life.qbic.datamanager.views.general.download.DownloadContentProvider;
import life.qbic.datamanager.views.general.download.TSVBuilder;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;

/**
 * Provides the downloadable (byte) content of a metadata sheet for all samples of an experiment.
 * Uses SamplePreview objects and an experiment object to provide basic information about experiment
 * and batch name as well.
 */
public class SampleMetadataContentProvider implements DownloadContentProvider {


  private Experiment experiment;
  private List<SamplePreview> samples;
  private static final String FILE_SUFFIX = "_samples.txt";

  @Override
  public byte[] getContent() {
    if(samples.isEmpty()) {
      return new byte[0];
    }
    TSVBuilder<SamplePreview> tsvBuilder = new TSVBuilder<>(samples);
    tsvBuilder.addColumn("Sample ID", SamplePreview::sampleCode);
    tsvBuilder.addColumn("Sample Name", SamplePreview::sampleLabel);
    tsvBuilder.addColumn("Organism ID", SamplePreview::organismId);
    tsvBuilder.addColumn("Batch", SamplePreview::batchLabel);
    tsvBuilder.addColumn("Species", sample -> sample.species().getLabel());
    tsvBuilder.addColumn("Specimen", sample -> sample.specimen().getLabel());
    tsvBuilder.addColumn("Analyte", sample -> sample.analyte().getLabel());
    tsvBuilder.addColumn("Analysis to Perform", SamplePreview::analysisMethod);
    tsvBuilder.addColumn("Comment", SamplePreview::comment);

    SamplePreview firstSample = samples.stream().findFirst().get();
    List<String> conditionHeader = createConditionHeader(firstSample.experimentalGroup().condition());

    for(int i = 0; i < conditionHeader.size(); i++) {
      String colName = conditionHeader.get(i);
      int finalI = i;
      tsvBuilder.addColumn(colName, sample -> sample.experimentalGroup().condition()
          .getVariableLevels().get(finalI).experimentalValue().value());
    }

    return tsvBuilder.getTSVString().getBytes(StandardCharsets.UTF_8);
  }

  private List<String> createConditionHeader(Condition condition) {
    List<String> headerWithUnits = new ArrayList<>();
    for(VariableLevel level : condition.getVariableLevels()) {
      String label = level.variableName().value();
      Optional<String> unit = level.experimentalValue().unit();
      if(unit.isPresent()) {
        label = String.format("%s [%s]", label, unit.get());
      }
      headerWithUnits.add(label);
    }
    return headerWithUnits;
  }

  @Override
  public String getFileName() {
    return fileNamePrefixFromExperimentName(experiment.getName()) + FILE_SUFFIX;
  }

  public void updateContext(Optional<Experiment> experiment, List<SamplePreview> samples) {
    this.experiment = experiment.orElseThrow();
    this.samples = samples;
    this.samples.sort(Comparator.comparing(SamplePreview::sampleCode));
  }

  private String fileNamePrefixFromExperimentName(String experimentName) {
    String prefix = experimentName.replace(" ","_");
    if(prefix.length() > 15) {
      return prefix.substring(0,16);
    }
    return prefix;
  }

}
