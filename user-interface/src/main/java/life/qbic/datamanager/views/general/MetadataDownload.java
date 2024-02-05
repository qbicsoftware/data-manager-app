package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;

/**
 * The MetadataDownload class extends the Anchor class and provides functionality for triggering the
 * download of a sample metadata file.
 */
public class MetadataDownload extends Anchor {

  private final String FILE_SUFFIX = "_samples.txt";
  public MetadataDownload() {
    super("_blank", "Download");
    /*
     * Using setVisisble(false), vaadin prevents any client side actions.
     * This prevents us from using JavaScript to click the link, which is the only option
     * for using anchors now.
     * Thus, we prevent the display of the link with `display: none`.
     * The link is still on the page but invisible.
     */
    getStyle().set("display", "none");

    setTarget("_blank");
    getElement().setAttribute("download", true);
  }

  public void trigger(Optional<Experiment> experiment, List<SamplePreview> samples) {
    if(samples.size() > 0) {
      UI ui = getUI().orElseThrow(() -> new ApplicationException(
          "Metadata Download component triggered but not attached to any UI."));
      samples.sort(Comparator.comparing(SamplePreview::sampleCode));
      StreamResource resource = new StreamResource(
          fileNamePrefixFromExperimentName(experiment.orElseThrow().getName()) + FILE_SUFFIX,
          () -> new ByteArrayInputStream(createContent(samples)));
      this.setHref(resource);
      ui.getPage().executeJs("$0.click()", this.getElement());
    }
  }

  private byte[] createContent(List<SamplePreview> samples) {
    StringBuilder tsv = new StringBuilder();
    List<String> header = new ArrayList<>(Arrays.asList("Sample ID",
        "Label",
        "Organism ID",
        "Batch",
        "Biological Replicate"));
    SamplePreview firstSample = samples.stream().findFirst().get();
    List<String> conditionHeader = createConditionHeader(firstSample.experimentalGroup().condition());
    header.addAll(conditionHeader);
    header.addAll(Arrays.asList("Species",
        "Specimen",
        "Analyte",
        "Analysis to Perform",
        "Comment"));
    tsv.append(String.join("\t", header)).append("\n");
    for(SamplePreview sample : samples) {
      String sampleId = sample.sampleCode();
      String sampleLabel = sample.sampleLabel();
      String organismId = sample.organismId();
      String batchName = sample.batchLabel();
      String bioReplicate = sample.replicateLabel();
      String species = sample.species().getLabel();
      String specimen = sample.specimen().getLabel();
      String analyte = sample.analyte().getLabel();
      String analysisToPerform = sample.analysisMethod();
      String comment = sample.comment();
      List<String> data = new ArrayList<>(Arrays.asList(sampleId, sampleLabel, organismId, batchName, bioReplicate));
      data.addAll(createConditionContent(sample.experimentalGroup().condition()));
      data.addAll(Arrays.asList(species, specimen, analyte, analysisToPerform, comment));
      tsv.append(String.join("\t", data)).append("\n");
    }
    return tsv.toString().getBytes(StandardCharsets.UTF_8);
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

  private List<String> createConditionContent(Condition condition) {
    return condition.getVariableLevels().stream().map(c -> c.experimentalValue().value()).toList();
  }

  private String fileNamePrefixFromExperimentName(String experimentName) {
    String prefix = experimentName.replaceAll(" ","_");
    if(prefix.length() > 15) {
      return prefix.substring(0,16);
    }
    return prefix;
  }
}
