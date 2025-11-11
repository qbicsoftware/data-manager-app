package life.qbic.datamanager.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.util.Collection;

/**
 * Grid Details Item
 * <p>
 * Component based on the {@link Div} component which allows the addition of row based entries to be shown in the {@link com.vaadin.flow.component.details.Details}
 * within a {@link com.vaadin.flow.component.grid.Grid}.
 * It supports the rendition of components, list of values and singular values
 *
 */
public class GridDetailsItem extends Div {

  public static final String ENTRY_LABEL_CSS = "entry-label";
  public static final String ENTRY_CSS = "entry";

  public GridDetailsItem() {
    addClassName("grid-details-item");
  }

  public void addEntry(String propertyLabel, String propertyValue) {
    Span propertyLabelSpan = new Span(propertyLabel + ":");
    propertyLabelSpan.addClassName(ENTRY_LABEL_CSS);
    Span propertyValueSpan = new Span(propertyValue);
    propertyValueSpan.addClassName("entry-value");
    Span entry = new Span();
    entry.addClassName(ENTRY_CSS);
    entry.add(propertyLabelSpan, propertyValueSpan);
    add(entry);
  }

  public void addListEntry(String propertyLabel, Collection<String> propertyValues) {
    Span propertyLabelSpan = new Span(propertyLabel + ":");
    propertyLabelSpan.addClassName(ENTRY_LABEL_CSS);
    Div propertyValuesDiv = new Div();
    propertyValuesDiv.addClassName("entry-value-list");
    propertyValues.forEach(propertyValue -> {
      Span propertyValueSpan = new Span(propertyValue);
      propertyValuesDiv.add(propertyValueSpan);
      propertyValueSpan.addClassName("entry-value");
    });
    Span entry = new Span();
    entry.add(propertyLabelSpan, propertyValuesDiv);
    entry.addClassName(ENTRY_CSS);
    add(entry);
  }

  public void addComponentEntry(String propertyLabel, Component propertyValueComponent) {
    Span propertyLabelSpan = new Span(propertyLabel + ":");
    propertyLabelSpan.addClassName(ENTRY_LABEL_CSS);
    Span entry = new Span();
    entry.addClassName(ENTRY_CSS);
    entry.add(propertyLabelSpan, propertyValueComponent);
    add(entry);
  }
}
