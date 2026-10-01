package life.qbic.datamanager.profilepicture;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * A client-side crop field for profile pictures.
 *
 * <p>Renders a file picker plus a Cropper.js square crop frame with a live circular preview. On
 * {@link #requestCrop(Consumer)} the browser exports the current crop as a 256×256 PNG data URL and
 * the decoded bytes are handed to the callback. The bytes are then re-validated and re-encoded
 * server-side by {@link ImageNormalizer} — this component is a convenience, never a trust
 * boundary.</p>
 *
 * @since 1.22.0
 */
@NpmPackage(value = "cropperjs", version = "1.6.2")
@JsModule("./javascript/profilepicturecropper.js")
public class ProfilePictureCropField extends Div {

  private static final String DATA_URL_PREFIX = "data:";

  public ProfilePictureCropField() {
    addClassName("profile-picture-crop");
  }

  @Override
  protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    getElement().executeJs("window.qbicProfilePictureCropper.init($0)", getElement());
  }

  /**
   * Exports the current crop from the browser and hands the decoded PNG bytes to the callback.
   *
   * @param callback receives the cropped PNG bytes, or {@code null} when no image is selected
   */
  public void requestCrop(Consumer<byte[]> callback) {
    getElement().executeJs("return window.qbicProfilePictureCropper.export($0)", getElement())
        .then(String.class, dataUrl -> callback.accept(decode(dataUrl)));
  }

  static byte[] decode(String dataUrl) {
    if (dataUrl == null || !dataUrl.startsWith(DATA_URL_PREFIX)) {
      return null;
    }
    int comma = dataUrl.indexOf(',');
    if (comma < 0) {
      return null;
    }
    try {
      return Base64.getDecoder().decode(dataUrl.substring(comma + 1));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
