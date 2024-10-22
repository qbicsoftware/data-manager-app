package life.qbic.datamanager.views;

import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class TagFactory {

  public static Tag forMeasurement(MeasurementType measurementType) {
    return switch (measurementType) {
      case GENOMICS -> violetTag("Genomics");
      case PROTEOMICS -> pinkTag("Proteomics");
    };
  }

  private static Tag tagWithColor(String label, TagColor color) {
    var tag = new Tag(label);
    tag.setTagColor(color);
    return tag;
  }

  private static Tag violetTag(String label) {
    return tagWithColor(label, TagColor.VIOLET);
  }

  private static Tag pinkTag(String label) {
    return tagWithColor(label, TagColor.PINK);
  }

}
