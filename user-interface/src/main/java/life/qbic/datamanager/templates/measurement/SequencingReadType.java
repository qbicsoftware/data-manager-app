package life.qbic.datamanager.templates.measurement;

import java.util.Arrays;
import java.util.List;

enum SequencingReadType {
  SINGLE_END("single-end"),
  PAIRED_END("paired-end");
  private final String presentationString;

  SequencingReadType(String presentationString) {
    this.presentationString = presentationString;
  }

  static List<String> getOptions() {
    return Arrays.stream(values()).map(it -> it.presentationString).toList();
  }
}
