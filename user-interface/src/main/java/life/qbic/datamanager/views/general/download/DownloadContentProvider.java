package life.qbic.datamanager.views.general.download;

/**
 * Provides content and file name for any files created from data and metadata.
 */
public interface DownloadContentProvider {

  public byte[] getContent();
  public String getFileName();
}
