package life.qbic.datamanager.views.projects.project.samples.registration.batch;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.button.Button;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.projectmanagement.application.sample.SampleMetadata;
import life.qbic.projectmanagement.domain.model.batch.BatchId;

/**
 * //TODO replace {@link BatchRegistrationDialog} with this one
 *
 * @since <version tag>
 */
public class EditSampleBatchDialog extends DialogWindow {

  private final List<SampleMetadata> sampleMetadata;
  private final BatchId batchId;

  public EditSampleBatchDialog(BatchId batchId) {
    this.batchId = Objects.requireNonNull(batchId, "Batch ID cannot be null");
    setHeaderTitle("Edit Sample Batch");
    sampleMetadata = new ArrayList<>();
  }

  @Override
  public void close() {
    sampleMetadata.clear();
    super.close();
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(),
        Collections.unmodifiableList(sampleMetadata)));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  public static class ConfirmEvent extends ComponentEvent<EditSampleBatchDialog> {

    final List<SampleMetadata> sampleMetadata;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source         the source component
     * @param fromClient     <code>true</code> if the event originated from the client
     *                       side, <code>false</code> otherwise
     * @param sampleMetadata
     */
    public ConfirmEvent(EditSampleBatchDialog source, boolean fromClient,
        List<SampleMetadata> sampleMetadata) {
      super(source, fromClient);
      this.sampleMetadata = sampleMetadata;
    }
  }

  public static class CancelEvent extends ComponentEvent<EditSampleBatchDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(EditSampleBatchDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
