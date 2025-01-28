package life.qbic.datamanager.files.structure.sample;

import java.util.Optional;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.importing.parser.ExampleProvider.Helper;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.VariableReference;

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
