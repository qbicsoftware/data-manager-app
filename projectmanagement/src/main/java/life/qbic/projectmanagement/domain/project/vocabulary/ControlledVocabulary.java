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
      Map<String,String> vocabularyTermsByName) {
    this.identifier = identifier;
    this.vocabularyTermsByName = vocabularyTermsByName;
  }

  final private String identifier;

  private Map<String,String> vocabularyTermsByName;

  public String getIdentifier() {
    return identifier;
  }

  public List<String> getVocabularyTermNames() {
    return new ArrayList<>(vocabularyTermsByName.keySet());
  }

  public String getVocabularyTermCode(String name) {
    return vocabularyTermsByName.get(name);
  }

  public void addVocabularyTerm(String name, String code) {
    this.vocabularyTermsByName.put(name, code);
  }
}
