package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.ContentAlignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import life.qbic.projectmanagement.application.ExperimentValueFormatter;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.VariableName;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class ExperimentalGroupsCard extends VerticalLayout {

  private static class ExperimentalGroupCard extends VerticalLayout {

    private final String CARD_WIDTH = "300px";
    private final String CARD_HEIGHT = "400px";

    public ExperimentalGroupCard(VariableLevel... variableLevels) {
      H5 cardTitle = new H5();
      cardTitle.setText("Sample Group Name " + new Random().nextInt(20));
      FlexLayout tagsContainer = new FlexLayout();
      tagsContainer.setFlexWrap(FlexWrap.WRAP);
      tagsContainer.setFlexDirection(FlexDirection.ROW);
      tagsContainer.setAlignContent(ContentAlignment.START);
      add(cardTitle, tagsContainer);
      setWidth(CARD_WIDTH);
      setHeight(CARD_HEIGHT);
      getStyle().set("border", "1px solid gray");
      getStyle().set("margin", "5px 10px");
      fillWithVariableLevels(tagsContainer, variableLevels);
    }

    private void fillWithVariableLevels(FlexLayout tagsContainer, VariableLevel[] variableLevels) {
      for (VariableLevel variableLevel : variableLevels) {
        Tag tag = new Tag(
            variableLevel.variableName().value() + ":" + ExperimentValueFormatter.format(
                variableLevel.experimentalValue()));
        tagsContainer.add(tag);
      }
    }
  }


  public ExperimentalGroupsCard() {
    FlexLayout container = new FlexLayout();
    container.setFlexDirection(FlexDirection.ROW);
    container.setFlexWrap(FlexWrap.WRAP);
    container.setAlignContent(ContentAlignment.STRETCH);
    for (int i = 0; i < 22; i++) {
      container.add(getSampleGroup());
    }
    add(container);
    setHeight(80, Unit.PERCENTAGE);
  }

  private static ExperimentalGroupCard getSampleGroup() {
    List<VariableLevel> variableLevels = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      int randomInt = new Random().nextInt(5);
      VariableLevel variableLevel = new VariableLevel(VariableName.create("variable " + randomInt),
          ExperimentalValue.create(String.valueOf(randomInt * randomInt)));
      variableLevels.add(variableLevel);
    }
    return new ExperimentalGroupCard(variableLevels.toArray(VariableLevel[]::new));
  }
}
