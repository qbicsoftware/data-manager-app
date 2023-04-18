package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.ContentAlignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.ExperimentValueFormatter;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class ExperimentalGroupsLayout extends VerticalLayout {

  private final FlexLayout experimentalGroupContainer;

  private final List<ExperimentalGroupCommandListener> experimentalGroupCommandListeners = new ArrayList<>();

  public ExperimentalGroupsLayout() {
    experimentalGroupContainer = new FlexLayout();
    experimentalGroupContainer.setFlexDirection(FlexDirection.ROW);
    experimentalGroupContainer.setFlexWrap(FlexWrap.WRAP);
    experimentalGroupContainer.setAlignContent(ContentAlignment.STRETCH);
    add(experimentalGroupContainer);
    setHeight(80, Unit.PERCENTAGE);
  }

  public void setExperimentalGroups(List<ExperimentalGroupDTO> experimentalGroups) {
    experimentalGroupContainer.removeAll();
    AddExperimentalGroupCard addExperimentalGroupCard = new AddExperimentalGroupCard();
    addExperimentalGroupCard.addClickListener(it -> fireExperimentalGroupCommand());
    experimentalGroupContainer.add(addExperimentalGroupCard);
    for (ExperimentalGroupDTO experimentalGroup : experimentalGroups) {
      // add card
      ExperimentalGroupCard experimentalGroupCard = getExperimentalGroup(experimentalGroup);
      experimentalGroupContainer.add(experimentalGroupCard);
    }

  }

  public void setExperimentalGroupCommandListener(ExperimentalGroupCommandListener listener) {
    experimentalGroupCommandListeners.clear();
    experimentalGroupCommandListeners.add(listener);
  }

  private void fireExperimentalGroupCommand() {
    AddExperimentalGroupCommand command = new AddExperimentalGroupCommand();
    for (ExperimentalGroupCommandListener experimentalGroupCommandListener : experimentalGroupCommandListeners) {
      experimentalGroupCommandListener.executeCommand(command);
    }
  }

  private static ExperimentalGroupCard getExperimentalGroup(
      ExperimentalGroupDTO experimentalGroup) {
    Set<VariableLevel> variableLevels = experimentalGroup.levels();
    return new ExperimentalGroupCard(variableLevels.toArray(VariableLevel[]::new));
  }

  public record AddExperimentalGroupCommand() {

  }

  @FunctionalInterface
  public interface ExperimentalGroupCommandListener {

    void executeCommand(AddExperimentalGroupCommand experimentalGroupCommand);

  }


  private static class ExperimentalGroupCard extends VerticalLayout {

    private final static String CARD_WIDTH = "200px";
    private final static String CARD_HEIGHT = "200px";


    public ExperimentalGroupCard(VariableLevel... variableLevels) {
      H5 cardTitle = new H5();
      cardTitle.setText("Experimental Group");
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
      variableLevels = Arrays.stream(variableLevels)
          .sorted((VariableLevel l1, VariableLevel l2) -> l1.variableName().value()
              .compareToIgnoreCase(l2.variableName().value())).toArray(VariableLevel[]::new);
      for (VariableLevel variableLevel : variableLevels) {
        Tag tag = new Tag(
            variableLevel.variableName().value() + ":" + ExperimentValueFormatter.format(
                variableLevel.experimentalValue()));
        tagsContainer.add(tag);
      }
    }
  }

  private static class AddExperimentalGroupCard extends VerticalLayout implements
      ClickNotifier<VerticalLayout> {

    private static final String CARD_WIDTH = "200px";
    private static final String CARD_HEIGHT = "200px";

    public AddExperimentalGroupCard() {
      Button addButton = new Button("Add Group");
      addButton.addClickListener(it -> fireEvent(new ClickEvent<VerticalLayout>(this)));
      setDefaultHorizontalComponentAlignment(Alignment.CENTER);
      setAlignItems(Alignment.CENTER);
      add(addButton);
      setWidth(CARD_WIDTH);
      setHeight(CARD_HEIGHT);
      getStyle().set("border", "1px solid gray");
      getStyle().set("margin", "5px 10px");
    }
  }


}
