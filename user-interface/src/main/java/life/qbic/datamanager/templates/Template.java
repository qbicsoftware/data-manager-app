package life.qbic.datamanager.templates;

import life.qbic.datamanager.download.DownloadContentProvider;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public abstract class Template implements DownloadContentProvider {

  public String getDomainName() {
    return "Unknown Domain";
  }
}
