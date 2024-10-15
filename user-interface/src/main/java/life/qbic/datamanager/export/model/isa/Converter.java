package life.qbic.datamanager.export.model.isa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Project;

/**
 * <b>Small converter utility class</b>
 * <p>
 * Converts some domain model concepts to ISA JSON concepts
 *
 * @since 1.5.0
 */
public class Converter {

  public static Investigation from(Project project) {
    Objects.requireNonNull(project);

    var projectManager = from(project.getProjectManager());
    var principalInvestigator = from(project.getPrincipalInvestigator());

    return new Investigation(project.getProjectCode().value(),
        project.getProjectIntent().projectTitle().title(),
        project.getProjectIntent().objective().objective(),
        List.of(projectManager, principalInvestigator));
  }

  private static Person from(Contact contact) {
    var nameList = Arrays.stream(contact.fullName().split("\\s")).toList();

    if (nameList.isEmpty()) {
      return new Person("", "", contact.emailAddress());
    }
    if (nameList.size() == 1) {
      return new Person("", nameList.get(0), contact.emailAddress());
    }
    return new Person(nameList.get(0), nameList.get(1), contact.emailAddress());
  }

}
