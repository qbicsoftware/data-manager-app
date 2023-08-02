package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import java.awt.Color;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ExtendedColor;
import org.apache.poi.xssf.usermodel.XSSFColor;

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

  /**
   * Converts a color with alpha channel information to a color object usable in the Spreadsheet.
   * Assumes white background.
   * @param foreground - the foreground color as defined in Java awt
   * @param alpha - the transparency as a double alpha value between 0 and 1
   * @return an ExtendedColor object to be used in a Spreadsheet
   */
  public static ExtendedColor convertRGBToSpreadsheetColor(Color foreground, double alpha) {
    return SpreadsheetMethods.convertRBGToSpreadsheetColor(foreground, alpha, Color.white);
  }

  /**
   * Converts a color with alpha channel information to a color object usable in the Spreadsheet.
   * The result is influenced by the background color
   * @param foreground - the foreground color as defined in Java awt
   * @param alpha - the transparency as a double alpha value between 0 and 1
   * @param background - the background color as defined in Java awt
   * @return an ExtendedColor object to be used in a Spreadsheet
   */
  public static ExtendedColor convertRBGToSpreadsheetColor(Color foreground, double alpha, Color background) {
    double red = ((1 - alpha) * background.getRed()) + (alpha * foreground.getRed());
    double green = ((1 - alpha) * background.getGreen()) + (alpha * foreground.getGreen());
    double blue = ((1 - alpha) * background.getBlue()) + (alpha * foreground.getBlue());

    Color awtColor = new Color((int) red, (int) green, (int) blue);

    return new XSSFColor(awtColor, null);
  }

}
