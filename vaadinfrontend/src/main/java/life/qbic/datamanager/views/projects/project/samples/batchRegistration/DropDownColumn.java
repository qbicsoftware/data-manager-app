package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import java.rmi.ServerError;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes a table column (without header) that contains a dropdown menu with items and optional
 * label to style a spreadsheet
 * @since 1.0.0
 */
public class DropDownColumn {

  private String dropDownLabel = "";
  private List<String> items = new ArrayList<>();
  private int fromRowIndex = 1;
  private int toRowIndex = 1000;
  private int colIndex = 0;

  public DropDownColumn addItem(String item) {
    items.add(item+" ");
    return this;
  }

  public DropDownColumn withItems(List<String> items) {
    for(String item : items) {
      this.addItem(item);
    }
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

  public boolean isWithinRange(int row, int col) {
    return fromRowIndex <= row && toRowIndex >= row && col == colIndex;
  }

  public String getLabel() {
    return dropDownLabel;
  }

  public List<String> getItems() {
    return items;
  }

  public boolean isInColumn(int columnIndex) {
    return columnIndex == colIndex;
  }

  public void increaseToRow(int rowIndex) {
    if(this.toRowIndex < rowIndex) {
      this.toRowIndex = rowIndex;
    }
  }
}
