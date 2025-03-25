package life.qbic.projectmanagement.infrastructure.api.template;

import java.util.Optional;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.VariableReference;
import life.qbic.projectmanagement.infrastructure.api.template.ExampleProvider.Helper;

public record ConfoundingVariableColumn(VariableReference variableReference, int index,
                                        String headerName) implements Column {

  @Override
  public boolean isMandatory() {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public Optional<Helper> getFillHelp() {
    return Optional.empty();
  }
}
