package life.qbic.datamanager.views;

import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;

/**
 * <b>Tag Factory</b>
 *
 * <p>Create display tags for all thinkable use cases.</p>
 *
 * @since 1.6.0
 */
public class TagFactory {

  private TagFactory() {}

  public static Tag forMeasurement(MeasurementType measurementType) {
    return switch (measurementType) {
      case GENOMICS -> pinkTag("Genomics");
      case PROTEOMICS -> violetTag("Proteomics");
    };
  }

  public static Tag forCustom(String label, TagColor tagColor) {
    return tagWithColor(label, tagColor);
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
