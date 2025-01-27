package life.qbic.datamanager.files.export.sample;

import java.util.List;
import life.qbic.datamanager.files.export.WorkbookFactory;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.apache.poi.ss.usermodel.Workbook;

public class SampleWorkbooks {

  private SampleWorkbooks() {
  }

  public static Workbook createRegistrationWorkbook(List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes, List<String> species, List<String> specimen) {
    WorkbookFactory factory = new SampleRegisterFactory(analysisMethods,
        conditions, analytes, species, specimen);
    return factory.createWorkbook();
  }

  public static Workbook createEditWorkbook(List<Sample> samples, List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes, List<String> species, List<String> specimen,
      List<ExperimentalGroup> experimentalGroups) {

    WorkbookFactory factory = new SampleEditFactory(samples, analysisMethods,
        conditions, analytes, species, specimen, experimentalGroups);
    return factory.createWorkbook();
  }

  public static Workbook createInformationWorkbook(List<Sample> samples,
      List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes, List<String> species, List<String> specimen,
      List<ExperimentalGroup> experimentalGroups) {
    WorkbookFactory factory = new SampleInformationFactory(samples, analysisMethods, conditions,
        analytes, species, specimen, experimentalGroups);
    return factory.createWorkbook();
  }
}
