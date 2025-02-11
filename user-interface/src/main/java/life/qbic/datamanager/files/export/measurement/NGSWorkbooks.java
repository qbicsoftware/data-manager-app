package life.qbic.datamanager.files.export.measurement;


import java.util.Arrays;
import java.util.List;
import life.qbic.datamanager.views.projects.project.measurements.NGSMeasurementEntry;
import org.apache.poi.ss.usermodel.Workbook;

public class NGSWorkbooks {

  enum SequencingReadType {
    SINGLE_END("single-end"),
    PAIRED_END("paired-end");
    private final String presentationString;

    SequencingReadType(String presentationString) {
      this.presentationString = presentationString;
    }

    static List<String> getOptions() {
      return Arrays.stream(values()).map(it -> it.presentationString).toList();
    }
  }

  public static Workbook createRegistrationWorkbook() {
    return new NgsRegisterFactory().createWorkbook();
  }

  public static Workbook createEditWorkbook(List<NGSMeasurementEntry> measurements) {
    return new NgsEditFactory(measurements).createWorkbook();
  }

}
