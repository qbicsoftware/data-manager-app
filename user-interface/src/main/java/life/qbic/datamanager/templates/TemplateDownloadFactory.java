package life.qbic.datamanager.templates;

import life.qbic.datamanager.download.DownloadContentProvider;
import life.qbic.datamanager.templates.measurement.NGSMeasurementTemplate;
import life.qbic.datamanager.templates.measurement.ProteomicsMeasurementTemplate;

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
          case MS_MEASUREMENT -> new ProteomicsMeasurementTemplate();
          case NGS_MEASUREMENT -> new NGSMeasurementTemplate();
        };
  }

  public enum TemplateType {
    NGS_MEASUREMENT, MS_MEASUREMENT
  }

}
