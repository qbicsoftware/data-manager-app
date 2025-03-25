package life.qbic.application.commons;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FileNameFormatter {

  private static final String SPACE_REPLACEMENT = "-";
  private static final String PART_JOINER = "_";

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String SLASH_REPLACEMENT = "_";

  private FileNameFormatter() {

  }

  public static String formatWithTimestampedSimple(LocalDate localDate, String project, String type, String extension) {
    return DATE_FORMATTER.format(localDate)
        + PART_JOINER
        + replaceForbiddenCharacters(project)
        + PART_JOINER
        + replaceForbiddenCharacters(type)
        + "." + replaceForbiddenCharacters(extension);
  }

  public static String formatWithTimestampedContext(LocalDate timestamp, String projectPart,
      String experimentPart, String type, String extension) {
    return DATE_FORMATTER.format(timestamp)
        + PART_JOINER
        + replaceForbiddenCharacters(projectPart)
        + PART_JOINER
        + replaceForbiddenCharacters(experimentPart)
        + PART_JOINER
        + replaceForbiddenCharacters(type)
        + "." + replaceForbiddenCharacters(extension);
  }

  private static String replaceForbiddenCharacters(String input) {
    return replaceSlashes(replaceSpaces(input));
  }

  private static String replaceSpaces(String projectPart) {
    return projectPart.replaceAll("\\s", SPACE_REPLACEMENT);
  }

  private static String replaceSlashes(String input) {
    return input.replace("/", SLASH_REPLACEMENT);
  }

  public static String formatWithVersion(String filename, int version,
      String extension) {
    return replaceSpaces(filename) + "_" + "v" + version + "." + replaceSpaces(extension);
  }
}
