package life.qbic.datamanager.files.export;

import org.apache.poi.ss.usermodel.Workbook;

public interface WorkbookFactory {

  Workbook createWorkbook();
}
