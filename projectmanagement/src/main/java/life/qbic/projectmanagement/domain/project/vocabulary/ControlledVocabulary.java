package life.qbic.projectmanagement.domain.project.vocabulary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * object for controlled vocabulary information.
 *
 * @since 1.0.0
 */
public class ControlledVocabulary {

  public ControlledVocabulary(String identifier,
      Map<String,String> vocabularyTermsByLabel) {
    this.identifier = identifier;
    this.vocabularyTermsByLabel = vocabularyTermsByLabel;
  }

  private final String identifier;

  private Map<String,String> vocabularyTermsByLabel;

  public String getIdentifier() {
    return identifier;
  }

  public List<String> getVocabularyTermLabels() {
    return new ArrayList<>(vocabularyTermsByLabel.keySet());
  }

  public String getVocabularyTermCode(String label) {
    return vocabularyTermsByLabel.get(label);
  }

  public void addVocabularyTerm(String label, String code) {
    this.vocabularyTermsByLabel.put(label, code);
  }
}
