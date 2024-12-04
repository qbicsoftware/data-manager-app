package life.qbic.projectmanagement.application.confounding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableData;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableLevelData;
import life.qbic.projectmanagement.domain.repository.ConfoundingVariableLevelRepository;
import life.qbic.projectmanagement.domain.repository.ConfoundingVariableRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
  @Transactional(readOnly = true)
  public List<ConfoundingVariableInformation> listConfoundingVariablesForExperiment(
      String projectId, ExperimentReference experiment) {
    return variableRepository.findAll(projectId, experiment.id())
        .stream()
        .map(it -> new ConfoundingVariableInformation(new VariableReference(it.getId()),
            it.getName()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ConfoundingVariableInformation> loadInformationForVariables(String projectId,
      List<VariableReference> variables) {
    return variableRepository.findAllById(projectId,
            variables.stream().map(VariableReference::id).toList()).stream()
        .map(data -> new ConfoundingVariableInformation(new VariableReference(data.getId()),
            data.getName()))
        .toList();
  }

  @Override
  @Transactional
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
  @Transactional
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
  public List<ConfoundingVariableLevel> setVariableLevelsForSample(String projectId,
      ExperimentReference experiment, SampleReference sampleReference,
      Map<VariableReference, String> levels) {
    List<Long> variableIds = levels.keySet().stream().map(VariableReference::id).toList();
    List<ConfoundingVariableLevel> savedLevels = new ArrayList<>();
    if (!variableRepository.existsAllById(projectId, variableIds)) {
      throw new IllegalArgumentException(
          "Not all variables exist in the database. Provided variables: " + levels.keySet());
    }
    for (Entry<VariableReference, String> levelEntry : levels.entrySet()) {
      log.debug("Adding level %s for variable %s to sample %s".formatted(levelEntry.getValue(),
          levelEntry.getKey(), sampleReference.id()));
      Optional<ConfoundingVariableLevelData> existingLevel = levelRepository.findVariableLevelOfSample(
          projectId, sampleReference.id(), levelEntry.getKey().id());
      var constructedLevel = existingLevel.map(
          it -> new ConfoundingVariableLevelData(it.getId(), it.getVariableId(), it.getSampleId(),
              levelEntry.getValue())).orElse(
          new ConfoundingVariableLevelData(null, levelEntry.getKey().id(), sampleReference.id(),
              levelEntry.getValue()));
      ConfoundingVariableLevelData savedLevelData = levelRepository.save(projectId,
          constructedLevel);
      savedLevels.add(
          new ConfoundingVariableLevel(new VariableReference(savedLevelData.getVariableId()),
              new SampleReference(savedLevelData.getSampleId()), savedLevelData.getValue()));
    }
    return savedLevels;
  }

  @Override
  @Transactional(readOnly = true)
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
  @Transactional(readOnly = true)
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
  @Transactional(readOnly = true)
  public List<ConfoundingVariableLevel> listLevelsForVariables(String projectId,
      List<VariableReference> variableReferences) {
    return levelRepository.findAllForVariables(projectId,
            variableReferences.stream().map(VariableReference::id).toList()).stream()
        .map(data -> new ConfoundingVariableLevel(
            new VariableReference(data.getVariableId()),
            new SampleReference(data.getSampleId()),
            data.getValue()
        )).toList();
  }

  @Override
  @Transactional
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
