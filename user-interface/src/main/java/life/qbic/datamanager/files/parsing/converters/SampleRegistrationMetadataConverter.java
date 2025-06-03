package life.qbic.datamanager.files.parsing.converters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor.SampleInformationForNewSample;
import life.qbic.datamanager.files.parsing.Sanitizer;
import life.qbic.datamanager.files.structure.sample.RegisterColumn;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SampleRegistrationInformation;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SampleRegistrationMetadataConverter implements MetadataConverterV2<SampleRegistrationInformation> {

  @Override
  public SampleRegistrationInformation convert(ParsingResult parsingResult) {
    throw new RuntimeException("Not yet implemented");
    var result = new ArrayList<SampleRegistrationInformation>();

    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var sampleName = parsingResult.getValueOrDefault(i, RegisterColumn.SAMPLE_NAME.headerName(),
          "");
      var analysisMethod = parsingResult.getValueOrDefault(i, RegisterColumn.ANALYSIS.headerName(),
          "");
      var biologicalReplicate = parsingResult.getValueOrDefault(i,
          RegisterColumn.BIOLOGICAL_REPLICATE.headerName(), "");
      var condition = parsingResult.getValueOrDefault(i, RegisterColumn.CONDITION.headerName(), "");
      var species = parsingResult.getValueOrDefault(i, RegisterColumn.SPECIES.headerName(), "");
      var analyte = parsingResult.getValueOrDefault(i, RegisterColumn.ANALYTE.headerName(), "");
      var specimen = parsingResult.getValueOrDefault(i, RegisterColumn.SPECIMEN.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i, RegisterColumn.COMMENT.headerName(), "");

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

      result.add(new SampleRegistrationInformation(
         sampleName,
         biologicalReplicate,
         condition,
         species,
         specimen,
         analyte,
         analysisMethod,
         comment,
         confoundingVariables,
      ));

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
}
