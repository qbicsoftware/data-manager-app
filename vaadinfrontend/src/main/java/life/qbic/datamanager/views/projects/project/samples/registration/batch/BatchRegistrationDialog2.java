package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class BatchRegistrationDialog2 extends DialogWindow {

  private final Text userHelpText = new Text("Please register your samples.");

  public BatchRegistrationDialog2() {
    addClassName("batch-registration-dialog");
    setConfirmButtonLabel("Register");
    TextField batchNameField = new TextField();
    batchNameField.addClassName("batch-name-field");
    batchNameField.addValueChangeListener(
        this::onBatchNameChanged);

    Button prefillSpreadsheet = new Button();
    prefillSpreadsheet.setText("Do it");
    prefillSpreadsheet.setAriaLabel("Prefill complete sample batch");
    prefillSpreadsheet.addClickListener(this::onPrefillClicked);
    prefillSpreadsheet.addClassName("prefill-batch");

    Button addRow = new Button();
    addRow.setText("Add Row");
    addRow.addClickListener(this::onAddRowClicked);
    addRow.addClassName("add-batch-row");

    Button removeLastRow = new Button();
    removeLastRow.setText("Remove Row");
    removeLastRow.addClickListener(this::onRemoveLastRowClicked);
    removeLastRow.addClassName("remove-batch-row");

    Div spreadsheet = new Div(
        new Text("Spreadsheet goes here")); //FIXME replace by real spreadsheet
    spreadsheet.setSizeFull(); //FIXME remove once real spreadsheet is in here

    // Register Batch
    //-------------------------
    // batchName -> prefillButton
    // please register your samples
    // ->     delete row -> add row
    // spreadsheet
    //-------------------------
    // -> cancel -> register
    setHeaderTitle("Register Batch");
    setResizable(true);

    Div batchControls = new Div();
    batchControls.addClassName("batch-controls");
    batchControls.add(batchNameField, prefillSpreadsheet);
    Div spreadsheetControls = new Div();
    spreadsheetControls.addClassName("spreadsheet-controls");
    spreadsheetControls.add(addRow, removeLastRow);

    add(batchControls,
        userHelpText,
        spreadsheetControls,
        spreadsheet);
  }

  private void onBatchNameChanged(ComponentValueChangeEvent<TextField, String> batchName) {
    this.userHelpText.setText(
        "Please register your samples for batch " + batchName.getValue() + ".");
  }

  private void onRemoveLastRowClicked(ClickEvent<Button> clickEvent) {
    //TODO remove the last row from the spreadsheet
  }

  private void onAddRowClicked(ClickEvent<Button> clickEvent) {
    //TODO add a row to the spreadsheet
  }

  private void onPrefillClicked(ClickEvent<Button> clickEvent) {
    //TODO prefill spreadsheet
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    //TODO
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  @Override
  public void close() {
    super.close();
    reset();
  }

  protected void reset() {
    //TODO implement
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public static class CancelEvent extends ComponentEvent<BatchRegistrationDialog2> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(BatchRegistrationDialog2 source, boolean fromClient) {
      super(source, fromClient);
    }
  }

}
