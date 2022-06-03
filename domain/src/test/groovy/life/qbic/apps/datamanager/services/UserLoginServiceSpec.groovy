package life.qbic.apps.datamanager.services


import life.qbic.apps.datamanager.notifications.MessageBusInterface
import life.qbic.apps.datamanager.notifications.NotificationService
import life.qbic.domain.usermanagement.User
import life.qbic.domain.usermanagement.repository.UserRepository
import spock.lang.Specification

class UserLoginServiceSpec extends Specification {

  NotificationService notificationService = new NotificationService(Mock(MessageBusInterface))
  UserLoginService service = new UserLoginService(new ActiveUserRepositoryMock())

  def "login with wrong credentials does not work"() {
    expect:
    !service.login(username, password)
    where:
    username                     | password
    "wrongUsername"              | "wrongPassword"
    "wrongUsername"              | "correctPassword"
    "correct-username@qbic.life" | "wrongPassword"
  }

  def "login with correct credentials does not work for inactive users"() {
    given:
    service = new UserLoginService(new InactiveUserRepositoryMock())
    expect:
    !service.login("correct-username@qbic.life", "correctPassword")
  }

  def "login with correct credentials does work"() {
    given:
    service = new UserLoginService(new ActiveUserRepositoryMock())
    expect:
    service.login("correct-username@qbic.life", "correctPassword")
  }

  private static class ActiveUserRepositoryMock extends UserRepository {

    ActiveUserRepositoryMock() {
      super(null)
    }

    @Override
    Optional<User> findByEmail(String email) throws RuntimeException {
      if (email.equals("correct-username@qbic.life")) {
        def user = User.create("Test", "correct-username@qbic.life")
        user.setPassword("correctPassword".toCharArray())
        user.active = true
        return Optional.of(user)
      }
      return Optional.empty()
    }
  }

  private static class InactiveUserRepositoryMock extends UserRepository {

    InactiveUserRepositoryMock() {
      super(null)
    }

    @Override
    Optional<User> findByEmail(String email) throws RuntimeException {
      if (email.equals("correct-username@qbic.life")) {
        def user = User.create("Test", "correct-username@qbic.life")
        user.setPassword("correctPassword".toCharArray())
        user.active = false
        return Optional.of(user)
      }
      return Optional.empty()
    }
  }
}
