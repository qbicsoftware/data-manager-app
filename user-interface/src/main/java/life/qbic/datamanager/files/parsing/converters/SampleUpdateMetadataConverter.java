package life.qbic.datamanager.files.parsing.converters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.Sanitizer;
import life.qbic.datamanager.files.structure.sample.EditColumn;
import life.qbic.datamanager.files.structure.sample.RegisterColumn;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SampleUpdateInformation;

/**
 * Sample Update Metadata Converter
 * <p>
 * Converter for converting a {@link SampleUpdateInformation} from a {@link ParsingResult}.
 *
 * @since 1.10.0
 */
public class SampleUpdateMetadataConverter implements MetadataConverterV2<SampleUpdateInformation> {

  @Override
  public List<SampleUpdateInformation> convert(ParsingResult parsingResult) {
    var convertedElements = new ArrayList<SampleUpdateInformation>();
    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var sampleCode = parsingResult.getValueOrDefault(i, EditColumn.SAMPLE_ID.headerName(), "");
      var sampleName = parsingResult.getValueOrDefault(i, EditColumn.SAMPLE_NAME.headerName(), "");
      var analysisMethod = parsingResult.getValueOrDefault(i, EditColumn.ANALYSIS.headerName(),
          "");
      var biologicalReplicate = parsingResult.getValueOrDefault(i,
          RegisterColumn.BIOLOGICAL_REPLICATE.headerName(), "");
      var condition = parsingResult.getValueOrDefault(i, EditColumn.CONDITION.headerName(), "");
      var species = parsingResult.getValueOrDefault(i, EditColumn.SPECIES.headerName(), "");
      var analyte = parsingResult.getValueOrDefault(i, EditColumn.ANALYTE.headerName(), "");
      var specimen = parsingResult.getValueOrDefault(i, EditColumn.SPECIMEN.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i, EditColumn.COMMENT.headerName(), "");

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

      convertedElements.add(new SampleUpdateInformation(
          sampleCode,
          sampleName,
          biologicalReplicate,
          condition,
          species,
          specimen,
          analyte,
          analysisMethod,
          comment,
          confoundingVariables
      ));
    }
    return convertedElements;
  }
}
