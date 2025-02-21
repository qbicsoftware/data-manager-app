package life.qbic.datamanager.files.export.sample;

import java.util.List;
import life.qbic.datamanager.files.export.WorkbookFactory;
import life.qbic.datamanager.views.general.confounding.ConfoundingVariable;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableLevel;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.apache.poi.ss.usermodel.Workbook;

public class SampleWorkbooks {

  private SampleWorkbooks() {
  }

  public static Workbook createRegistrationWorkbook(List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes, List<String> species, List<String> specimen,
      List<ConfoundingVariable> confoundingVariables) {

    WorkbookFactory factory = new SampleRegisterFactory(analysisMethods,
        conditions, analytes, species, specimen, confoundingVariables);
    return factory.createWorkbook();
  }

  public static Workbook createEditWorkbook(List<Sample> samples, List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes, List<String> species, List<String> specimen,
      List<ExperimentalGroup> experimentalGroups, List<ConfoundingVariable> confoundingVariables,
      List<ConfoundingVariableLevel> confoundingVariableLevels) {

    WorkbookFactory factory = new SampleEditFactory(samples, analysisMethods,
        conditions, analytes, species, specimen, experimentalGroups, confoundingVariables,
        confoundingVariableLevels);
    return factory.createWorkbook();
  }

  public static Workbook createInformationWorkbook(List<Sample> samples,
      List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes, List<String> species, List<String> specimen,
      List<ExperimentalGroup> experimentalGroups,
      List<ConfoundingVariable> confoundingVariables,
      List<ConfoundingVariableLevel> confoundingVariableLevels) {
    WorkbookFactory factory = new SampleInformationFactory(samples, analysisMethods, conditions,
        analytes, species, specimen, experimentalGroups, confoundingVariables,
        confoundingVariableLevels);
    return factory.createWorkbook();
  }
}
