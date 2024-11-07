package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.html.Div;
import java.util.Objects;

/**
 * <b>Contacts Form</b>
 *
 * <p>Provides fields for defining the principal investigator, project responsible
 * and project manager<</p>
 *
 * @since 1.6.0
 */
public class ContactsForm extends Div {

  private final ContactField principleInvestigator;
  private final ContactField personResponsible;
  private final ContactField projectManager;

  public ContactsForm(
      ContactField principalInvestigator,
      ContactField personResponsible,
      ContactField projectManager) {
    this.principleInvestigator =  Objects.requireNonNull(principalInvestigator);
    this.personResponsible = Objects.requireNonNull(personResponsible);
    this.projectManager = Objects.requireNonNull(projectManager);

    addClassNames("vertical-list", "gap-m");

    add(principalInvestigator, personResponsible, projectManager);
  }

}
