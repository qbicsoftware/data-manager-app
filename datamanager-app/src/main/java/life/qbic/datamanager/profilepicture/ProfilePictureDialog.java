package life.qbic.datamanager.profilepicture;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import java.util.function.Consumer;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * Dialog for choosing and cropping a profile picture.
 *
 * <p>Hosts a {@link ProfilePictureCropField} and returns the cropped 256×256 PNG bytes on confirm.
 * The bytes are re-validated server-side by {@link ProfilePictureService}.</p>
 *
 * @since 1.19.0
 */
public class ProfilePictureDialog extends DialogWindow {

  private final ProfilePictureCropField cropField = new ProfilePictureCropField();
  private final Span error = new Span();
  private Consumer<byte[]> pictureSelectedListener = png -> {
  };

  public ProfilePictureDialog() {
    addClassName("profile-picture-dialog");
    setHeaderTitle("Profile picture");
    add(cropField, error);
    error.addClassName("profile-picture-dialog__error");
    error.setVisible(false);
    cropField.setValidationErrorHandler(reason -> {
      if (reason == null) {
        error.setVisible(false);
      } else {
        showError(ProfilePictureMessages.userMessageForReason(reason));
      }
    });
    setConfirmButtonLabel("Save picture");
    setCancelButtonLabel("Cancel");
  }

  /**
   * Registers the listener invoked with the cropped PNG bytes when the user confirms.
   *
   * @param listener the listener
   */
  public void addPictureSelectedListener(Consumer<byte[]> listener) {
    this.pictureSelectedListener = listener;
  }

  /**
   * Shows an inline error message and keeps the dialog open.
   *
   * @param message the message to show
   */
  public void showError(String message) {
    error.setText(message);
    error.setVisible(true);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    error.setVisible(false);
    cropField.requestCrop(png -> {
      if (png == null) {
        // Keep an existing constraint error visible; only fall back to the generic hint.
        if (!error.isVisible()) {
          showError("Please choose an image first.");
        }
        return;
      }
      pictureSelectedListener.accept(png);
    });
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    close();
  }
}
