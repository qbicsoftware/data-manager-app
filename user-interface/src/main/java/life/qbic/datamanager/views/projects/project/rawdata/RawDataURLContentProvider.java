package life.qbic.datamanager.views.projects.project.rawdata;

import java.nio.charset.StandardCharsets;
import java.util.List;
import life.qbic.datamanager.views.general.download.DownloadContentProvider;
import life.qbic.datamanager.views.general.download.TextFileBuilder;
import life.qbic.datamanager.views.projects.project.rawdata.RawDataMain.RawDataURL;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;

/**
 * Provides the downloadable (byte) content of a textfile containing the download urls for the selected raw data items.
 */
public class RawDataURLContentProvider implements DownloadContentProvider {

  private Experiment experiment;
  private List<RawDataURL> rawDataUrls;
  private static final String FILE_SUFFIX = "_rawdata_urls.txt";

  @Override
  public byte[] getContent() {
    if (rawDataUrls.isEmpty()) {
      return new byte[0];
    }
    TextFileBuilder<RawDataURL> textFileBuilder = new TextFileBuilder<>(rawDataUrls,
        RawDataURL::toString);
    return textFileBuilder.getRowString().getBytes(StandardCharsets.UTF_8);
  }

  public void updateContext(Experiment experiment, List<RawDataURL> rawDataUrls) {
    this.experiment = experiment;
    this.rawDataUrls = rawDataUrls;
  }

  @Override
  public String getFileName() {
    return fileNamePrefixFromExperimentName(experiment.getName()) + FILE_SUFFIX;
  }

  private String fileNamePrefixFromExperimentName(String experimentName) {
    String prefix = experimentName.replace(" ", "_");
    if (prefix.length() > 15) {
      return prefix.substring(0, 16);
    }
    return prefix;
  }
}