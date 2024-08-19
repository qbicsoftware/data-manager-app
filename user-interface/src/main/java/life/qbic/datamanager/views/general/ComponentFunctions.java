package life.qbic.datamanager.views.general;

import static java.util.Objects.nonNull;

import com.vaadin.flow.component.Component;

public class ComponentFunctions {

  private ComponentFunctions() {
  }

  /**
   * Navigates up through the tree and checks whether the potential parent is an actual parent of
   * the other component.
   *
   * @param potentialParent the component suspected to be a parent of the other component
   * @param other           the potential child component
   * @return true if protential parent is a parent of the other component
   */
  public static boolean isParentOf(Component potentialParent, Component other) {
    var currentParent = other.getParent().orElse(null);
    while (nonNull(currentParent)) {
      if (currentParent.equals(potentialParent)) {
        return true;
      }
      currentParent = currentParent.getParent().orElse(null);
    }
    return false;
  }


}
