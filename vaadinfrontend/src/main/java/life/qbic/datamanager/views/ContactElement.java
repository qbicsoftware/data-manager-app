package life.qbic.datamanager.views;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import life.qbic.projectmanagement.domain.project.PersonReference;

/**
 * <b>Contact Element</b>
 *
 * <p>Element consisting of a vertical layout and labels to represent a {@link PersonReference} contact via full name and email.
 * Furthermore, the element contains specific styling for the labels.
 * </p>
 * <p>Use this element when you want to represent contact data of a person!</p>
 *
 * @since 1.0.0
 */
public class ContactElement extends VerticalLayout {
  private final Label nameLabel;
  private final Label emailLabel;

  public ContactElement() {
    this("-", "");
  }

  public ContactElement(String fullName, String emailAddress) {

    nameLabel = new Label(fullName);
    emailLabel = new Label(emailAddress);

    emailLabel.addClassNames("text-s", "text-secondary");

    this.setSpacing(false);

    add(nameLabel, emailLabel);
  }

  public static ContactElement from(PersonReference personReference) {
    return new ContactElement(personReference.fullName(), personReference.getEmailAddress());
  }

  public void setContent(String fullName, String email) {
    nameLabel.setText(fullName);
    emailLabel.setText(email);
  }

  public void clear() {
    setContent("-", "");
  }

}
