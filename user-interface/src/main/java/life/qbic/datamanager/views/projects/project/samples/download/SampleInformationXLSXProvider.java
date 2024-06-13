package life.qbic.datamanager.views.projects.project.samples.download;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.datamanager.views.general.download.DownloadContentProvider;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.model.experiment.VariableName;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * <b>Sample Information XLSX Provider</b>
 *
 * <p>Provides the download content as XLSX spreadsheet for the sample information</p>
 *
 * @since 1.1.0
 */
public class SampleInformationXLSXProvider implements DownloadContentProvider {

  private static final String FILE_NAME = "sample_information.xlsx";
  private static final Logger log = logger(SampleInformationXLSXProvider.class);
  private final List<SamplePreview> samples = new ArrayList<>();


  private static void setAutoWidth(Sheet sheet) {
    for (int col = 0; col <= SamplePreviewColumn.values().length; col++) {
      sheet.autoSizeColumn(col);
    }
  }

  private static void formatHeader(Row header, Map<String, Integer> variableNamesToColumn) {
    for (SamplePreviewColumn value : SamplePreviewColumn.values()) {
      var cell = header.createCell(value.column());
      cell.setCellValue(value.header());
    }
    for (Map.Entry<String, Integer> entry : variableNamesToColumn.entrySet()) {
      var cell = header.createCell(entry.getValue());
      cell.setCellValue(entry.getKey());
    }
  }

  private Set<String> uniqueExperimentalVariables(List<SamplePreview> samples) {
    HashSet<String> variableNames = new HashSet<>();
    for (SamplePreview sample : samples) {
      var variableLevels = sample.experimentalGroup().condition().getVariableLevels();
      variableNames.addAll(variableLevels
          .stream()
          .map(VariableLevel::variableName)
          .map(VariableName::value).toList());
    }
    return variableNames;
  }

  @Override
  public byte[] getContent() {
    if (samples.isEmpty()) {
      return new byte[0];
    }

    ByteArrayOutputStream byteArrayOutputStream;

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Sample Information");

      Row row = sheet.createRow(0);
      Map<String, Integer> variableNamesToColumn = new HashMap<>();
      Set<String> experimentalVariableNames = uniqueExperimentalVariables(samples);
      Iterator<String> variableNamesIterator = experimentalVariableNames.iterator();
      for (int assignedColumn = SamplePreviewColumn.values().length;
          assignedColumn < SamplePreviewColumn.values().length + experimentalVariableNames.size();
          assignedColumn++) {
        variableNamesToColumn.put(variableNamesIterator.next(), assignedColumn);
      }
      formatHeader(row, variableNamesToColumn);

      int rowNum = 1;

      for (SamplePreview sample : samples) {
        Row sampleRow = sheet.createRow(rowNum);
        createSampleInfoEntry(sample, sampleRow, variableNamesToColumn);
        rowNum++;
      }

      setAutoWidth(sheet);
      byteArrayOutputStream = new ByteArrayOutputStream();
      workbook.write(byteArrayOutputStream);


    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new ApplicationException(ErrorCode.GENERAL, null);
    }

    return byteArrayOutputStream.toByteArray();
  }

  private void createSampleInfoEntry(SamplePreview sample, Row sampleRow,
      Map<String, Integer> variableNamesToColumn) {
    var sampleIdCol = sampleRow.createCell(SamplePreviewColumn.SAMPLE_ID.column());
    sampleIdCol.setCellValue(sample.sampleCode());

    var labelCol = sampleRow.createCell(SamplePreviewColumn.LABEL.column());
    labelCol.setCellValue(sample.sampleLabel());

    var organismIdCol = sampleRow.createCell(SamplePreviewColumn.ORGANISM_ID.column());
    organismIdCol.setCellValue(sample.organismId());

    var batchCol = sampleRow.createCell(SamplePreviewColumn.BATCH.column());
    batchCol.setCellValue(sample.batchLabel());

    var speciesCol = sampleRow.createCell(SamplePreviewColumn.SPECIES.column());
    speciesCol.setCellValue(sample.species().getLabel());

    var specimenCol = sampleRow.createCell(SamplePreviewColumn.SPECIMEN.column());
    specimenCol.setCellValue(sample.specimen().getLabel());

    var analyteCol = sampleRow.createCell(SamplePreviewColumn.ANALYTE.column());
    analyteCol.setCellValue(sample.analyte().getLabel());

    var analysisCol = sampleRow.createCell(SamplePreviewColumn.ANALYSIS.column());
    analysisCol.setCellValue(sample.analysisMethod());

    var commentCol = sampleRow.createCell(SamplePreviewColumn.COMMENT.column());
    commentCol.setCellValue(sample.comment());

    for (VariableLevel variableLevel : sample.experimentalGroup().condition().getVariableLevels()) {
      var conditionCol = sampleRow.createCell(
          variableNamesToColumn.get(variableLevel.variableName().value()));
      conditionCol.setCellValue(variableLevel.experimentalValue().value());
    }
  }

  public void setSamples(List<SamplePreview> samplePreviews) {
    this.samples.clear();
    this.samples.addAll(samplePreviews);
    this.samples.sort(Comparator.comparing(SamplePreview::sampleCode));
  }

  @Override
  public String getFileName() {
    return FILE_NAME;
  }

  enum SamplePreviewColumn {

    SAMPLE_ID("Sample ID", 0),
    LABEL("Label", 1),
    ORGANISM_ID("Organism ID", 2),
    BATCH("Batch", 3),
    SPECIES("Species", 4),
    SPECIMEN("Specimen", 5),
    ANALYTE("Analyte", 6),
    ANALYSIS("Analysis", 7),
    COMMENT("Comment", 8);

    private final String headerName;

    private final int column;

    SamplePreviewColumn(String headerName, int column) {
      this.headerName = headerName;
      this.column = column;
    }

    public String header() {
      return headerName;
    }

    public int column() {
      return column;
    }

  }
}
