package life.qbic.datamanager.views.identicon;

import static java.util.Objects.isNull;

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

  /**
   * Adds a rectangle to the canvas
   *
   * @param x      the upper left corner position x value
   * @param y      the upper left corner position y value
   * @param rx     the radius of corners in x direction; 0 -> square corners; max value = width/2;
   * @param ry     the radius of corners in y direction; 0 -> square corners; may value = height/2;
   * @param width  the width of the rectangle
   * @param height the height of the rectangle
   * @param color  the fill color; null values are allowed.
   * @return a builder with an added rectangle.
   */
  public SvgBuilder addRectangle(int x, int y, int rx, int ry, int width, int height,
      String color) {
    return new SvgBuilder(this.width, this.height,
        svgString + generateRectangle(x, y, rx, ry, width, height, color, null));
  }

  /**
   * @param x          the x coordinate of the center
   * @param y          the y coordinate of the center
   * @param radius     the radius of the circle
   * @param color      the fill color; null if currentColor is used
   * @param cssClasses the css class to apply; null in case of no css class.
   * @return a builder with an added circle
   */
  public SvgBuilder addCircle(int x, int y, int radius, String color, String cssClasses) {
    return new SvgBuilder(this.width, this.height,
        svgString + generateCircle(x, y, radius, color, cssClasses));
  }

  /**
   * Builds the SVG string
   * @return the SVG string corresponding to the svg.
   */
  public String build() {
    return svgString + "</svg>";
  }

  /**
   *
   * @param x the upper left corner position x value
   * @param y the upper left corner position y value
   * @param rx the radius of corners in x direction; 0 -> square corners; max value = width/2;
   * @param ry the radius of corners in y direction; 0 -> square corners; may value = height/2;
   * @param width the width of the rectangle
   * @param height the height of the rectangle
   * @param color the fill color; null values are allowed.
   * @param cssClass the css class to apply; null in case of no css class.
   * @return a string representation of the rectangle
   */
  private static String generateRectangle(int x, int y, int rx, int ry, int width, int height,
      String color, String cssClass) {
    var formattedColor = isNull(color) ? "currentColor" : color;
    var formattedCssClass = isNull(cssClass)
        ? "" : " class=\"%s\"";
    return
        "<rect x=\"%s\" y=\"%s\" rx=\"%s\" ry=\"%s\" width=\"%s\" height=\"%s\" fill=\"%s\"%s></rect>".formatted(
            x, y, rx, ry, width, height, formattedColor, formattedCssClass);
  }

  /**
   * @param cx       the x coordinate of the center
   * @param cy       the y coordinate of the center
   * @param radius   the radius of the circle
   * @param color    the fill color; null if currentColor is used
   * @param cssClass the css class to apply; null in case of no css class.
   * @return a string representation of the circle
   */
  private static String generateCircle(int cx, int cy, int radius, String color, String cssClass) {
    String formattedCssClass = isNull(cssClass) ? "" : " class=\"%s\"".formatted(cssClass);
    String formattedColor = isNull(color) ? "currentColor" : color;
    return "<circle cx=\"%s\" cy=\"%s\" r=\"%s\" fill=\"%s\"%s></circle>".formatted(cx, cy, radius,
        formattedColor, formattedCssClass);
  }


}
