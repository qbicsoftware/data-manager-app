package life.qbic.datamanager.importing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import life.qbic.datamanager.files.structure.sample.EditColumn;
import life.qbic.datamanager.files.structure.sample.RegisterColumn;
import life.qbic.datamanager.importing.parser.ParsingResult;
import life.qbic.datamanager.parser.Sanitizer;

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
      var sampleName = parsingResult.getValueOrDefault(i, RegisterColumn.SAMPLE_NAME.getName(),
          "");
      var analysisMethod = parsingResult.getValueOrDefault(i, RegisterColumn.ANALYSIS.getName(),
          "");
      var biologicalReplicate = parsingResult.getValueOrDefault(i,
          RegisterColumn.BIOLOGICAL_REPLICATE.getName(), "");
      var condition = parsingResult.getValueOrDefault(i, RegisterColumn.CONDITION.getName(), "");
      var species = parsingResult.getValueOrDefault(i, RegisterColumn.SPECIES.getName(), "");
      var analyte = parsingResult.getValueOrDefault(i, RegisterColumn.ANALYTE.getName(), "");
      var specimen = parsingResult.getValueOrDefault(i, RegisterColumn.SPECIMEN.getName(), "");
      var comment = parsingResult.getValueOrDefault(i, RegisterColumn.COMMENT.getName(), "");

      var sanitizedHeaderNames = RegisterColumn.headerNames().stream()
          .map(Sanitizer::headerEncoder)
          .collect(
              Collectors.toSet());

      var confoundingVariableColumns = new HashMap<String, Integer>();
      parsingResult.columnMap().forEach((key, value) -> {
        if (!sanitizedHeaderNames.contains(key)) {
          confoundingVariableColumns.put(key, value);
        }
      });
      var confoundingVariables = new HashMap<String, String>();
      final int finalI = i;
      confoundingVariableColumns.forEach((key, value) -> parsingResult.getValue(finalI, key)
          .ifPresent(it -> confoundingVariables.put(key, it)));

      result.add(new SampleInformationForNewSample(
          sampleName,
          analysisMethod,
          biologicalReplicate,
          condition,
          species,
          analyte,
          specimen,
          comment,
          confoundingVariables
      ));
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
      var sampleCode = parsingResult.getValueOrDefault(i, EditColumn.SAMPLE_ID.getName(), "");
      var sampleName = parsingResult.getValueOrDefault(i, EditColumn.SAMPLE_NAME.getName(), "");
      var analysisMethod = parsingResult.getValueOrDefault(i, EditColumn.ANALYSIS.getName(),
          "");
      var biologicalReplicate = parsingResult.getValueOrDefault(i,
          RegisterColumn.BIOLOGICAL_REPLICATE.getName(), "");
      var condition = parsingResult.getValueOrDefault(i, EditColumn.CONDITION.getName(), "");
      var species = parsingResult.getValueOrDefault(i, EditColumn.SPECIES.getName(), "");
      var analyte = parsingResult.getValueOrDefault(i, EditColumn.ANALYTE.getName(), "");
      var specimen = parsingResult.getValueOrDefault(i, EditColumn.SPECIMEN.getName(), "");
      var comment = parsingResult.getValueOrDefault(i, EditColumn.COMMENT.getName(), "");


      var sanitizedHeaderNames = EditColumn.headerNames().stream()
          .map(Sanitizer::headerEncoder)
          .collect(
              Collectors.toSet());

      var confoundingVariableColumns = new HashMap<String, Integer>();
      parsingResult.columnMap().forEach((key, value) -> {
        if (!sanitizedHeaderNames.contains(key)) {
          confoundingVariableColumns.put(key, value);
        }
      });
      var confoundingVariables = new HashMap<String, String>();
      final int finalI = i;
      confoundingVariableColumns.forEach((key, value) -> parsingResult.getValue(finalI, key)
          .ifPresent(it -> confoundingVariables.put(key, it)));

      result.add(new SampleInformationForExistingSample(sampleCode,
          sampleName,
          analysisMethod,
          biologicalReplicate,
          condition,
          species,
          specimen,
          analyte,
          comment,
          confoundingVariables
      ));
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
      String analysisMethod,
      String biologicalReplicate,
      String condition,
      String species,
      String analyte,
      String specimen,
      String comment,
      Map<String, String> confoundingVariables
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
      String sampleName,
      String analysisMethod,
      String biologicalReplicate,
      String condition,
      String species,
      String specimen,
      String analyte,
      String comment,
      Map<String, String> confoundingVariables) {

  }
}
