package life.qbic.apps.datamanager.notifications;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class Exchange {

  private static Exchange instance;

  public static Exchange instance() {
    if (instance == null) {
      instance = new Exchange();
    }
    return instance;
  }

  protected Exchange() {
    super();
  }

  public void submit(Notification notification){}

  public void subscribe(NotificationSubscriber subscriber, String notificationType){}

}
