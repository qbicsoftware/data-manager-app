package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;

import org.apache.poi.ss.usermodel.Workbook;

public class IPWorkbooks {

  public static Workbook createRegistrationWorkbook() {
    return new IpRegisterFactory().createWorkbook();
  }
}
