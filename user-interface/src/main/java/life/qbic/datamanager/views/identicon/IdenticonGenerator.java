package life.qbic.datamanager.views.identicon;

import static java.util.Objects.isNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * An Identicon is a visual representation of a hash value. The input is hashed and turned into a
 * visual representation.
 *
 * @since 1.1.0
 */
public class IdenticonGenerator {

  private static final String[] COLOUR_SPACE = new String[]{
      "#1E88E5",
      "#7B99FA",
      "#53CDD8",
      "#F68787",
      "#96EAB7",
  };

  private static final String DEFAULT_COLOUR_BLACK = "#ffffff";

  private enum CSSClass {
    ONE("identicon-one"),
    TWO("identicon-two"),
    THREE("identicon-three"),
    FOUR("identicon-four");

    private final String cssClassName;

    CSSClass(String cssClassName) {
      this.cssClassName = cssClassName;
    }

    public String getCssClassName() {
      return cssClassName;
    }
  }
  private IdenticonGenerator() {
  }

  /**
   * Generates a valid SVG string from a given input
   *
   * @param input a non-empty string value used for hashing; must not be null
   * @return a string containing valid SVG content as specified in <a
   * href="http://www.w3.org/2000/svg">the SVG specification</a>
   */
  public static String generateIdenticonSVG(String input) {
    if (isNull(input) || input.isBlank()) {
      throw new IllegalArgumentException("Input cannot be null or empty");
    }
    //hash the input
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException e) {
      throw new IdenticonGenerationException(e);
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
    final String identiconColor = getHexColor(hashedInput);

    // use the css class to overwrite the colors
    int classByteValue = Byte.toUnsignedInt(hashedInput[63]);
    // unsigned byte max value 256
    CSSClass chosenClass = CSSClass.values()[0];
    for (CSSClass value : CSSClass.values()) {
      if (classByteValue <= 256.0 / (CSSClass.values().length - value.ordinal())) {
        chosenClass = value;
        break;
      }
    }

    //apply the visibility mask -> mirror horizontal and vertical
    SvgBuilder svgBuilder = SvgBuilder.startSvg(8, 8);
    for (int i = 0; i < visibilityMask.length; i++) {
      if (!visibilityMask[i]) {
        continue;
      }
      int x = i % 4;
      int y = i / 4;
      svgBuilder = svgBuilder.addRectangle(x, y, 0, 0, 1, 1, identiconColor,
              chosenClass.getCssClassName())
          .addRectangle(x, 7 - y, 0, 0, 1, 1, identiconColor, chosenClass.getCssClassName())
          .addRectangle(7 - x, y, 0, 0, 1, 1, identiconColor, chosenClass.getCssClassName())
          .addRectangle(7 - x, 7 - y, 0, 0, 1, 1, identiconColor, chosenClass.getCssClassName());
    }
    return svgBuilder
        .build();
  }

  private static String getHexColor(byte[] hashedInput) {
    if (hashedInput == null || hashedInput.length < 3) {
      return DEFAULT_COLOUR_BLACK;
    }
    // we are looking into the first 3 byte and create an XOR on them, to enhance the
    // variation of values a little bit
    var colourAssignmentByte = Byte.toUnsignedInt((byte) (hashedInput[0] ^ hashedInput[1] ^ hashedInput[2]));
    // Will always access with an index between 0 and the length of the colour space array minus 1
    return COLOUR_SPACE[colourAssignmentByte % COLOUR_SPACE.length];
  }

  public static class IdenticonGenerationException extends RuntimeException {

    public IdenticonGenerationException(Throwable cause) {
      super(cause);
    }
  }
}
