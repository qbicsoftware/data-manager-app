package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.apache.poi.ss.usermodel.Cell;

final class SpreadsheetMethods {

  private SpreadsheetMethods() {

  }
  private static final Logger log = LoggerFactory.logger(SpreadsheetMethods.class);

  /**
   * Returns a String representation of a Cell or null when the cell is null or its type was defined wrongly
   * @param cell the Cell that should be parsed
   * @return A String representation or null if the cell was not or wrongly defined
   */
  public static String cellToStringOrNull(Cell cell) {
    if(cell==null) {
      return null;
    }
    switch (cell.getCellType()) {
      case STRING -> {
        return cell.getStringCellValue();
      }
      case NUMERIC -> {
        double dbl = cell.getNumericCellValue();
        if((dbl % 1) == 0) {
          int integer = (int) dbl;
          return Integer.toString(integer);
        } else {
          return Double.toString(dbl);
        }
      }
      case FORMULA -> {
        return cell.getCellFormula();
      }
      case BLANK -> {
        return "";
      }
      case BOOLEAN -> {
        return Boolean.toString(cell.getBooleanCellValue());
      }
      case ERROR -> {
        log.debug("Cell is of type ERROR, returned CELL ERROR.");
        return "CELL ERROR";
      }
      default -> {
        log.debug("Cell with type "+cell.getCellType()+ " was not handled.");
        return null;
      }
    }
  }

}
