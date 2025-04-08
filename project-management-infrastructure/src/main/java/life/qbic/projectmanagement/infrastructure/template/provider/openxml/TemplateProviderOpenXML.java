package life.qbic.projectmanagement.infrastructure.template.provider.openxml;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.api.template.TemplateProvider;
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
  private final SampleTemplateFactory templateFactory;

  public TemplateProviderOpenXML() {
    this.templateFactory = new SampleTemplateFactory();
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

  private Workbook forRequest(SampleRegistration req) {
    return templateFactory.forRegistration(
        req.analysisMethods(),
        req.conditions(),
        req.analytes(),
        req.species(),
        req.specimen(),
        req.confoundingVariables()).createWorkbook();
  }

  private Workbook forRequest(SampleInformation req) {
    return templateFactory.forInformation(
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
    return templateFactory.forUpdate(
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
