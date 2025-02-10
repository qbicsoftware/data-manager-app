package life.qbic.datamanager.files.export;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FileNameFormatter {

  private static final String SPACE_REPLACEMENT = "-";
  private static final String PART_JOINER = "_";

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private FileNameFormatter() {

  }


  public static String formatWithTimestampedContext(LocalDate timestamp, String projectPart,
      String experimentPart, String type, String extension) {
    return DATE_FORMATTER.format(timestamp)
        + PART_JOINER
        + replaceSpaces(projectPart)
        + PART_JOINER
        + replaceSpaces(experimentPart)
        + PART_JOINER
        + replaceSpaces(type)
        + "." + replaceSpaces(extension);
  }

  private static String replaceSpaces(String projectPart) {
    return projectPart.replaceAll("\\s", SPACE_REPLACEMENT);
  }

  public static String formatWithVersion(String filename, int version,
      String extension) {
    return replaceSpaces(filename) + "_" + "v" + version + "." + replaceSpaces(extension);
  }
}
