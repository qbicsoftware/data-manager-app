package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;

import java.util.List;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.WorkbookFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.ngs.NgsEditFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.ngs.NgsEditFactory.MeasurementEntryNGS;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.proteomics.ProteomicsEditFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.proteomics.ProteomicsEditFactory.MeasurementEntryPxP;

/**
 * <b>Measurement Template Factory</b>
 * <p>
 * Creates @{@link WorkbookFactory} instances for different metadata templates.
 *
 * @since 1.11.0
 */
public class MeasurementTemplateFactory {

  public WorkbookFactory forUpdateNGS(List<MeasurementEntryNGS> measurements) {

    return new NgsEditFactory(measurements);
  }

  public WorkbookFactory forUpdatePxP(List<MeasurementEntryPxP> measurements) {

    return new ProteomicsEditFactory(measurements);
  }

}
