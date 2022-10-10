package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.Objects;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import org.springframework.stereotype.Component;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Component
public class ProjectInformationHandler {

  private ProjectInformationLayout layout;

  public void handle(ProjectInformationLayout projectInformationLayout) {
    Objects.requireNonNull(projectInformationLayout);
    if (projectInformationLayout != layout) {
      layout = projectInformationLayout;
      restrictInputLength();
    }
  }

  public void loadOfferContent(Offer offer) {
    layout.titleField.setValue(offer.projectTitle().title());
    layout.projectObjective.setValue(offer.projectObjective().objective());
    offer.experimentalDesignDescription()
        .ifPresent(it -> layout.experimentalDesignField.setValue(it.description()));
  }

  private void restrictInputLength() {
    layout.titleField.setMaxLength((int) ProjectTitle.maxLength());
    layout.projectObjective.setMaxLength((int) ProjectObjective.maxLength());
    layout.experimentalDesignField.setMaxLength(
        (int) ExperimentalDesignDescription.maxLength());

    layout.titleField.setValueChangeMode(ValueChangeMode.EAGER);
    layout.projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
    layout.experimentalDesignField.setValueChangeMode(ValueChangeMode.EAGER);

    layout.titleField.addValueChangeListener(
        e -> addConsumedLengthHelper(e, layout.titleField));
    layout.projectObjective.addValueChangeListener(
        e -> addConsumedLengthHelper(e, layout.projectObjective));
    layout.experimentalDesignField.addValueChangeListener(
        e -> addConsumedLengthHelper(e, layout.experimentalDesignField));
  }

  private void addConsumedLengthHelper(ComponentValueChangeEvent<TextArea, String> e,
      TextArea textArea) {
    int maxLength = textArea.getMaxLength();
    int consumedLength = e.getValue().length();
    e.getSource().setHelperText(consumedLength + "/" + maxLength);
  }

  private void addConsumedLengthHelper(ComponentValueChangeEvent<TextField, String> e,
      TextField textField) {
    int maxLength = textField.getMaxLength();
    int consumedLength = e.getValue().length();
    e.getSource().setHelperText(consumedLength + "/" + maxLength);
  }
}
