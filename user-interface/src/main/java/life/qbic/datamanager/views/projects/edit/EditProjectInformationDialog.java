package life.qbic.datamanager.views.projects.edit;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.projectmanagement.application.ContactRepository;

/**
 * <b>Project Information Dialog</b>
 *
 * <p>Dialog to create a project based on a project intent or to update a project's information</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class EditProjectInformationDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 7327075228498213661L;

  private final Binder<ProjectInformation> binder;
  private final EditProjectInformationForm formLayout;

  private ProjectInformation oldValue = new ProjectInformation();

  public EditProjectInformationDialog(ContactRepository contactRepository) {
    super();

    addClassName("edit-project-dialog");
    setHeaderTitle("Project Information");
    setConfirmButtonLabel("Save");
    setCancelButtonLabel("Cancel");

    formLayout = new EditProjectInformationForm();

    List<Contact> knownContacts = contactRepository.findAll().stream().map(contact ->
        new Contact(contact.fullName(), contact.emailAddress())).toList();

    if (knownContacts.isEmpty()) {
      formLayout.hideContactBox();
    } else {
      formLayout.setKnownContacts(knownContacts);
    }

    binder = formLayout.getBinder();

    add(formLayout);
  }

  public void setProjectInformation(ProjectInformation projectInformation) {
    binder.setBean(projectInformation);
    try {
      oldValue = new ProjectInformation();
      binder.writeBean(oldValue);
    } catch (ValidationException e) {
      oldValue = null;
      throw new IllegalArgumentException(
          "Project information should be valid but was not. " + projectInformation, e);
    }
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    ProjectInformation projectInformation = new ProjectInformation();
    try {
      binder.writeBean(projectInformation);
      fireEvent(
          new ProjectUpdateEvent(oldValue, projectInformation, this, clickEvent.isFromClient()));
    } catch (ValidationException e) {
      formLayout.validate();
    }
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    //as this is the first listener called on cancel event, no closing should happen here.
    //If this method closes the dialog, the calling code has no opportunity to prevent that.
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }


  public void addProjectUpdateEventListener(ComponentEventListener<ProjectUpdateEvent> listener) {
    addListener(ProjectUpdateEvent.class, listener);
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public static class CancelEvent extends
      life.qbic.datamanager.views.events.UserCancelEvent<EditProjectInformationDialog> {

    public CancelEvent(EditProjectInformationDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * <b>Project Update Event</b>
   *
   * <p>Indicates that a user submitted a project update request</p>
   *
   * @since 1.0.0
   */
  public static class ProjectUpdateEvent extends ComponentEvent<EditProjectInformationDialog> {

    @Serial
    private static final long serialVersionUID = 1072173555312630829L;

    private final ProjectInformation oldValue;
    private final ProjectInformation value;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     * @param oldValue   the project information before modification
     * @param value      the modified project information
     */
    public ProjectUpdateEvent(ProjectInformation oldValue, ProjectInformation value,
        EditProjectInformationDialog source, boolean fromClient) {
      super(source, fromClient);
      requireNonNull(value, "new project information (value) must not be null");
      this.oldValue = oldValue;
      this.value = value;
    }

    public Optional<ProjectInformation> getOldValue() {
      return Optional.ofNullable(oldValue);
    }

    public ProjectInformation getValue() {
      return value;
    }
  }

  public static final class ProjectInformation implements Serializable {

    private String projectId = "";

    @Serial
    private static final long serialVersionUID = -7260109309939021850L;
    @NotEmpty
    private String projectTitle = "";
    @NotEmpty
    private String projectObjective = "";

    private FundingEntry fundingEntry;
    @NotEmpty
    private Contact principalInvestigator;
    private Contact responsiblePerson;
    @NotEmpty
    private Contact projectManager;

    public Optional<FundingEntry> getFundingEntry() {
      if (fundingEntry == null || fundingEntry.isEmpty()) {
        return Optional.empty();
      }
      return Optional.ofNullable(fundingEntry);
    }

    public static ProjectInformation copy(ProjectInformation projectInformation) {
      ProjectInformation copy = new ProjectInformation();
      copy.projectId = projectInformation.projectId;
      copy.projectTitle = projectInformation.projectTitle;
      copy.projectObjective = projectInformation.projectObjective;
      copy.fundingEntry = projectInformation.fundingEntry;
      copy.principalInvestigator = projectInformation.principalInvestigator;
      copy.responsiblePerson = projectInformation.responsiblePerson;
      copy.projectManager = projectInformation.projectManager;
      return copy;
    }

    public void setFundingEntry(FundingEntry fundingEntry) {
      this.fundingEntry = fundingEntry;
    }

    public void setProjectTitle(String projectTitle) {
      this.projectTitle = projectTitle;
    }

    public void setProjectObjective(String projectObjective) {
      this.projectObjective = projectObjective;
    }

    public Contact getPrincipalInvestigator() {
      return principalInvestigator;
    }

    public void setPrincipalInvestigator(
        Contact principalInvestigator) {
      this.principalInvestigator = principalInvestigator;
    }

    public Optional<Contact> getResponsiblePerson() {
      return Optional.ofNullable(responsiblePerson);
    }

    public void setResponsiblePerson(Contact responsiblePerson) {
      this.responsiblePerson = responsiblePerson;
    }

    public Contact getProjectManager() {
      return projectManager;
    }

    public void setProjectManager(Contact projectManager) {
      this.projectManager = projectManager;
    }

    public String getProjectTitle() {
      return projectTitle;
    }

    public String getProjectObjective() {
      return projectObjective;
    }

    public void setProjectId(String projectId) {
      this.projectId = projectId;
    }

    public String getProjectId() {
      return projectId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ProjectInformation that = (ProjectInformation) o;
      return Objects.equals(projectTitle, that.projectTitle) && Objects.equals(
          projectObjective, that.projectObjective) && Objects.equals(fundingEntry,
          that.fundingEntry) && Objects.equals(principalInvestigator,
          that.principalInvestigator) && Objects.equals(responsiblePerson,
          that.responsiblePerson) && Objects.equals(projectManager, that.projectManager);
    }

    @Override
    public int hashCode() {
      return Objects.hash(projectTitle, projectObjective, fundingEntry, principalInvestigator,
          responsiblePerson, projectManager);
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", ProjectInformation.class.getSimpleName() + "[", "]")
          .add("projectTitle='" + projectTitle + "'")
          .add("projectObjective='" + projectObjective + "'")
          .add("principalInvestigator=" + principalInvestigator)
          .add("responsiblePerson=" + responsiblePerson)
          .add("projectManager=" + projectManager)
          .toString();
    }
  }
}
