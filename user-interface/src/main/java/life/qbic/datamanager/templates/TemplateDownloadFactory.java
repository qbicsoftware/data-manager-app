package life.qbic.datamanager.templates;

import life.qbic.datamanager.views.general.download.DownloadContentProvider;

/**
 * <b>Template Download Factory</b>
 *
 * <p>Factory that allows for the generation of different {@link DownloadContentProvider}, for
 * example for measurement template spreadsheets.</p>
 *
 * @since 1.0.0
 */
public class TemplateDownloadFactory {

  public static Template provider(TemplateType templateType) {
    return
        switch (templateType) {
          case MS_MEASUREMENT -> new MSMeasurementTemplate();
          case NGS_MEASUREMENT -> new NGSMeasurementTemplate();
        };
  }

  public enum TemplateType {
    NGS_MEASUREMENT, MS_MEASUREMENT
  }

}
