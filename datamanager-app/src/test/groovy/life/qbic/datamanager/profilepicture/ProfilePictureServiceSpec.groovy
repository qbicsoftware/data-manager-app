package life.qbic.datamanager.profilepicture

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import life.qbic.application.commons.ApplicationException.ErrorCode
import life.qbic.usergroups.api.GroupAdministrationPermission
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification

class ProfilePictureServiceSpec extends Specification {

    InMemoryProfilePictureRepository repository = new InMemoryProfilePictureRepository()
    InMemoryAuditRepository auditRepository = new InMemoryAuditRepository()

    GroupPictureAuthorization groupAuthorization = { String groupId, String actingUserId ->
        groupId == "group-1" && actingUserId == "manager"
    } as GroupPictureAuthorization

    GroupAdministrationPermission adminPermission = { String userId ->
        userId == "admin"
    } as GroupAdministrationPermission

    ProfilePictureService service = new ProfilePictureService(repository, auditRepository,
            groupAuthorization, adminPermission)

    def "stores a user picture and records a SET audit entry"() {
        when:
        def result = service.setUserPicture("user-1", png(400, 300))

        then:
        !result.isError()
        def stored = repository.findByOwnerTypeAndOwnerId(ProfilePictureOwnerType.USER, "user-1").get()
        stored.contentHash ==~ /^[0-9a-f]{64}$/
        stored.width == ImageNormalizer.TARGET_SIZE

        and:
        auditRepository.entries.size() == 1
        auditRepository.entries.first().action == ProfilePictureAuditEntry.ACTION_SET
        auditRepository.entries.first().previousContentHash == null
        auditRepository.entries.first().actorId == "user-1"
    }

    def "replacing a user picture records a REPLACE audit entry with the previous hash"() {
        given:
        service.setUserPicture("user-1", png(400, 300))
        def firstHash = repository.findByOwnerTypeAndOwnerId(
                ProfilePictureOwnerType.USER, "user-1").get().contentHash

        when:
        def result = service.setUserPicture("user-1", png(123, 456))

        then:
        !result.isError()
        auditRepository.entries.size() == 2
        with(auditRepository.entries.last()) {
            action == ProfilePictureAuditEntry.ACTION_REPLACE
            previousContentHash == firstHash
        }
    }

    def "rejects a picture upload with a missing acting user"() {
        when:
        def result = service.setUserPicture(input, png(200, 200))

        then:
        result.isError()
        result.getError().errorCode() == ErrorCode.GENERAL

        where:
        input << [null, "", "  "]
    }

    def "returns a validation error for an invalid image"() {
        when:
        def result = service.setUserPicture("user-1", "not an image".getBytes("UTF-8"))

        then:
        result.isError()
        result.getError().errorParameters().value().toList() == ["UNSUPPORTED_FORMAT"]
    }

    def "removes a user picture"() {
        given:
        service.setUserPicture("user-1", png(200, 200))

        when:
        def result = service.removeUserPicture("user-1")

        then:
        !result.isError()
        repository.findByOwnerTypeAndOwnerId(ProfilePictureOwnerType.USER, "user-1").isEmpty()
    }

    def "allows an authorized actor to set a group picture"() {
        when:
        def result = service.setGroupPicture("group-1", "manager", png(300, 200))

        then:
        !result.isError()
        repository.findByOwnerTypeAndOwnerId(ProfilePictureOwnerType.GROUP, "group-1").isPresent()
    }

    def "denies an unauthorized actor setting a group picture"() {
        when:
        def result = service.setGroupPicture("group-1", "member", png(300, 200))

        then:
        result.isError()
        result.getError().errorCode() == ErrorCode.ACCESS_DENIED
        repository.findByOwnerTypeAndOwnerId(ProfilePictureOwnerType.GROUP, "group-1").isEmpty()
    }

    def "only a system admin may force-remove a picture"() {
        given:
        service.setUserPicture("user-1", png(200, 200))

        when:
        def denied = service.forceRemove("user-1", ProfilePictureOwnerType.USER, "user-1")

        then:
        denied.isError()
        denied.getError().errorCode() == ErrorCode.ACCESS_DENIED
        repository.findByOwnerTypeAndOwnerId(ProfilePictureOwnerType.USER, "user-1").isPresent()

        when:
        def allowed = service.forceRemove("admin", ProfilePictureOwnerType.USER, "user-1")

        then:
        !allowed.isError()
        repository.findByOwnerTypeAndOwnerId(ProfilePictureOwnerType.USER, "user-1").isEmpty()
        and: "the owner's audit entries are removed too"
        auditRepository.entries.isEmpty()
    }

    def "the audit list is admin-only"() {
        given:
        service.setUserPicture("user-1", png(200, 200))
        Pageable pageable = PageRequest.of(0, 20)

        expect:
        service.listAudit("user-1", pageable).isError()
        !service.listAudit("admin", pageable).isError()
        service.listAudit("admin", pageable).getValue().size() == 1
    }

    private static byte[] png(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        Graphics2D graphics = image.createGraphics()
        graphics.setColor(Color.GREEN)
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    static class InMemoryProfilePictureRepository implements ProfilePictureRepository {

        Map<String, ProfilePictureRepository.ProfilePictureEntity> store = [:]

        private static String key(ProfilePictureOwnerType type, String id) {
            return "${type}:${id}"
        }

        @Override
        Optional<ProfilePictureRepository.ProfilePictureEntity> findByOwnerTypeAndOwnerId(
                ProfilePictureOwnerType ownerType, String ownerId) {
            return Optional.ofNullable(store[key(ownerType, ownerId)])
        }

        @Override
        Optional<ProfilePictureRepository.ContentHashProjection>
                findContentHashByOwnerTypeAndOwnerId(ProfilePictureOwnerType ownerType,
                        String ownerId) {
            return Optional.ofNullable(store[key(ownerType, ownerId)]).map { entity ->
                [getContentHash: { entity.contentHash }] as ProfilePictureRepository.ContentHashProjection
            }
        }

        @Override
        ProfilePictureRepository.ProfilePictureEntity save(
                ProfilePictureRepository.ProfilePictureEntity entity) {
            store[key(entity.ownerType, entity.ownerId)] = entity
            return entity
        }

        @Override
        void deleteByOwnerTypeAndOwnerId(ProfilePictureOwnerType ownerType, String ownerId) {
            store.remove(key(ownerType, ownerId))
        }
    }

    static class InMemoryAuditRepository implements ProfilePictureAuditRepository {

        List<ProfilePictureAuditRepository.ProfilePictureAuditEntity> entries = []

        @Override
        ProfilePictureAuditRepository.ProfilePictureAuditEntity save(
                ProfilePictureAuditRepository.ProfilePictureAuditEntity entity) {
            entries.add(entity)
            return entity
        }

        @Override
        Page<ProfilePictureAuditRepository.ProfilePictureAuditEntity> findAllByOrderByCreatedAtDesc(
                Pageable pageable) {
            return new PageImpl<>(entries.reverse(), pageable, entries.size())
        }

        @Override
        List<ProfilePictureAuditRepository.ProfilePictureAuditEntity>
                findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(
                        ProfilePictureOwnerType ownerType, String ownerId) {
            return entries.findAll {
                it.ownerType == ownerType && it.ownerId == ownerId
            }
        }

        @Override
        void deleteByOwnerTypeAndOwnerId(ProfilePictureOwnerType ownerType, String ownerId) {
            entries.removeAll { it.ownerType == ownerType && it.ownerId == ownerId }
        }
    }
}
