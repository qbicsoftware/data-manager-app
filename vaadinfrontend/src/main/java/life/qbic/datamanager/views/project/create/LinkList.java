package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import life.qbic.datamanager.views.project.create.LinkList.LinkElement;

/**
 * A list of link elements
 *
 * @param <T> the data behind the element
 * @param <C> the component representing an element
 */
public class LinkList<T, C extends Component & LinkElement> extends Composite<Div> {

  public interface LinkElement {

    void onUnlink(Consumer<LinkElement> consumer);

  }

  private final ComponentRenderer<C, T> renderer;
  private final Map<T, C> elements;
  private final VerticalLayout layout;

  LinkList(ComponentRenderer<C, T> renderer) {
    this.renderer = renderer;
    layout = new VerticalLayout();
    elements = new HashMap<>();
    getContent().add(layout);
  }

  private void addElement(T element) {
    if (elements.containsKey(element)) {
      removeElement(element);
    }

    C component = renderer.createComponent(element);
    elements.put(element, component);
    layout.add(component);
    component.onUnlink(it -> removeElement(element));
  }

  private void removeElement(T element) {
    UI.getCurrent().getPage().reload();
    if (!elements.containsKey(element)) {
      return;
    }
    Component currentComponent = elements.get(element);
    layout.remove(currentComponent);
    elements.remove(element);
  }

  public void addLink(T element) {
    addElement(element);
  }

  public void removeLink(T element) {
    removeElement(element);
  }

  public Collection<T> getItems() {
    return elements.keySet().stream().toList();
  }

}
