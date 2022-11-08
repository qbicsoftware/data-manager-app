package life.qbic.datamanager.views;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import life.qbic.projectmanagement.domain.project.PersonReference;

import java.util.Objects;

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
  private final Label projectManagerLabel;
  private final Label emailLabel;

  public ContactElement(){
    projectManagerLabel = new Label("-");
    emailLabel = new Label();

    emailLabel.addClassNames("text-s","text-secondary");

    addClassNames("flex");
    add(projectManagerLabel, emailLabel);
  }
  public ContactElement(PersonReference personReference){
    Objects.requireNonNull(personReference);

    projectManagerLabel = new Label(personReference.fullName());
    emailLabel = new Label(personReference.getEmailAddress());

    emailLabel.addClassNames("text-s","text-secondary");

    this.setSpacing(false);

    add(projectManagerLabel, emailLabel);
  }

  public void setContent(String fullName, String email){
    projectManagerLabel.setText(fullName);
    emailLabel.setText(email);
  }

  public void reset(){
    projectManagerLabel.setText("-");
    emailLabel.setText("");
  }

}
