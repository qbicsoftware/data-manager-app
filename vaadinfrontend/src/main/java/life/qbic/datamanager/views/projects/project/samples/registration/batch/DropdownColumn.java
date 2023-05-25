package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a table column (without header) that contains a dropdown menu with items and optional
 * label to style a spreadsheet
 * @since 1.0.0
 */
public class DropdownColumn {

  private String dropdownLabel = "";
  private List<String> items = new ArrayList<>();
  private int fromRowIndex = 1;
  private int toRowIndex = 1000;
  private int colIndex = 0;

  /**
   * Adds an item to the dropdown menu of this DropDownColumn
   * @param item String denoting the item
   * @return this DropdownColumn, now with one more item
   */
  public DropdownColumn addItem(String item) {
    items.add(item+" ");
    return this;
  }

  /**
   * Sets items to display in the dropdown menu of this DropDownColumn
   * @param items List of Strings denoting the items
   * @return this DropdownColumn, now with the provided items
   */
  public DropdownColumn withItems(List<String> items) {
    this.items = new ArrayList<>();
    for(String item : items) {
      this.addItem(item);
    }
    return this;
  }

  /**
   * Sets the minimum row index from which on the dropdown menu should be displayed
   * @param i the first row index
   * @return this DropdownColumn
   */
  public DropdownColumn fromRowIndex(int i) {
    this.fromRowIndex = i;
    return this;
  }

  /**
   * Sets the maximum row index until which the dropdown menu should be displayed
   * @param i the last row index
   * @return this DropdownColumn
   */
  public DropdownColumn toRowIndex(int i) {
    this.toRowIndex = i;
    return this;
  }

  /**
   * Sets the column index of the column in which the dropdown menu should be displayed
   * @param i the column index
   * @return this DropdownColumn
   */
  public DropdownColumn atColIndex(int i) {
    this.colIndex = i;
    return this;
  }

  /**
   * Tests if this DropDownColumn has been defined for a provided column index and if it includes a
   * provided row, that is, if a cell is to be rendered with this dropdown.
   * @param row the row index of the spreadsheet cell to test
   * @param col the column index of the spreadsheet cell to test
   * @return true, if the cell coordinates are within range of this DropDownColumn, false otherwise
   */
  public boolean isWithinRange(int row, int col) {
    return fromRowIndex <= row && toRowIndex >= row && col == colIndex;
  }

  /**
   * Returns the label of this DropDownColumn, if it was set
   * @return the label, which might be an empty String
   */
  public String getLabel() {
    return dropdownLabel;
  }

  /**
   * Returns the items of this DropDownColumn, if they were set
   * @return the list of items to display in the dropdown, if there are any
   */
  public List<String> getItems() {
    return items;
  }

  public boolean isInColumn(int columnIndex) {
    return columnIndex == colIndex;
  }

  /**
   * Increases the row range of this DropDownColumn to include the specified row
   * No change is made if the row index was already in that range.
   * @param rowIndex the row index of the spreadsheet cell
   */
  public void increaseToRow(int rowIndex) {
    if(this.toRowIndex < rowIndex) {
      this.toRowIndex = rowIndex;
    }
  }
}
