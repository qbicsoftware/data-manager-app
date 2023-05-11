package life.qbic.projectmanagement.domain.project.sample;

import java.util.Objects;

public record SampleCode(String code) {

  public SampleCode(String code) {
    this.code = Objects.requireNonNull(code);
  }
}
