package life.qbic.datamanager.views.settings

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import life.qbic.application.commons.ApplicationException
import life.qbic.application.commons.ApplicationException.ErrorCode
import life.qbic.application.commons.ApplicationException.ErrorParameters
import life.qbic.application.commons.Result
import life.qbic.usergroups.application.GroupInfoProjection
import life.qbic.usergroups.application.GroupService
import life.qbic.usergroups.domain.model.GroupDescription
import life.qbic.usergroups.domain.model.GroupId
import life.qbic.usergroups.domain.model.GroupName
import life.qbic.usergroups.domain.model.GroupType
import spock.lang.Specification

import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

/**
 * Unit tests for the new-group creation form.
 *
 * <p>Covers the client-side validation that mirrors the domain value objects, the create and
 * cancel events, the best-effort name availability hint, and the create orchestration (service
 * call, toasts, navigation) — all without a Spring or Vaadin {@code UI} context.
 */
class NewGroupFormSpec extends Specification {

  static final String USER_ID = "user-1"

  GroupService groupService = Mock(GroupService)
  NewGroupForm form
  List<NewGroupForm.CreateEvent> createEvents = []
  List<NewGroupForm.CancelEvent> cancelEvents = []
  List<String> availabilityCalls = []
  List<String> successToasts = []
  List<String> errorToasts = []
  int navigations = 0

  def setup() {
    Function<String, Boolean> availability = { String name ->
      availabilityCalls << name
      name != "Sprint Team"
    } as Function<String, Boolean>
    form = new NewGroupForm(availability, groupService,
        { USER_ID } as Supplier<String>,
        { String name -> successToasts << name } as Consumer<String>,
        { errorToasts << "error" } as Runnable,
        { navigations++ } as Runnable)
    form.addCreateListener { NewGroupForm.CreateEvent e -> createEvents << e }
    form.addCancelListener { NewGroupForm.CancelEvent e -> cancelEvents << e }
  }

  def "rejects a blank name with an inline error and fires no create event"() {
    given: "an empty form"
    form.setValues("", "some description")

    when: "the user clicks create"
    createButton().click()

    then: "the name is marked invalid and no event escapes"
    nameField().invalid
    createEvents.isEmpty()
  }

  def "rejects a name longer than 80 characters with an inline error and no create event"() {
    given: "an oversized name"
    form.setValues("x" * 81, null)

    when: "the user clicks create"
    createButton().click()

    then: "the name is marked invalid and no event escapes"
    nameField().invalid
    nameField().errorMessage.startsWith("Group name must not exceed 80 characters.")
    createEvents.isEmpty()
  }

  def "rejects a description longer than 500 characters with an inline error and no create event"() {
    given: "an oversized description"
    form.setValues("Sprint Team", "y" * 501)

    when: "the user clicks create"
    createButton().click()

    then: "the description is marked invalid and no event escapes"
    descriptionField().invalid
    descriptionField().errorMessage.startsWith("Group description must not exceed 500 characters.")
    createEvents.isEmpty()
  }

  def "creates the group with the trimmed name and description for valid input"() {
    given: "a valid name and description"
    def groupName = GroupName.from("Sprint Team")
    def groupDesc = GroupDescription.from("sprint planning team")
    and: "a displayed form"
    form.setValues("  Sprint Team  ", "  sprint planning team  ")

    when: "the user clicks create"
    createButton().click()

    then: "the service is called with trimmed values and we toast + navigate"
    1 * groupService.createAdHocGroup(USER_ID, groupName, groupDesc) >>
        Result.fromValue(new GroupInfoProjection(GroupId.create(), groupName, groupDesc,
            GroupType.ADHOC))
    successToasts == ["Sprint Team"]
    navigations == 1
  }

  def "creates the group with a null description when the description is blank"() {
    given: "a valid name and no description"
    def groupName = GroupName.from("Sprint Team")
    def groupDesc = GroupDescription.from(null)
    and: "a displayed form"
    form.setValues("Sprint Team", "   ")

    when: "the user clicks create"
    createButton().click()

    then: "the service is called with a null description and we toast + navigate"
    1 * groupService.createAdHocGroup(USER_ID, groupName, groupDesc) >>
        Result.fromValue(new GroupInfoProjection(GroupId.create(), groupName, groupDesc,
            GroupType.ADHOC))
    successToasts == ["Sprint Team"]
    navigations == 1
  }

  def "rejects a duplicate name inline and does not navigate"() {
    given: "a duplicate name rejection from the service"
    def groupName = GroupName.from("Sprint Team")
    def groupDesc = GroupDescription.from(null)
    and: "a displayed form"
    form.setValues("Sprint Team", null)

    when: "the user clicks create"
    createButton().click()

    then: "an inline name error is shown and no navigation or success happened"
    1 * groupService.createAdHocGroup(USER_ID, groupName, groupDesc) >>
        Result.fromError(new ApplicationException("duplicate", ErrorCode.DUPLICATE_GROUP_NAME,
            ErrorParameters.empty()))
    nameField().errorMessage
    nameField().invalid
    navigations == 0
    successToasts.isEmpty()
  }

  def "shows a generic error toast on service failure and does not navigate"() {
    given: "a general service failure"
    def groupName = GroupName.from("Sprint Team")
    def groupDesc = GroupDescription.from(null)
    and: "a displayed form"
    form.setValues("Sprint Team", null)

    when: "the user clicks create"
    createButton().click()

    then: "an error toast is shown and no navigation happens"
    1 * groupService.createAdHocGroup(USER_ID, groupName, groupDesc) >>
        Result.fromError(new ApplicationException("boom", ErrorCode.GENERAL,
            ErrorParameters.empty()))
    errorToasts == ["error"]
    navigations == 0
    successToasts.isEmpty()
  }

  def "fires a cancel event when the cancel button is clicked"() {
    when: "the user clicks cancel"
    cancelButton().click()

    then: "one cancel event is fired and no create event"
    cancelEvents.size() == 1
    createEvents.isEmpty()
  }

  def "shows the availability hint on name blur and calls the availability seam"() {
    given: "a name that is already taken"
    form.setValues("Sprint Team", null)

    when: "the availability check runs"
    form.runNameAvailabilityCheck()

    then: "the availability seam is consulted and a taken-hint is shown"
    availabilityCalls == ["Sprint Team"]
    nameField().helperText.contains("already taken")
  }

  def "shows an available hint once a unique name is entered"() {
    given: "a free name"
    form.setValues("New Team", null)

    when: "the availability check runs"
    form.runNameAvailabilityCheck()

    then: "the availability seam is consulted and the available-hint is shown"
    availabilityCalls == ["New Team"]
    nameField().helperText.contains("available")
  }

  def "exposes name availability check via the public markNameError seam"() {
    when: "a caller marks the name field as errored"
    form.markNameError("A group with this name already exists.")

    then: "the name field shows the error"
    nameField().invalid
    nameField().errorMessage.contains("already exists")
  }

  private TextField nameField() {
    allDescendants(form).find { it instanceof TextField } as TextField
  }

  private TextArea descriptionField() {
    allDescendants(form).find { it instanceof TextArea } as TextArea
  }

  private Button createButton() {
    allDescendants(form).find { it instanceof Button && it.text == "Create" } as Button
  }

  private Button cancelButton() {
    allDescendants(form).find { it instanceof Button && it.text == "Cancel" } as Button
  }

  private static List<Component> allDescendants(Component component) {
    List<Component> result = []
    collectDescendants(component, result)
    return result
  }

  private static void collectDescendants(Component component, List<Component> acc) {
    component.children.forEach { child ->
      acc << child
      collectDescendants(child, acc)
    }
  }
}