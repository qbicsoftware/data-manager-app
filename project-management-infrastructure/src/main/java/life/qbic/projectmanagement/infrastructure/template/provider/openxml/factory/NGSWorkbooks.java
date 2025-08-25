package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;


import java.util.Arrays;
import java.util.List;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.NgsEditFactory.MeasurementEntryNGS;
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

  public static Workbook createEditWorkbook(List<MeasurementEntryNGS> measurements) {
    return new NgsEditFactory(measurements).createWorkbook();
  }

}
