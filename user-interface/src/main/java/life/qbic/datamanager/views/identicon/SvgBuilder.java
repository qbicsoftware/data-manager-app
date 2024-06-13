package life.qbic.datamanager.views.identicon;

/**
 * Builds valid SVG images
 *
 * For the SVG specification see `<a href="http://www.w3.org/2000/svg">http://www.w3.org/2000/svg</a>`
 *
 * @since 1.1.0
 */
public class SvgBuilder {

  private final int width;
  private final int height;
  private final String svgString;

  private SvgBuilder(int width, int height, String svgString) {
    this.width = width;
    this.height = height;
    this.svgString = svgString;
  }

  /**
   * Creates a builder and starts the svg building on a canvas of 10x10 in internal units.
   *
   * @return a new intance of {@link SvgBuilder}
   * @see #startSvg(int, int)
   */
  public static SvgBuilder startSvg() {
    return startSvg(10, 10);
  }

  /**
   * Creates a builder and starts the svg building.
   * @param width the canvas width in svg units
   * @param height the canvas height in svg units
   * @return a new intance of {@link SvgBuilder}
   * @see #startSvg()
   */
  public static SvgBuilder startSvg(int width, int height) {
    String svgString =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + width + " " + height + "\">";
    return new SvgBuilder(width, height, svgString);
  }

  /**
   * Constructs a color in Hex notation from RGB values.
   * @param red the amount of red
   * @param green the amount of green
   * @param blue the amount of blue
   * @return a color in hex notation e.g. <span style="color:#00FFFF">#00FFFF</span> for <span style="color:#00FFFF">aqua</span>
   */
  public static String toHexColor(byte red, byte green, byte blue) {
    return "#"
        + Integer.toHexString(Byte.toUnsignedInt(red))
        + Integer.toHexString(Byte.toUnsignedInt(green))
        + Integer.toHexString(Byte.toUnsignedInt(blue));
  }

  public SvgBuilder addRectangle(int x, int y, int rx, int ry, int width, int height,
      String color) {
    return new SvgBuilder(this.width, this.height,
        svgString + generateRectangle(x, y, rx, ry, width, height, color));
  }

  public SvgBuilder addCircle(int x, int y, int radius, String color) {
    return new SvgBuilder(this.width, this.height, svgString + generateCircle(x, y, radius, color));
  }

  public String build() {
    return svgString + "</svg>";
  }

  private static String generateRectangle(int x, int y, int rx, int ry, int width, int height,
      String color) {
    return "<rect x=\"%s\" y=\"%s\" rx=\"%s\" ry=\"%s\" width=\"%s\" height=\"%s\"  fill=\"%s\" ></rect>".formatted(
        x, y, rx, ry, width, height, color);
  }

  private static String generateCircle(int cx, int cy, int radius, String color) {
    return "<circle cx=\"%s\" cy=\"%s\" r=\"%s\" fill=\"%s\"></circle>".formatted(cx, cy, radius,
        color);
  }


}
