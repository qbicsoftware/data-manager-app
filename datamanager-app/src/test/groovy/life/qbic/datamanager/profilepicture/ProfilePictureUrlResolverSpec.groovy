package life.qbic.datamanager.profilepicture

import spock.lang.Specification

class ProfilePictureUrlResolverSpec extends Specification {

  def cleanup() {
    // Reset the configured context path so other specs are unaffected by the static state.
    new ProfilePictureUrlResolver(null, "")
  }

  def "stablePath includes the configured context path when no request is active"() {
    given:
    new ProfilePictureUrlResolver(null, "/dev")

    when:
    String path = ProfilePictureUrlResolver.stablePath(ProfilePictureOwnerType.USER, "abc-123")

    then:
    path == "/dev/profile-pictures/user/abc-123"
  }

  def "stablePath omits the context path when none is configured"() {
    given:
    new ProfilePictureUrlResolver(null, "")

    when:
    String path = ProfilePictureUrlResolver.stablePath(ProfilePictureOwnerType.GROUP, "group-1")

    then:
    path == "/profile-pictures/group/group-1"
  }

  def "a trailing slash in the configured context path is normalised away"() {
    given:
    new ProfilePictureUrlResolver(null, "/dev/")

    when:
    String path = ProfilePictureUrlResolver.stablePath(ProfilePictureOwnerType.USER, "xyz")

    then:
    path == "/dev/profile-pictures/user/xyz"
  }

  def "a rooted context path is treated as no context path"() {
    given:
    new ProfilePictureUrlResolver(null, "/")

    when:
    String path = ProfilePictureUrlResolver.stablePath(ProfilePictureOwnerType.USER, "xyz")

    then:
    path == "/profile-pictures/user/xyz"
  }
}
