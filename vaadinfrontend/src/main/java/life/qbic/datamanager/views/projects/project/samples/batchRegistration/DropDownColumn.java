package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes a table column (without header) that contains a dropdown menu with items and optional
 * label to style a spreadsheet
 * @since 1.0.0
 */
public class DropDownColumn {

  private String dropDownLabel = "";
  private Map<String, Integer> dropdownItemsWithMaxUse = new HashMap<>();
  private int fromRowIndex = 1;
  private int toRowIndex = 1000;
  private int colIndex = 0;

  public DropDownColumn addItem(String item) {
    return addItemWithMaxUse(item, Integer.MAX_VALUE);
  }

  public DropDownColumn addItemWithMaxUse(String item, int maxUse) {
    this.dropdownItemsWithMaxUse.put(item, maxUse);
    return this;
  }

  public DropDownColumn fromRowIndex(int i) {
    this.fromRowIndex = i;
    return this;
  }

  public DropDownColumn toRowIndex(int i) {
    this.toRowIndex = i;
    return this;
  }

  public DropDownColumn atColIndex(int i) {
    this.colIndex = i;
    return this;
  }

  public boolean isWithInRange(int row, int col) {
    return fromRowIndex <= row && toRowIndex >= row && col == colIndex;
  }

  public String getLabel() {
    return dropDownLabel;
  }

  public Map<String, Integer> getDropdownItemsWithMaxUse() {
    return dropdownItemsWithMaxUse;
  }
}
