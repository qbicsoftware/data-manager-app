package life.qbic.datamanager.parser;

import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.parser.sample.EditColumn;
import life.qbic.datamanager.parser.sample.RegisterColumn;

/**
 * Extracts sample information from a parsing result.
 * <p>
 * This class does not perform any validation and missing entries will be provided with empty
 * strings.
 *
 * @since 1.5.0
 */
public class SampleInformationExtractor {

  /**
   * Extract information for new samples from a parsing result.
   *
   * @param parsingResult the result of parsing user provided information.
   * @return a record of extracted sample information. Missing entries are provided as empty
   * strings.
   */
  public List<SampleInformationForNewSample> extractInformationForNewSamples(
      ParsingResult parsingResult) {
    var result = new ArrayList<SampleInformationForNewSample>();
    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var sampleName = parsingResult.getValueOrDefault(i, RegisterColumn.SAMPLE_NAME.headerName(),
          "");
      var condition = parsingResult.getValueOrDefault(i, RegisterColumn.CONDITION.headerName(), "");
      var species = parsingResult.getValueOrDefault(i, RegisterColumn.SPECIES.headerName(), "");
      var specimen = parsingResult.getValueOrDefault(i, RegisterColumn.SPECIMEN.headerName(), "");
      var analyte = parsingResult.getValueOrDefault(i, RegisterColumn.ANALYTE.headerName(), "");
      var analysisMethod = parsingResult.getValueOrDefault(i, RegisterColumn.ANALYSIS.headerName(),
          "");
      var comment = parsingResult.getValueOrDefault(i, RegisterColumn.COMMENT.headerName(), "");
      result.add(new SampleInformationForNewSample(
          sampleName,
          condition,
          species,
          specimen,
          analyte,
          analysisMethod,
          comment));
    }
    return result;
  }

  /**
   * Extract information for existing samples from a parsing result.
   *
   * @param parsingResult the result of parsing user provided information.
   * @return a record of extracted sample information. Missing entries are provided as empty
   * strings.
   */
  public List<SampleInformationForExistingSample> extractInformationForExistingSamples(
      ParsingResult parsingResult) {
    var result = new ArrayList<SampleInformationForExistingSample>();
    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var sampleCode = parsingResult.getValueOrDefault(i, EditColumn.SAMPLE_ID.headerName(), "");
      var condition = parsingResult.getValueOrDefault(i, EditColumn.CONDITION.headerName(), "");
      var species = parsingResult.getValueOrDefault(i, EditColumn.SPECIES.headerName(), "");
      var specimen = parsingResult.getValueOrDefault(i, EditColumn.SPECIMEN.headerName(), "");
      var analyte = parsingResult.getValueOrDefault(i, EditColumn.ANALYTE.headerName(), "");
      var analysisMethod = parsingResult.getValueOrDefault(i, EditColumn.ANALYSIS.headerName(),
          "");
      result.add(new SampleInformationForExistingSample(sampleCode,
          condition,
          species,
          specimen,
          analyte,
          analysisMethod));
    }
    return result;
  }

  /**
   * Information expected for registering new samples
   *
   * @param condition
   * @param species
   * @param specimen
   * @param analyte
   * @param analysisMethod
   */
  public record SampleInformationForNewSample(
      String sampleName,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment
  ) {

  }

  /**
   * Information expected for editing existing samples
   *
   * @param sampleCode
   * @param condition
   * @param species
   * @param specimen
   * @param analyte
   * @param analysisMethod
   */
  public record SampleInformationForExistingSample(
      String sampleCode,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod
  ) {

  }
}
