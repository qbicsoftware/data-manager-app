package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.Serial;
import java.util.List;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent.BioIcon;

/**
 * Factory class for creating ComboBoxes allowing the selection of icons for species and specimen.
 */
public class BioIconComboboxFactory {

  public ComboBox<BioIcon> iconBox(List<BioIcon> options, String title) {
    ComboBox<BioIcon> comboBox = new ComboBox<>(title);
    comboBox.setItems(options);
    comboBox.setWidth("150px");
    comboBox.setItemLabelGenerator(
        BioIcon::getLabel);
    comboBox.setRenderer(new ComponentRenderer<>(iconResource -> {
      Span element = new Span();
      element.addClassName("icon-and-component");
      AbstractIcon<?> icon = iconResource.getIconResource().createIcon();
      icon.addClassName("primary");
      element.add(icon);
      element.add(iconResource.getLabel());
      return element;
    }));
    return comboBox;
  }

  /**
   * Helper class that wraps a Span including an icon around a Combobox with icons. Not used due to
   * UX concerns, might be useful when more time can be invested into this.
   */
  public class ComboWithIcon extends Span {

    @Serial
    private static final long serialVersionUID = -8985311313964473711L;
    AbstractIcon<?> currentIcon;

    ComboBox<BioIcon> innerBox;

    public ComboWithIcon(ComboBox<BioIcon> box) {
      super();
      this.innerBox = box;
      addClassName("icon-and-component-with-label");
      box.addValueChangeListener(
          (ValueChangeListener<ComponentValueChangeEvent<ComboBox<BioIcon>, BioIcon>>)
              bioIconValueChangeEvent -> setNewIcon(bioIconValueChangeEvent.getValue()));

      currentIcon = VaadinIcon.QUESTION.create();
      add(currentIcon);
      add(box);
    }

    public ComboBox<BioIcon> getComboBox() {
      return innerBox;
    }

    private void setNewIcon(BioIcon value) {
      remove(currentIcon);
      currentIcon = value.getIconResource().createIcon();
      currentIcon.addClassName("primary");
      addComponentAsFirst(currentIcon);
    }
  }
}

