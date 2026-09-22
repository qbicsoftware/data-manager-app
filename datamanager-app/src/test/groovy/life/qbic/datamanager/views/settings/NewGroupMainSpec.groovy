package life.qbic.datamanager.views.settings

import com.vaadin.flow.component.button.Button
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
 * Unit tests for the new-group page route logic.
 *
 * <p>Exercises the seam-based constructor so no Vaadin {@code UI} or Spring context is required:
 * success navigates back to My Groups, a duplicate name is rejected inline, and generic errors
 * surface a toast without navigating.
 */
class NewGroupMainSpec extends Specification {

  static final String USER_ID = "user-1"

  GroupService groupService = Mock(GroupService)
  Function<String, Boolean> nameAvailability = { true } as Function<String, Boolean>
  List<String> successToasts = []
  List<String> errorToasts = []
  int navigations = 0
  List<String> nameErrors = []
  NewGroupMain main

  def setup() {
    Consumer<String> successSeam = { String name -> successToasts << name } as Consumer<String>
    Runnable errorSeam = { errorToasts << "error" } as Runnable
    Runnable navigateSeam = { navigations++ } as Runnable
    Consumer<String> nameErrorSeam = { String message -> nameErrors << message } as Consumer<String>
    Supplier<String> userIdSeam = { USER_ID } as Supplier<String>

    main = new NewGroupMain(groupService, nameAvailability, successSeam, errorSeam,
        navigateSeam, userIdSeam, nameErrorSeam)
  }

  def "creates an ad-hoc group on submit and navigates back with a success toast"() {
    given: "a group service that reports a successful creation"
    def groupName = GroupName.from("Sprint Team")
    def groupDescription = GroupDescription.from("sprint planning team")
    def projection = new GroupInfoProjection(GroupId.create(), groupName, groupDescription,
        GroupType.ADHOC)
    groupService.createAdHocGroup(USER_ID, groupName, groupDescription) >>
        Result.fromValue(projection)

    and: "a displayed form"
    renderForm()

    when: "the user submits valid data"
    submitForm(main, "Sprint Team", "sprint planning team")

    then: "a success toast is shown, the group was created and the user is navigated back"
    successToasts == ["Sprint Team"]
    navigations == 1
    nameErrors.isEmpty()
  }

  def "rejects a duplicate group name inline and does not navigate"() {
    given: "a group service that reports a duplicate name"
    groupService.createAdHocGroup(USER_ID, GroupName.from("Sprint Team"),
        GroupDescription.from(null)) >>
        Result.fromError(new ApplicationException("duplicate", ErrorCode.DUPLICATE_GROUP_NAME,
            ErrorParameters.empty()))

    and: "a displayed form"
    renderForm()

    when: "the user submits the taken name"
    submitForm(main, "Sprint Team", null)

    then: "an inline name error is set and no navigation or toast happened"
    nameErrors == [NewGroupMain.DUPLICATE_NAME_FIELD_MESSAGE]
    navigations == 0
    successToasts.isEmpty()
    errorToasts.isEmpty()
  }

  def "shows a generic error toast on service failure and does not navigate"() {
    given: "a group service that fails with a general error"
    groupService.createAdHocGroup(USER_ID, GroupName.from("Sprint Team"),
        GroupDescription.from(null)) >>
        Result.fromError(new ApplicationException("boom", ErrorCode.GENERAL,
            ErrorParameters.empty()))

    and: "a displayed form"
    renderForm()

    when: "the user submits"
    submitForm(main, "Sprint Team", null)

    then: "the error toast seam fires and no navigation happens"
    errorToasts == ["error"]
    navigations == 0
    nameErrors.isEmpty()
  }

  private void renderForm() {
    main.beforeEnter(null)
    assert main.form() != null
  }

  private static void submitForm(NewGroupMain main, String name, String description) {
    def form = main.form()
    form.setValues(name, description)
    List<Button> buttons = []
    collectButtons(form, buttons)
    buttons.find { it.text == "Create" }.click()
  }

  private static void collectButtons(com.vaadin.flow.component.Component component,
      List<Button> buttons) {
    if (component instanceof Button button) {
      buttons << button
    }
    component.children.each { child ->
      collectButtons(child, buttons)
    }
  }
}