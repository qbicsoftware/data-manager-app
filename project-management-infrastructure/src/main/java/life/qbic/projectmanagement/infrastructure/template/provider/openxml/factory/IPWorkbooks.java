package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;

import java.util.List;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.IpEditFactory.MeasurementEntryIP;
import org.apache.poi.ss.usermodel.Workbook;

public class IPWorkbooks {

  public static Workbook createRegistrationWorkbook() {
    return new IpRegisterFactory().createWorkbook();
  }

  public static Workbook createEditWorkbook(List<MeasurementEntryIP> measurements) {
    return new IpEditFactory(measurements).createWorkbook();
  }
}
