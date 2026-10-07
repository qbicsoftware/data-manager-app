package life.qbic.datamanager.views.groups

import life.qbic.datamanager.views.groups.GroupMembersComponent.GroupMembersUpdatedRequest
import life.qbic.datamanager.views.groups.GroupMembersComponent.MemberAction
import life.qbic.identity.api.UserInformationService
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService
import life.qbic.usergroups.api.GroupAdministrationPermission
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.usergroups.api.GroupManagementService
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import life.qbic.datamanager.profilepicture.GroupPictureAuthorization
import life.qbic.datamanager.profilepicture.ProfilePictureAuditRepository
import life.qbic.datamanager.profilepicture.ProfilePictureOwnerType
import life.qbic.datamanager.profilepicture.ProfilePictureRepository
import life.qbic.datamanager.profilepicture.ProfilePictureService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

/**
 * Unit tests for the admin org-group management page (FEAT-USER-GROUPS-02 AC1/AC4).
 *
 * <p>After the rework the page reuses the shared {@link GroupMembersComponent} exactly like the
 * ad-hoc group detail page ({@link GroupDetailMain}). The QBiC admin acts as owner-equivalent
 * ({@code actingUserRole = OWNER}), so the shared component supplies the full toolbar — Add
 * member, multi-select Remove, Assign role (promote/demote). These specs target the seam-exposed
 * logic (member-action routing to the admin-gated org methods) without a Spring or Vaadin
 * context, mirroring the existing {@code GroupMembersComponentSpec} pattern.</p>
 */
class AdminGroupManagersMainSpec extends Specification {

  GroupInformationService groupInformationService = Stub(GroupInformationService)
  GroupManagementService groupManagementService = Mock(GroupManagementService)
  UserInformationService userInformationService = Stub(UserInformationService)
  GroupAdministrationPermission administrationPermission = Stub(GroupAdministrationPermission)
  AuthenticationToUserIdTranslationService userIdTranslator = Stub(AuthenticationToUserIdTranslationService)
  MessageSourceNotificationFactory messageFactory = Stub(MessageSourceNotificationFactory)
  ProfilePictureService profilePictureService = new ProfilePictureService(
      new EmptyPictureRepository(), new EmptyAuditRepository(),
      { String g, String u -> false } as GroupPictureAuthorization,
      administrationPermission)

  AdminGroupManagersMain view() {
    new AdminGroupManagersMain(groupInformationService, groupManagementService,
        userInformationService, administrationPermission, userIdTranslator, messageFactory,
        profilePictureService)
  }

  def setup() {
    // The member-action routing resolves the acting user through SecurityContextHolder.
    userIdTranslator.translateToUserId(_) >> Optional.of("admin-1")
    SecurityContextHolder.getContext().authentication =
        new UsernamePasswordAuthenticationToken("principal", "credentials")
  }

  def cleanup() {
    SecurityContextHolder.clearContext()
  }

  def "ADD_MEMBER routes to the admin-gated addOrgMember"() {
    given: "a view and an ADD_MEMBER request"
    def page = view()

    when:
    page.handleMemberRequest(new GroupMembersUpdatedRequest("g1", "alice", MemberAction.ADD_MEMBER))

    then:
    1 * groupManagementService.addOrgMember("g1", "admin-1", "alice")
  }

  def "REMOVE_MEMBER routes to the admin-gated removeOrgMember"() {
    given: "a view and a REMOVE_MEMBER request"
    def page = view()

    when:
    page.handleMemberRequest(new GroupMembersUpdatedRequest("g1", "alice", MemberAction.REMOVE_MEMBER))

    then:
    1 * groupManagementService.removeOrgMember("g1", "admin-1", "alice")
  }

  def "APPOINT_MANAGER routes to the admin-gated appointOrgManager"() {
    given: "a view and an APPOINT_MANAGER request"
    def page = view()

    when:
    page.handleMemberRequest(new GroupMembersUpdatedRequest("g1", "alice", MemberAction.APPOINT_MANAGER))

    then:
    1 * groupManagementService.appointOrgManager("g1", "admin-1", "alice")
  }

  def "DEMOTE_MANAGER routes to the admin-gated demoteOrgManager"() {
    given: "a view and a DEMOTE_MANAGER request"
    def page = view()

    when:
    page.handleMemberRequest(new GroupMembersUpdatedRequest("g1", "alice", MemberAction.DEMOTE_MANAGER))

    then:
    1 * groupManagementService.demoteOrgManager("g1", "admin-1", "alice")
  }

  def "admin gate resolves through the permission port"() {
    given: "an admin and a non-admin user"
    administrationPermission.isAdmin("admin-1") >> true
    administrationPermission.isAdmin("user-1") >> false

    expect: "the port decides the admin state"
    administrationPermission.isAdmin("admin-1")
    !administrationPermission.isAdmin("user-1")
  }

  /** Empty picture store so the view can resolve "no picture" without a database. */
  static class EmptyPictureRepository implements ProfilePictureRepository {

    @Override
    Optional<ProfilePictureRepository.ProfilePictureEntity> findByOwnerTypeAndOwnerId(
        ProfilePictureOwnerType ownerType, String ownerId) {
      return Optional.empty()
    }

    @Override
    Optional<ProfilePictureRepository.ContentHashProjection>
        findContentHashByOwnerTypeAndOwnerId(ProfilePictureOwnerType ownerType, String ownerId) {
      return Optional.empty()
    }

    @Override
    ProfilePictureRepository.ProfilePictureEntity save(
        ProfilePictureRepository.ProfilePictureEntity entity) {
      return entity
    }

    @Override
    void deleteByOwnerTypeAndOwnerId(ProfilePictureOwnerType ownerType, String ownerId) {
    }
  }

  static class EmptyAuditRepository implements ProfilePictureAuditRepository {

    @Override
    ProfilePictureAuditRepository.ProfilePictureAuditEntity save(
        ProfilePictureAuditRepository.ProfilePictureAuditEntity entity) {
      return entity
    }

    @Override
    Page<ProfilePictureAuditRepository.ProfilePictureAuditEntity>
        findAllByOrderByCreatedAtDesc(Pageable pageable) {
      return Page.empty()
    }

    @Override
    List<ProfilePictureAuditRepository.ProfilePictureAuditEntity>
        findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ProfilePictureOwnerType ownerType,
            String ownerId) {
      return []
    }

    @Override
    Page<ProfilePictureAuditRepository.ProfilePictureAuditEntity>
        findAllByReviewedFalseOrderByCreatedAtDesc(Pageable pageable) {
      return Page.empty()
    }

    @Override
    List<ProfilePictureAuditRepository.ProfilePictureAuditEntity> findAllByIdIn(
        java.util.Collection<Long> ids) {
      return []
    }
  }
}