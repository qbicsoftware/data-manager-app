package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;

import java.util.List;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.WorkbookFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.NgsEditFactory.MeasurementEntryNGS;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.ProteomicsEditFactory.MeasurementEntryPxP;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class MeasurementTemplateFactory {

  public WorkbookFactory forUpdateNGS(List<MeasurementEntryNGS> measurements) {

    return new NgsEditFactory(measurements);
  }

  public WorkbookFactory forUpdatePxP(List<MeasurementEntryPxP> measurements) {

    return new ProteomicsEditFactory(measurements);  }

}
