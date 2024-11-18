package life.qbic.projectmanagement.application.confounding;

import java.util.List;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableData;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableLevelData;
import life.qbic.projectmanagement.domain.repository.ConfoundingVariableLevelRepository;
import life.qbic.projectmanagement.domain.repository.ConfoundingVariableRepository;
import org.springframework.stereotype.Component;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Component
public class ConfoundingVariableServiceImpl implements ConfoundingVariableService {

  private final ConfoundingVariableRepository variableRepository;
  private final ConfoundingVariableLevelRepository levelRepository;
  private static final Logger log = LoggerFactory.logger(ConfoundingVariableServiceImpl.class);

  public ConfoundingVariableServiceImpl(ConfoundingVariableRepository variableRepository,
      ConfoundingVariableLevelRepository levelRepository) {
    this.variableRepository = variableRepository;
    this.levelRepository = levelRepository;
  }

  @Override
  public List<ConfoundingVariableInformation> listConfoundingVariablesForExperiment(
      String projectId, ExperimentReference experiment) {
    return variableRepository.findAll(projectId, experiment.id())
        .stream()
        .map(it -> new ConfoundingVariableInformation(new VariableReference(it.getId()),
            it.getName()))
        .toList();
  }

  @Override
  public List<ConfoundingVariableInformation> loadInformationForVariables(String projectId,
      List<VariableReference> variables) {
    return variableRepository.findAllById(projectId,
            variables.stream().map(VariableReference::id).toList()).stream()
        .map(data -> new ConfoundingVariableInformation(new VariableReference(data.getId()),
            data.getName()))
        .toList();
  }

  @Override
  public ConfoundingVariableInformation createConfoundingVariable(String projectId,
      ExperimentReference experiment, String variableName) {
    ConfoundingVariableData confoundingVariableData = new ConfoundingVariableData(null,
        experiment.id(), variableName);
    ConfoundingVariableData savedVariable = variableRepository.save(projectId,
        confoundingVariableData);
    return new ConfoundingVariableInformation(new VariableReference(savedVariable.getId()),
        savedVariable.getName());
  }

  @Override
  public ConfoundingVariableLevel setVariableLevelForSample(String projectId,
      ExperimentReference experiment, SampleReference sampleReference,
      VariableReference variableReference,
      String level) {
    ConfoundingVariableData existingVariable = variableRepository.findById(projectId,
        variableReference.id()).orElseThrow();
    log.debug(
        "Adding level %s for variable %s to sample %s".formatted(level, existingVariable.getName(),
            sampleReference.id()));
    Optional<ConfoundingVariableLevelData> existingLevel = levelRepository.findVariableLevelOfSample(
        projectId, sampleReference.id(),
        variableReference.id());
    var constructedLevel = existingLevel.map(
        it -> new ConfoundingVariableLevelData(
            it.getId(),
            it.getVariableId(),
            it.getSampleId(),
            level
        )
    ).orElse(new ConfoundingVariableLevelData(null, existingVariable.getId(),
        sampleReference.id(), level));
    ConfoundingVariableLevelData savedLevelData = levelRepository.save(projectId, constructedLevel);
    return new ConfoundingVariableLevel(new VariableReference(savedLevelData.getVariableId()),
        new SampleReference(savedLevelData.getSampleId()), savedLevelData.getValue());
  }

  @Override
  public Optional<ConfoundingVariableLevel> getVariableLevelForSample(String projectId,
      SampleReference sampleReference,
      VariableReference variableReference) {
    return levelRepository.findVariableLevelOfSample(projectId, sampleReference.id(),
        variableReference.id()).map(
        data -> new ConfoundingVariableLevel(new VariableReference(data.getVariableId()),
            new SampleReference(data.getSampleId()),
            data.getValue()));
  }

  @Override
  public List<ConfoundingVariableLevel> listLevelsForVariable(String projectId,
      VariableReference variableReference) {
    return levelRepository.findAllForVariable(projectId, variableReference.id()).stream()
        .map(data -> new ConfoundingVariableLevel(
            new VariableReference(data.getVariableId()),
            new SampleReference(data.getSampleId()),
            data.getValue()
        )).toList();
  }

  @Override
  public void deleteConfoundingVariable(String projectId,
      ExperimentReference experiment,
      VariableReference variableReference) {
    Optional<ConfoundingVariableData> optionalVariable = variableRepository.findById(projectId,
        variableReference.id());
    optionalVariable.ifPresent(variable -> {
      levelRepository.deleteAllForVariable(projectId, variableReference.id());
      variableRepository.deleteById(projectId, variableReference.id());
    });
  }
}
