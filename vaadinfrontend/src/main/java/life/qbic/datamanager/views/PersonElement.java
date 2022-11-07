package life.qbic.datamanager.views;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import life.qbic.projectmanagement.domain.project.PersonReference;

import java.util.Objects;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class PersonElement extends VerticalLayout {
  private final Label projectManagerLabel;
  private final Label emailLabel;

  public PersonElement(){
    projectManagerLabel = new Label("-");
    emailLabel = new Label();

    emailLabel.addClassNames("text-s","text-secondary");

    addClassNames("flex");
    add(projectManagerLabel, emailLabel);
  }
  public PersonElement(PersonReference personReference){
    Objects.requireNonNull(personReference);

    projectManagerLabel = new Label(personReference.fullName());
    emailLabel = new Label(personReference.getEmailAddress());

    emailLabel.addClassNames("text-s","text-secondary");

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
