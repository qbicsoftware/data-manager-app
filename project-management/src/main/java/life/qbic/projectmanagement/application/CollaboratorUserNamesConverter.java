package life.qbic.projectmanagement.application;

import jakarta.persistence.Converter;

/**
 * Converts a string of comma separated usernames to a list of usernames.
 *
 * @since 1.0.0
 */
@Converter(autoApply = false)
public class CollaboratorUserNamesConverter extends CommaSeparatedStringListConverter {
}
