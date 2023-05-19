package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.ContentAlignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignSelf;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding.Right;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding.Top;
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
    setPadding(false);
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
    return new ExperimentalGroupCard(experimentalGroup.sampleSize(),
        variableLevels.toArray(VariableLevel[]::new));
  }

  public record AddExperimentalGroupCommand() {

  }

  @FunctionalInterface
  public interface ExperimentalGroupCommandListener {

    void executeCommand(AddExperimentalGroupCommand experimentalGroupCommand);

  }

  private static class BaseExperimentalGroupCard extends VerticalLayout {

    public static final String CARD_WIDTH = "200px";
    public static final String CARD_HEIGHT = "200px";

    public BaseExperimentalGroupCard() {
      setWidth(CARD_WIDTH);
      setHeight(CARD_HEIGHT);
      setSpacing(false);
      getStyle().set("margin", "5px 5px");
      setCardLayoutStyle();
    }

    private void setCardLayoutStyle() {
      addClassNames(
          "rounded-m",
          "box-border",
          "rounded-m",
          "shadow-xs",
          "p-m"
      );
    }
  }

  private static class ExperimentalGroupCard extends BaseExperimentalGroupCard {

    public ExperimentalGroupCard(int sampleSize, VariableLevel... variableLevels) {
      H5 cardTitle = new H5();
      cardTitle.setText("Experimental Group");
      FlexLayout tagsContainer = new FlexLayout();
      tagsContainer.setFlexWrap(FlexWrap.WRAP);
      tagsContainer.setFlexDirection(FlexDirection.ROW);
      tagsContainer.setAlignContent(ContentAlignment.START);
      tagsContainer.addClassNames(Overflow.HIDDEN);
      tagsContainer.setSizeFull();
      // inheriting width leads to tags bigger than the text they contain
      // tags have to inherit container width as MAX-width, so they overflow correctly
      // we only know what that width is after setting it to full here
      tagsContainer.setMaxWidth(tagsContainer.getWidth());
      add(cardTitle, tagsContainer);
      fillWithVariableLevels(tagsContainer, variableLevels);
      Span sampleSizeText = new Span("Group size: "+sampleSize);
      sampleSizeText.addClassNames(FontWeight.BOLD, FontSize.XSMALL, AlignSelf.END, Margin.Right.SMALL);
      add(sampleSizeText);
    }

    private void overWriteSpacingStyles(Tag tag) {
      // classes have to be removed by name, or all classes are overwritten/multiple classes of the same type exist
      tag.removeClassName(Margin.Top.SMALL);
      tag.removeClassName(Padding.Top.SMALL);
      tag.removeClassName(Padding.Right.SMALL);

      tag.addClassName(Margin.Top.XSMALL);
      tag.addClassName(Right.XSMALL);
      tag.addClassName(Top.XSMALL);
    }

    private void fillWithVariableLevels(FlexLayout tagsContainer, VariableLevel[] variableLevels) {
      variableLevels = Arrays.stream(variableLevels)
          .sorted((VariableLevel l1, VariableLevel l2) -> l1.variableName().value()
              .compareToIgnoreCase(l2.variableName().value())).toArray(VariableLevel[]::new);
      for (VariableLevel variableLevel : variableLevels) {
        String formattedValue = ExperimentValueFormatter.format(
            variableLevel.experimentalValue());
        String experimentalValueText = variableLevel.variableName().value() + ":" + formattedValue;
        Tag tag = new Tag(experimentalValueText);
        overWriteSpacingStyles(tag);
        tag.getElement().setProperty("title", experimentalValueText);
        tagsContainer.add(tag);
      }
    }
  }

  private static class AddExperimentalGroupCard extends BaseExperimentalGroupCard implements
      ClickNotifier<VerticalLayout> {

    public AddExperimentalGroupCard() {
      Icon plusIcon = new Icon(VaadinIcon.PLUS);
      plusIcon.setSize(IconSize.LARGE);
      Button addButton = new Button("Add Group");
      addButton.addClickListener(it -> fireEvent(new ClickEvent<VerticalLayout>(this)));
      setDefaultHorizontalComponentAlignment(Alignment.CENTER);
      setAlignItems(Alignment.CENTER);
      setJustifyContentMode(JustifyContentMode.CENTER);
      this.setSpacing(true);
      add(plusIcon);
      add(addButton);
    }

  }


}
