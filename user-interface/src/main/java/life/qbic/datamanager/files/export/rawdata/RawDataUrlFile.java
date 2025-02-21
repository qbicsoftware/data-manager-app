package life.qbic.datamanager.files.export.rawdata;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.projects.project.rawdata.RawDataMain.RawDataURL;

public class RawDataUrlFile {

  private final List<RawDataURL> rawDataURLs;

  RawDataUrlFile(List<RawDataURL> rawDataURLs) {
    this.rawDataURLs = new ArrayList<>(rawDataURLs);
  }

  public static RawDataUrlFile create(List<RawDataURL> rawDataUrls) {
    return new RawDataUrlFile(rawDataUrls);
  }

  public byte[] getBytes(Charset charset) {
    StringBuilder stringBuilder = new StringBuilder();
    for (RawDataURL rawDataURL : rawDataURLs) {
      stringBuilder.append(rawDataURL.toString() + "\n");
    }
    return stringBuilder.toString().getBytes(charset);
  }
}
