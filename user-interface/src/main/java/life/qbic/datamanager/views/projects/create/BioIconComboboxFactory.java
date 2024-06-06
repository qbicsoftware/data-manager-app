package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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
    comboBox.setRenderer(new ComponentRenderer<>(bioIcon -> {

      Span element = new Span(styleIcon(bioIcon), new Text(bioIcon.getLabel()));
      element.addClassName("icon-and-component");

      return element;
    }));
    comboBox.addValueChangeListener(valueChanged -> valueChanged.getSource()
        .setPrefixComponent(styleIcon(valueChanged.getValue())));
    return comboBox;
  }

  private static AbstractIcon<?> styleIcon(BioIcon bioIcon) {
    AbstractIcon<?> icon = bioIcon.getIconResource().createIcon();
    icon.addClassName("primary");
    return icon;
  }

}

