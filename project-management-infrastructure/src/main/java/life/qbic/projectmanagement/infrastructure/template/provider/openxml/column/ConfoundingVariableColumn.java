package life.qbic.projectmanagement.infrastructure.template.provider.openxml.column;

import java.util.Optional;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.VariableReference;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.Column;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.ExampleProvider.Helper;

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
