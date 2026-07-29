package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;

import java.util.List;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.WorkbookFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.IpEditFactory.MeasurementEntryIP;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.NgsEditFactory.MeasurementEntryNGS;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.ProteomicsEditFactory.MeasurementEntryPxP;

/**
 * <b>Measurement Template Factory</b>
 * <p>
 * Creates @{@link WorkbookFactory} instances for different metadata templates.
 *
 * @since 1.11.0
 */
public class MeasurementTemplateFactory {

  /**
   * @param measurements expected to be pre-sorted by ascending measurement code ascending
   */
  public WorkbookFactory forUpdateNGS(List<MeasurementEntryNGS> measurements) {

    return new NgsEditFactory(measurements);
  }

  /** @param measurements expected to be pre-sorted by ascending measurement code ascending */
  public WorkbookFactory forUpdatePxP(List<MeasurementEntryPxP> measurements) {

    return new ProteomicsEditFactory(measurements);
  }

  /** @param measurements expected to be pre-sorted by ascending measurement code ascending */
  public WorkbookFactory forUpdateIP(List<MeasurementEntryIP> measurements) {

    return new IpEditFactory(measurements);
  }

}
