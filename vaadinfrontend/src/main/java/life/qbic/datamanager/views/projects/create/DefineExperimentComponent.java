package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Define Experiment Component</b>
 *
 * <p>Component to define the minimum required experiment information within a project</p>
 *
 * @since 1.0.0
 */
public class DefineExperimentComponent extends Div {

  public final TextField experimentNameField = new TextField("Experiment Name");
  public final TextArea experimentalDesignDescription = new TextArea(
      "Experimental Design Description");
  public final MultiSelectComboBox<Species> speciesBox = new MultiSelectComboBox<>("Species");
  public final MultiSelectComboBox<Specimen> specimenBox = new MultiSelectComboBox<>("Specimen");
  public final MultiSelectComboBox<Analyte> analyteBox = new MultiSelectComboBox<>("Analyte");
  private final ExperimentDefinitionLayoutHandler experimentDefinitionLayoutHandler;

  public DefineExperimentComponent(
      @Autowired ExperimentalDesignSearchService experimentalDesignSearchService) {
    addClassName("create-experiment-content");
    Objects.requireNonNull(experimentalDesignSearchService);
    initExperimentDefinitionLayout();
    experimentDefinitionLayoutHandler = new ExperimentDefinitionLayoutHandler(
        experimentalDesignSearchService);
  }

  private void initExperimentDefinitionLayout() {
    initHeaderAndDescription();
    styleFields();
    add(speciesBox, specimenBox, analyteBox, experimentalDesignDescription);
  }

  private void initHeaderAndDescription() {
    Span experimentHeader = new Span("Experiment");
    Span experimentDescription = new Span(
        "Please specify the sample origin information of the samples. Multiple "
            + "values are allowed!");
    experimentHeader.addClassName("header");
    add(experimentHeader, experimentDescription, experimentNameField,
        experimentDescription);
  }

  private void styleFields() {
    speciesBox.setRequired(true);
    specimenBox.setRequired(true);
    analyteBox.setRequired(true);
    speciesBox.addClassName("chip-badge");
    specimenBox.addClassName("chip-badge");
    analyteBox.addClassName("chip-badge");
    String fullWidthClass = "full-width-input";
    speciesBox.addClassName(fullWidthClass);
    specimenBox.addClassName(fullWidthClass);
    analyteBox.addClassName(fullWidthClass);
    experimentNameField.addClassName(fullWidthClass);
    experimentalDesignDescription.addClassName(fullWidthClass);
  }

  public void hideNameField() {
    experimentNameField.setVisible(false);
  }

  public void showNameField() {
    experimentNameField.setVisible(true);
  }

  public boolean isValid() {
    return experimentDefinitionLayoutHandler.validateInput();
  }

  public void reset() {
    experimentDefinitionLayoutHandler.reset();
  }

  private final class ExperimentDefinitionLayoutHandler {

    private final List<Binder<?>> binders = new ArrayList<>();
    private final ExperimentalDesignSearchService experimentalDesignSearchService;

    public ExperimentDefinitionLayoutHandler(
        ExperimentalDesignSearchService experimentalDesignSearchService) {
      this.experimentalDesignSearchService = experimentalDesignSearchService;
      configureValidators();
      setupExperimentalDesignSearch();
    }

    private void configureValidators() {
      Binder<Container<Set<Species>>> binderSpecies = new Binder<>();
      binderSpecies.forField(speciesBox).asRequired("Please select at least one species")
          .bind(Container::value, Container::setValue);
      Binder<Container<Set<Analyte>>> binderAnalyte = new Binder<>();
      binderAnalyte.forField(analyteBox).asRequired("Please select at least one analyte")
          .bind(Container::value, Container::setValue);
      Binder<Container<Set<Specimen>>> binderSpecimen = new Binder<>();
      binderSpecimen.forField(specimenBox).asRequired("Please select at least one specimen")
          .bind(Container::value, Container::setValue);
      binders.addAll(List.of(binderSpecies, binderSpecimen, binderAnalyte));
    }

    private boolean validateInput() {
      binders.forEach(Binder::validate);
      return binders.stream().allMatch(Binder::isValid);
    }

    /**
     * Resets the values and validity of all components that implement value storing and validity
     * interfaces
     */
    private void reset() {
      resetChildValues();
      resetChildValidation();
    }

    private void resetChildValues() {
      getChildren().filter(comp -> comp instanceof HasValue<?, ?>)
          .forEach(comp -> ((HasValue<?, ?>) comp).clear());
    }

    private void resetChildValidation() {
      getChildren().filter(comp -> comp instanceof HasValidation)
          .forEach(comp -> ((HasValidation) comp).setInvalid(false));
    }

    private void setupExperimentalDesignSearch() {
      speciesBox.setItems(experimentalDesignSearchService.retrieveSpecies().stream()
          .sorted(Comparator.comparing(Species::label)).toList());
      speciesBox.setItemLabelGenerator(Species::value);
      specimenBox.setItems(experimentalDesignSearchService.retrieveSpecimens().stream()
          .sorted(Comparator.comparing(Specimen::label)).toList());
      specimenBox.setItemLabelGenerator(Specimen::value);
      analyteBox.setItems(experimentalDesignSearchService.retrieveAnalytes().stream()
          .sorted(Comparator.comparing(Analyte::label)).toList());
      analyteBox.setItemLabelGenerator(Analyte::value);
    }

  }

  static class Container<T> {

    private T value;

    T value() {
      return this.value;
    }

    void setValue(T newValue) {
      this.value = newValue;
    }

  }


}
