package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.dnd.DragEndEvent;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DragStartEvent;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * A List supporting drag and drop features to rearrange its items.
 *
 * @param <T> the type of Item to support. Should be a specific class.
 */
public class DragDropList<T extends Component> extends Composite<Div> {

  protected static class DropPosition<S extends Component> extends Composite<Div> implements
      DropTarget<S> {


    private DropPosition() {
      this.addClassNames("list-item-drop-position border rounded-02 width-full margin-none");
      setDropEffect(DropEffect.MOVE);
      setActive(false);
    }

    @Override
    public void setActive(boolean active) {
      DropTarget.super.setActive(active);
      setVisible(active);
    }
  }

  protected static class DragDropItem<T extends Component> extends Div {

    protected final T item;
    protected final DropPosition<DragDropItem<T>> dropBefore;
    protected final DropPosition<DragDropItem<T>> dropAfter;
    private final DragSource<DragDropItem<T>> dragSource;

    public DragDropItem(T item) {
      this.addClassNames("width-full draggable");
      this.item = Objects.requireNonNull(item);
      this.dropBefore = new DropPosition<>();
      this.dropAfter = new DropPosition<>();
//      dropBefore.setDropTargetComponent(this);
//      dropAfter.setDropTargetComponent(this);
      this.add(dropBefore, item, dropAfter);

      this.dragSource = new DragSource<>() {
        @Override
        public DragDropItem<T> getDragSourceComponent() {
          return DragDropItem.this;
        }

//    Setting only the item as draggable element does not work
//    https://github.com/vaadin/flow/issues/19039
//    https://github.com/vaadin/flow/issues/19040
//        @Override
//        public Element getDraggableElement() {
//          return item.getElement();
//        }
      };
      dragSource.setEffectAllowed(EffectAllowed.MOVE);
      dragSource.setDraggable(true);
      dragSource.setDragData(item);
      dragSource.addDragStartListener(dragStartEvent -> {
        dropBefore.setVisible(false);
        dropAfter.setVisible(false);
      });
      dragSource.addDragEndListener(dragEndEvent -> {
        dropBefore.setVisible(dropBefore.isActive());
        dropAfter.setVisible(dropAfter.isActive());
      });
      setDropActive(false);
    }

    public Registration addDropAfterListener(
        ComponentEventListener<DropEvent<DragDropItem<T>>> listener) {

      return dropAfter.addDropListener(
          it -> listener.onComponentEvent(new DropEvent<>(this, it.isFromClient(),
              it.getEffectAllowed().getClientPropertyValue())));
    }

    public Registration addDropBeforeListener(
        ComponentEventListener<DropEvent<DragDropItem<T>>> listener) {
      return dropBefore.addDropListener(
          it -> listener.onComponentEvent(new DropEvent<>(this, it.isFromClient(),
              it.getEffectAllowed().getClientPropertyValue())));
    }

    public void setDropActive(boolean dropActive) {
      dropBefore.setActive(dropActive);
      dropAfter.setActive(dropActive);
    }

    public Registration addDragStartListener(
        ComponentEventListener<DragStartEvent<DragDropItem<T>>> listener) {
      return dragSource.addDragStartListener(listener);
    }

    public Registration addDragEndListener(
        ComponentEventListener<DragEndEvent<DragDropItem<T>>> listener) {
      return dragSource.addDragEndListener(listener);
    }

    public void setDropBeforeEnabled(boolean enabled) {
      super.remove(dropBefore);
      if (enabled) {
        super.addComponentAsFirst(dropBefore);
      }
    }

    public void setDropAfterEnabled(boolean enabled) {
      super.remove(dropAfter);
      if (enabled) {
        super.add(dropAfter);
      }
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", DragDropItem.class.getSimpleName() + "[", "]")
          .add("item=" + item)
          .add("hashCode=" + hashCode())
          .toString();
    }

    T getItem() {
      return item;
    }

    public void setDraggable(boolean draggable) {
      dragSource.setDraggable(draggable);
    }
  }

  private final Class<T> itemClazz;
  private final List<DragDropItem<T>> items = new ArrayList<>();

  public DragDropList(Class<T> itemClazz) {
    this.itemClazz = itemClazz;
  }

  //region Manipulation API

  /**
   * Inserts the specified element at the specified position in this list (optional operation).
   * Shifts the element currently at that position (if any) and any subsequent elements to the right
   * (adds one to their indices).
   *
   * @param index index at which the specified element is to be inserted
   * @param item  element to be inserted
   * @throws NullPointerException      if the specified element is null
   * @throws IndexOutOfBoundsException if the index is out of range
   *                                   ({@code index < 0 || index > size()})
   */
  public void add(int index, T item) {
    Objects.requireNonNull(item);
    if (index < 0 || index > items.size()) {
      throw new IndexOutOfBoundsException("Index out of bounds");
    }
    if (!(index == 0 || index == items.size()) && getAt(index) == item) {
      return;
    }

    var constructedItem = new DragDropItem<>(item);

    //add dragsource
    if (index == 0) {
      //add as first, we do not care about existing items
      items.addFirst(constructedItem);
      getContent().addComponentAsFirst(constructedItem);
    } else if (index == items.size()) {
      //add as last, we do not care about existing items
      items.add(constructedItem);
      getContent().add(constructedItem);
      constructedItem.setDropAfterEnabled(true);
    } else {
      //we do care about existing items and need to get the component index
      DragDropItem<T> currentItemAtTheIndex = items.get(index);
      int componentIndex = getContent().indexOf(currentItemAtTheIndex);
      items.add(index, constructedItem);
      getContent().addComponentAtIndex(componentIndex, constructedItem);
    }
    updateDraggable();

    //add drop targets
    updateDropTargets();
    constructedItem.addDropBeforeListener(dropEvent -> {
      if (dropEvent.getDragSourceComponent().isEmpty()) {
        return;
      }
      if (dropEvent.getDragSourceComponent()
          .orElseThrow() instanceof DragDropList.DragDropItem<?> sourceDragDropItem
          && dropEvent.getComponent() instanceof DragDropList.DragDropItem<?> targetDragDropItem
          && itemClazz.equals(targetDragDropItem.getItem().getClass())
          && itemClazz.equals(sourceDragDropItem.getItem().getClass())) {
        //only if the types match and the
        var dropPosition = items.indexOf(targetDragDropItem);
        //unchecked cast is fine as itemClazz is of Class<T> and the item class is always the generic class of the DragDropItem
        move(dropPosition, (DragDropItem<T>) sourceDragDropItem);
      }
    });
    constructedItem.addDropAfterListener(dropEvent -> {
      if (dropEvent.getDragSourceComponent().isEmpty()) {
        return;
      }
      if (dropEvent.getDragSourceComponent()
          .orElseThrow() instanceof DragDropList.DragDropItem<?> sourceDragDropItem
          && dropEvent.getComponent() instanceof DragDropList.DragDropItem<?> targetDragDropItem
          && itemClazz.equals(targetDragDropItem.getItem().getClass())
          && itemClazz.equals(sourceDragDropItem.getItem().getClass())) {
        //only if the types match and the
        var dropPosition = items.indexOf(targetDragDropItem);
        //unchecked cast is fine as itemClazz is of Class<T> and the item class is always the generic class of the DragDropItem
        move(dropPosition + 1, (DragDropItem<T>) sourceDragDropItem);
      }
    });

    //specify behaviour
    constructedItem.addDragStartListener(dragStartEvent -> {
      items.forEach(it -> {
        it.setDropActive(true);
      });
      int draggedIdx = items.indexOf(dragStartEvent.getComponent());
      if (draggedIdx > 0) {
        DragDropItem<T> previous = items.get(draggedIdx - 1);
        previous.setDropAfterEnabled(false);
      }
      if (draggedIdx < items.size() - 1) {
        DragDropItem<T> nextItem = items.get(draggedIdx + 1);
        nextItem.setDropBeforeEnabled(false);
      }
      dragStartEvent.getComponent().setDropActive(false);
    });
    constructedItem.addDragEndListener(dragEndEvent -> {
      items.forEach(it -> {
        it.setDropActive(false);
      });
      //clear changes made during dragstarted
      updateDropTargets();
    });
  }

  /**
   * Appends the specified element to the end of this list.
   *
   * @see #add(int, Component)
   */
  public void add(T item) {
    add(items.size(), item);
  }

  /**
   * Removes the first occurrence of the specified element from this list, if it is present
   * (optional operation).  If this list does not contain the element, it is unchanged.  More
   * formally, removes the element with the lowest index {@code i} such that
   * {@code Objects.equals(item, get(i))} (if such an element exists).
   *
   * @param item element to be removed from this list, if present
   */
  public void remove(T item) {
    items.stream()
        .filter(it -> it.getItem().equals(item))
        .findFirst()
        .ifPresent(this::remove);
  }

  /**
   * Removes all of the elements from this list (optional operation). The list will be empty after
   * this call returns.
   */
  public void clear() {
    for (DragDropItem<T> item : List.copyOf(items)) {
      this.remove(item);
    }
  }
  //endregion

  //region Information API

  /**
   * Returns {@code true} if this list contains no elements.
   *
   * @return {@code true} if this list contains no elements
   */
  public boolean isEmpty() {
    return items.isEmpty();
  }

  /**
   * Returns the element at the specified position in this list.
   *
   * @param index index of the element to return
   * @return the element at the specified position in this list
   * @throws IndexOutOfBoundsException if the index is out of range
   *                                   ({@code index < 0 || index >= size()})
   * @see List#get(int)
   */
  public T getAt(int index) {
    return items.get(index).getItem();
  }

  /**
   * Returns the index of the first occurrence of the specified element in this list, or -1 if this
   * list does not contain the element. More formally, returns the lowest index {@code i} such that
   * {@code Objects.equals(item, get(i))}, or -1 if there is no such index.
   *
   * @param item element to search for; can be null
   * @return the index of the first occurrence of the specified element in this list, or -1 if this
   * list does not contain the element
   */
  public int indexOf(T item) {
    for (DragDropItem<T> dragDropItem : items) {
      if (dragDropItem.getItem().equals(item)) {
        return items.indexOf(dragDropItem);
      }
    }
    return -1;
  }

  /**
   * Returns the number of elements in this list.  If this list contains more than
   * {@code Integer.MAX_VALUE} elements, returns {@code Integer.MAX_VALUE}.
   *
   * @return the number of elements in this list
   * @see List#size()
   */
  public int size() {
    return items.size();
  }

  /**
   * Returns a sequential {@code Stream} with this list as its source.
   *
   * @return a sequential {@code Stream} over the elements in this collection
   */
  public Stream<T> stream() {
    return items.stream().map(DragDropItem::getItem);
  }

  public Class<T> getItemType() {
    return itemClazz;
  }
  //endregion

  private void remove(DragDropItem<T> dragDropItem) {
    items.remove(dragDropItem);
    getContent().remove(dragDropItem);
    updateDraggable();
    updateDropTargets();
  }

  private void move(int to, DragDropItem<T> dragDropItem) {
    int from = items.indexOf(dragDropItem);
    if (from == to) {
      return;
    }
    if (from < to) {
      //first remove then move as remove has no impact on to index
      items.add(to, dragDropItem);
      getContent().addComponentAtIndex(to, dragDropItem);
      items.remove(from);
    } else if (from > to) {
      items.add(to, dragDropItem);
      getContent().addComponentAtIndex(to, dragDropItem);
      items.remove(from + 1); //because we added one before
    }
    updateDropTargets();
  }

  private void updateDraggable() {
    if (isEmpty()) {
      return;
    }
    if (items.size() == 1) {
      items.getFirst().setDraggable(false);
    } else {
      items.forEach(item -> item.setDraggable(true));
    }
  }

  private void updateDropTargets() {
    if (isEmpty()) {
      return;
    }
    items.forEach(it -> {
      it.setDropAfterEnabled(false);
      it.setDropBeforeEnabled(true);
    });
    items.getLast().setDropAfterEnabled(true);
  }
}
