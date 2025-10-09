package life.qbic.datamanager.views.general.contact;

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

  public ContactsForm(
      ContactField principalInvestigator,
      ContactField personResponsible,
      ContactField projectManager) {
    Objects.requireNonNull(principalInvestigator);
    Objects.requireNonNull(personResponsible);
    Objects.requireNonNull(projectManager);

    addClassNames("flex-vertical", "width-full", "gap-m");

    add(principalInvestigator, personResponsible, projectManager);
  }

}
