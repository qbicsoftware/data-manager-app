package life.qbic.projectmanagement.infrastructure.template.provider.openxml;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.api.template.TemplateProvider;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.MeasurementTemplateFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.NgsEditFactory.MeasurementEntryNGS;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.ProteomicsEditFactory.MeasurementEntryPxP;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.SampleTemplateFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

/**
 * OpenXML implementation of the {@link TemplateProvider} interface.
 * <p>
 * Provides templates adhering to the openXML specification for spreadsheets. The corresponding
 * mime-type is:
 * <p>
 * <code>application/vnd.openxmlformats-officedocument.spreadsheetml.sheet</code>
 *
 * @since 1.10.0
 */
@Component
public class TemplateProviderOpenXML implements TemplateProvider {

  private static final Logger log = logger(TemplateProviderOpenXML.class);
  private final SampleTemplateFactory sampleTemplateFactory;
  private final MeasurementTemplateFactory measurementTemplateFactory;

  public TemplateProviderOpenXML() {
    this.sampleTemplateFactory = new SampleTemplateFactory();
    this.measurementTemplateFactory = new MeasurementTemplateFactory();
  }

  @Override
  public MimeType providedMimeType() {
    return MimeType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  }

  @Override
  public DigitalObject getTemplate(TemplateRequest request) {
    return switch (request) {
      case SampleRegistration req -> getTemplate(req);
      case SampleUpdate req -> getTemplate(req);
      case SampleInformation req -> getTemplate(req);
      case MeasurementInformationCollectionNGS req -> getTemplate(req);
      case MeasurementInformationCollectionPxP req -> getTemplate(req);
    };
  }

  private DigitalObject getTemplate(SampleRegistration req) {
    var workbook = forRequest(req);

    return new TemplateContent(workbook, providedMimeType(),
        "sample registration template");
  }

  private DigitalObject getTemplate(SampleUpdate req) {
    var workbook = forRequest(req);

    return new TemplateContent(workbook, providedMimeType(),
        "sample update template");
  }

  private DigitalObject getTemplate(SampleInformation req) {
    var workbook = forRequest(req);

    return new TemplateContent(workbook, providedMimeType(), "sample information");
  }

  private DigitalObject getTemplate(MeasurementInformationCollectionPxP req) {
    var workbook = forRequest(req);

    return new TemplateContent(workbook, providedMimeType(), "measurement update template");
  }

  private Workbook forRequest(MeasurementInformationCollectionPxP req) {
    var entries = req.measurements().stream()
        .flatMap(value -> fromRequest(value).stream())
        .toList();
    return measurementTemplateFactory.forUpdatePxP(entries).createWorkbook();
  }

  private DigitalObject getTemplate(MeasurementInformationCollectionNGS req) {
    var workbook = forRequest(req);

    return new TemplateContent(workbook, providedMimeType(), "measurement information");
  }

  private Workbook forRequest(MeasurementInformationCollectionNGS req) {
    var entries = req.measurements().stream()
        .flatMap(value -> fromRequest(value).stream())
        .toList();
    return measurementTemplateFactory.forUpdateNGS(entries).createWorkbook();
  }

  private static List<MeasurementEntryPxP> fromRequest(MeasurementInformationPxP req) {
    var entries = new ArrayList<MeasurementEntryPxP>();
    for (var specificMetadataEntry : req.specificMetadata().entrySet()) {
      var specificData = specificMetadataEntry.getValue();
      var entry = new MeasurementEntryPxP(
          req.measurementId(),
          specificData.sampleId(),
          specificData.sampleName(),
          req.samplePoolGroup(),
          req.technicalReplicateName(),
          req.organisationIRI(),
          req.organisationName(),
          req.facility(),
          req.msDeviceIRI(),
          req.deviceName(),
          specificData.fractionName(),
          req.digestionMethod(),
          req.digestionEnzyme(),
          req.enrichmentMethod(),
          req.injectionVolume(),
          req.lcColumn(),
          req.lcmsMethod(),
          req.labelingType(),
          specificData.label(),
          specificData.comment()
      );
      entries.add(entry);
    }
    return entries;
  }

  private static List<MeasurementEntryNGS> fromRequest(MeasurementInformationNGS req) {
    var entries = new ArrayList<MeasurementEntryNGS>();
    for (var specificMetadataEntry : req.specificMetadata().entrySet()) {
      var specificData = specificMetadataEntry.getValue();
      var entry = new MeasurementEntryNGS(
          req.measurementId(),
          specificData.sampleId(),
          specificData.sampleName(),
          req.samplePoolGroup(),
          req.organisationIRI(),
          req.organisationName(),
          req.facility(),
          req.instrumentIRI(),
          req.instrumentName(),
          req.sequencingReadType(),
          req.libraryKit(),
          req.flowCell(),
          req.sequencingRunProtocol(),
          specificData.indexI7(),
          specificData.indexI5(),
          specificData.comment()
      );
      entries.add(entry);
    }
    return entries;
  }

  private Workbook forRequest(SampleRegistration req) {
    return sampleTemplateFactory.forRegistration(
        req.analysisMethods(),
        req.conditions(),
        req.analytes(),
        req.species(),
        req.specimen(),
        req.confoundingVariables()).createWorkbook();
  }

  private Workbook forRequest(SampleInformation req) {
    return sampleTemplateFactory.forInformation(
        req.samples(),
        req.analysisMethods(),
        req.conditions(),
        req.analytes(),
        req.species(),
        req.specimen(),
        req.experimentalGroups(),
        req.confoundingVariables(),
        req.confoundingVariableLevels()
    ).createWorkbook();
  }

  private Workbook forRequest(SampleUpdate req) {
    var info = req.information();
    return sampleTemplateFactory.forUpdate(
        info.samples(),
        info.analysisMethods(),
        info.conditions(),
        info.analytes(),
        info.species(),
        info.specimen(),
        info.experimentalGroups(),
        info.confoundingVariables(),
        info.confoundingVariableLevels()).createWorkbook();
  }

  private record TemplateContent(Workbook workbook, MimeType type, String templateName) implements
      DigitalObject {

    @Override
    public InputStream content() {
      var outputStream = new ByteArrayOutputStream();

      try {
        workbook.write(outputStream);
      } catch (IOException exp) {
        log.error("Error writing file", exp);
        throw new RuntimeException("Error writing file", exp);
      }

      return new ByteArrayInputStream(outputStream.toByteArray());
    }

    @Override
    public MimeType mimeType() {
      return type;
    }

    @Override
    public Optional<String> name() {
      return Optional.of(templateName);
    }

    @Override
    public Optional<String> id() {
      return Optional.empty();
    }
  }
}
