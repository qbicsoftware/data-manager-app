package life.qbic.apps.datamanager.services


import life.qbic.apps.datamanager.notifications.MessageBusInterface
import life.qbic.apps.datamanager.notifications.NotificationService
import life.qbic.domain.usermanagement.User
import life.qbic.domain.usermanagement.repository.UserRepository
import spock.lang.Specification

class UserLoginServiceSpec extends Specification {

  NotificationService notificationService = new NotificationService(Mock(MessageBusInterface))
  UserLoginService service = new UserLoginService(new UserRepositoryMock())

  def "login with wrong credentials does not work"() {
    expect:
    !service.login(username, password)
    where:
    username                     | password
    "wrongUsername"              | "wrongPassword"
    "wrongUsername"              | "correctPassword"
    "correct-username@qbic.life" | "wrongPassword"

  }

  def "login with correct credentials does work"() {
    expect:
    service.login("correct-username@qbic.life", "correctPassword")
  }

  private static class UserRepositoryMock extends UserRepository {

    UserRepositoryMock() {
      super(null)
    }

    @Override
    Optional<User> findByEmail(String email) throws RuntimeException {
      if (email.equals("correct-username@qbic.life")) {
        def user = User.create("Test", "correct-username@qbic.life")
        user.setPassword("correctPassword".toCharArray())
        return Optional.of(user)
      }
      return Optional.empty()
    }
  }
}
