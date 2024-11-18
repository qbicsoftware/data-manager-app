package life.qbic.datamanager.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableLevel;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.SampleReference;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * TODO! REMOVE!!!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Route(value = "confounding", layout = UserMainLayout.class)
@PermitAll
@SpringComponent
@UIScope
public class ConfoundingView extends FormLayout implements HasUrlParameter<String> {

  private String projectId;
  private ExperimentId experimentId;
  private final TextArea textArea;
  private final TextField variableAdding;
  private final TextField levelAddingLevelValue;
  private final ComboBox<Sample> levelAddingSample;
  private final ComboBox<ConfoundingVariableInformation> variableSelect;

  private final ConfoundingVariableService confoundingVariableService;
  private final SampleInformationService sampleInformationService;

  @Override
  public void setParameter(BeforeEvent event,
      @OptionalParameter String parameter) {
    Location location = event.getLocation();
    QueryParameters queryParameters = location.getQueryParameters();
    this.projectId = queryParameters.getSingleParameter("project")
        .orElseThrow();
    this.experimentId = queryParameters.getSingleParameter("experiment")
        .map(ExperimentId::parse).orElseThrow();
    contextSet();
  }

  private void contextSet() {
    ExperimentReference experimentReference = new ExperimentReference(experimentId.value());
    reloadVariables(experimentReference);
    reloadSamples(experimentReference);

  }

  private void reloadSamples(ExperimentReference experimentReference) {
    var samples = sampleInformationService.retrieveSamplesForExperiment(
        ExperimentId.parse(experimentReference.id())).valueOrElse(List.of());
    levelAddingSample.setItems(samples);
  }

  private void reloadVariables(ExperimentReference experimentReference) {
    ConfoundingVariableInformation selectedValue = variableSelect.getValue();
    List<ConfoundingVariableInformation> confoundingVariables = confoundingVariableService.listConfoundingVariablesForExperiment(
        projectId,
        experimentReference);
    variableSelect.setItems(confoundingVariables);
    variableSelect.setValue(selectedValue);
  }

  public ConfoundingView(ConfoundingVariableService confoundingVariableService,
      SampleInformationService sampleInformationService) {
    this.confoundingVariableService = confoundingVariableService;
    this.sampleInformationService = sampleInformationService;
    levelAddingLevelValue = new TextField();
    levelAddingSample = new ComboBox<>();
    Button addLevelButton = new Button("Add Level");
    textArea = new TextArea();
    textArea.setReadOnly(true);
    textArea.setWidthFull();
    variableAdding = new TextField();
    Button addVariableButton = new Button("Add Variable");
    variableSelect = new ComboBox<>();
    variableSelect.setItemLabelGenerator(item -> item.variableName() + "[" + item.id().id() + "]");
    variableSelect.setRenderer(
        new TextRenderer<>(item -> item.variableName() + "[" + item.id().id() + "]"));
    addFormItem(new Div(variableAdding, addVariableButton), "Add a variable");
    addFormItem(variableSelect, "select from an existing variable");
    addFormItem(textArea, "Existing Levels for the selected variable");
    addFormItem(new Div(levelAddingSample, levelAddingLevelValue, addLevelButton),
        "Add a new level for the variable");

    addVariableButton.addClickListener(e -> {
      if (variableAdding.isEmpty()) {
        return;
      }
      ExperimentReference experimentReference = new ExperimentReference(this.experimentId.value());
      confoundingVariableService.createConfoundingVariable(projectId,
          experimentReference,
          variableAdding.getValue());
      reloadVariables(experimentReference);
    });
    addLevelButton.addClickListener(e -> {
      if (levelAddingLevelValue.isEmpty()) {
        return;
      }
      if (levelAddingSample.isEmpty()) {
        return;
      }
      if (variableSelect.isEmpty()) {
        return;
      }
      ExperimentReference experimentReference = new ExperimentReference(experimentId.value());
      confoundingVariableService.setVariableLevelForSample(projectId,
          experimentReference,
          new SampleReference(levelAddingSample.getValue().sampleId().value()),
          variableSelect.getValue().id(),
          levelAddingLevelValue.getValue());
      reloadVariables(experimentReference);
    });

    levelAddingSample.setRenderer(new TextRenderer<>(Sample::label));
    levelAddingSample.setItemLabelGenerator(Sample::label);
    variableSelect.addValueChangeListener(it -> {
      if (Objects.isNull(it.getValue()) || it.getValue().equals(variableSelect.getEmptyValue())) {
        textArea.clear();
        return;
      }
      List<ConfoundingVariableLevel> confoundingVariableLevels = confoundingVariableService.listLevelsForVariable(
          projectId, it.getValue()
              .id());
      List<SampleId> sampleIds = confoundingVariableLevels.stream()
          .map(ConfoundingVariableLevel::sample)
          .distinct()
          .map(SampleReference::id)
          .map(SampleId::parse)
          .toList();
      Map<String, Sample> samples = sampleInformationService.retrieveSamplesByIds(sampleIds)
          .stream()
          .collect(
              Collectors.toMap(o -> o.sampleId().value(), o -> o));
      List<String> values = confoundingVariableLevels.stream()
          .map(confoundingVariableLevel -> "%s has %s of %s".formatted(
              samples.get(confoundingVariableLevel.sample().id()).label(),
              it.getValue().variableName(), confoundingVariableLevel.level())).toList();
      textArea.setValue(String.join("\n", values));
    });
  }
}
