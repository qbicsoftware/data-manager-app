package life.qbic.projectmanagement.infrastructure.api.template;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.api.template.TemplateProvider;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class TemplateProviderOpenXML implements TemplateProvider {

  private static final Logger log = logger(TemplateProviderOpenXML.class);

  @Override
  public MimeType providedMimeType() {
    return MimeType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  }

  @Override
  public DigitalObject getTemplate(TemplateRequest request) {
    return switch (request) {
      case SampleRegistration req -> getTemplate(req);
      case SampleUpdate req -> getTemplate(req);
    };
  }

  private Workbook forRequest(SampleRegistration req) {
    return new SampleRegisterFactory(
        req.analysisMethods(),
        req.conditions(),
        req.analytes(),
        req.species(),
        req.specimen(),
        req.confoundingVariables()).createWorkbook();
  }

  private Workbook forRequest(SampleUpdate req) {
    return new SampleUpdateFactory(
        req.samplesInBatch(),
        req.analysisMethods(),
        req.conditions(),
        req.analytes(),
        req.species(),
        req.specimen(),
        req.experimentalGroups(),
        req.confoundingVariables(),
        req.confoundingVariableLevels()).createWorkbook();
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
