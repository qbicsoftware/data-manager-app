package life.qbic.controlling.infrastructure.sample.translation;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.controlling.infrastructure.sample.openbis.AnalyteTermMapper;

/**
 * <b>Simple openBIS term mapper</b>
 * <p>
 * OpenBIS term mapper implementation of the {@link AnalyteTermMapper} interface.
 * <p>
 * Enables the client to lookup a possible mapping openBIS term for a given input analyte value.
 *
 * @since 1.0.0
 */
public class SimpleOpenBisTermMapper implements AnalyteTermMapper {

  private static final Logger log = logger(SimpleOpenBisTermMapper.class);
  private static final Map<String, String> ANALYTE_TO_SAMPLE_TYPE;

  /*
  EXTEND MAPPINGS TO OPENBIS TERMS HERE
   */
  static {
    ANALYTE_TO_SAMPLE_TYPE = new HashMap<>();
    ANALYTE_TO_SAMPLE_TYPE.put("AMPLICON", "AMPLICON");
    ANALYTE_TO_SAMPLE_TYPE.put("CARBOHYDRATES", "CARBOHYDRATES");
    ANALYTE_TO_SAMPLE_TYPE.put("CELL_LYSATE", "CELL_LYSATE");
    ANALYTE_TO_SAMPLE_TYPE.put("CF_DNA", "CF_DNA");
    ANALYTE_TO_SAMPLE_TYPE.put("DNA", "DNA");
    ANALYTE_TO_SAMPLE_TYPE.put("GLYCANS", "GLYCANS");
    ANALYTE_TO_SAMPLE_TYPE.put("GLYCOPEPTIDES", "GLYCOPEPTIDES");
    ANALYTE_TO_SAMPLE_TYPE.put("LIPIDS", "LIPIDS");
    ANALYTE_TO_SAMPLE_TYPE.put("M_RNA", "M_RNA");
    ANALYTE_TO_SAMPLE_TYPE.put("PEPTIDES", "PEPTIDES");
    ANALYTE_TO_SAMPLE_TYPE.put("PHOSPHOPEPTIDES", "PHOSPHOPEPTIDES");
    ANALYTE_TO_SAMPLE_TYPE.put("PHOSPHOPROTEINS", "PHOSPHOPROTEINS");
    ANALYTE_TO_SAMPLE_TYPE.put("PHOSPHOLIPIDS", "PHOSPHOLIPIDS");
    ANALYTE_TO_SAMPLE_TYPE.put("PROTEINS", "PROTEINS");
    ANALYTE_TO_SAMPLE_TYPE.put("RNA", "RNA");
    ANALYTE_TO_SAMPLE_TYPE.put("R_RNA", "R_RNA");
    ANALYTE_TO_SAMPLE_TYPE.put("SINGLE_NUCLEI", "SINGLE_NUCLEI");
    ANALYTE_TO_SAMPLE_TYPE.put("SMALLMOLECULES", "SMALLMOLECULES");
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
