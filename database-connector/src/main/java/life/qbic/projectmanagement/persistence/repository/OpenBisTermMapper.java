package life.qbic.projectmanagement.persistence.repository;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import life.qbic.logging.api.Logger;

/**
 * <b>OpenBIS term mapper implementation</b>
 * <p>
 * OpenBIS term mapper implementation of the {@link AnalyteTermMapper} interface.
 *
 * @since 1.0.0
 */
public class OpenBisTermMapper implements AnalyteTermMapper {

  private static final Logger log = logger(OpenBisTermMapper.class);
  private static final Map<String, String> ANALYTE_TO_SAMPLE_TYPE;

  static {
    ANALYTE_TO_SAMPLE_TYPE = new HashMap<>();
    ANALYTE_TO_SAMPLE_TYPE.put("RNA", "RNA");
    ANALYTE_TO_SAMPLE_TYPE.put("DNA", "DNA");
  }

  @Override
  public Optional<String> mapFrom(String term) {
    try {
      return Optional.of(ANALYTE_TO_SAMPLE_TYPE.get(term));
    } catch (NullPointerException e) {
      log.debug("Unknown analyte term '%s', cannot map to openBIS terminology.".formatted(term));
      return Optional.empty();
    }
  }
}
