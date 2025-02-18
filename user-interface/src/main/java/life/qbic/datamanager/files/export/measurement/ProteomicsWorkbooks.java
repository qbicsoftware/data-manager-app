package life.qbic.datamanager.files.export.measurement;

import java.util.Arrays;
import java.util.List;
import life.qbic.datamanager.views.projects.project.measurements.ProteomicsMeasurementEntry;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementProteomicsValidator;
import org.apache.poi.ss.usermodel.Workbook;

public class ProteomicsWorkbooks {

  /**
   * Describes Digestion Methods for Samples to be Measured by Proteomics
   */
  public enum DigestionMethod {
    IN_GEL("in gel"),
    IN_SOLUTION("in solution"),
    IST_PROTEOMICS_KIT("iST proteomics kit"),
    ON_BEADS("on beads");

    private final String name;

    DigestionMethod(String name) {
      this.name = name;
    }

    public static boolean isDigestionMethod(String input) {
      return Arrays.stream(MeasurementProteomicsValidator.DigestionMethod.values()).anyMatch(o ->
          o.getName().equalsIgnoreCase(input));
    }

    public String getName() {
      return name;
    }

    public static List<String> getOptions() {
      return Arrays.stream(values()).map(DigestionMethod::getName).toList();
    }
  }

  public static Workbook createRegistrationWorkbook() {
    return new ProteomicsRegisterFactory().createWorkbook();
  }

  public static Workbook createEditWorkbook(List<ProteomicsMeasurementEntry> measurements) {
    return new ProteomicsEditFactory(measurements).createWorkbook();
  }
}
