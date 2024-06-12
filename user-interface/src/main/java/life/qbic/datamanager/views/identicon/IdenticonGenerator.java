package life.qbic.datamanager.views.identicon;

import static java.util.Objects.isNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class IdenticonGenerator {

  public static String generateIdenticon(String input) {
    if (isNull(input) || input.isBlank()) {
      throw new IllegalArgumentException("Input cannot be null or empty");
    }
    //hash the input
    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    byte[] hashedInput = digest.digest(input.getBytes(StandardCharsets.UTF_8));
    //SHA-512 always generates a 512 bit (64 byte) digest
    // As we have a 4x4 matrix (16 spots) to fill that will be mirrored, we can use 4 bits each to determine
    // the visibility of a matrix spot.
    boolean[] visibilityMask = new boolean[16];
    for (int i = 0; i < visibilityMask.length; i++) {
      visibilityMask[i] = Byte.toUnsignedInt((byte) (hashedInput[i] ^ hashedInput[i + 3])) >= 125;
    }

    // the color is determined by the first three bytes
    final String identiconColor = SvgBuilder.toHexColor((byte) (hashedInput[0] ^ hashedInput[1]),
        (byte) (hashedInput[1] ^ hashedInput[2]),
        (byte) (hashedInput[2] ^ hashedInput[3]));

    //apply the visibility mask -> mirror horizontal and vertical
    SvgBuilder svgBuilder = SvgBuilder.startSvg(8, 8);
    for (int i = 0; i < visibilityMask.length; i++) {
      if (!visibilityMask[i]) {
        continue;
      }
      int x = i % 4;
      int y = i / 4;
      svgBuilder = svgBuilder.addRectangle(x, y, 0, 0, 1, 1, identiconColor)
          .addRectangle(x, 7 - y, 0, 0, 1, 1, identiconColor)
          .addRectangle(7 - x, y, 0, 0, 1, 1, identiconColor)
          .addRectangle(7 - x, 7 - y, 0, 0, 1, 1, identiconColor);
    }
    return svgBuilder
        .build();
  }
}
