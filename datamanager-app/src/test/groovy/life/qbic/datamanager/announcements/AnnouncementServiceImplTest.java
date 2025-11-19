package life.qbic.datamanager.announcements;

import static java.lang.Thread.sleep;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnouncementServiceImplTest {

  static class MockRepo implements AnnouncementRepository {

    static List<Announcement> announcements = new ArrayList<>();

    public MockRepo() {
      Instant time = Instant.now().plus(Duration.ofMillis(50));
      announcements.add(0,
          new Announcement(0L, time.minus(Duration.ofDays(1)), time.minus(Duration.ofMillis(50)),
              "An announcement -1d to -50ms")
      );
      announcements.add(0,
          new Announcement(0L, time.minus(Duration.ofDays(1)), time.plus(Duration.ofMillis(200)),
              "An announcement -1d to +50ms")
      );
      announcements.add(0,
          new Announcement(0L, time.plus(Duration.ofMillis(0)), time.plus(Duration.ofMillis(250)),
              "An announcement 0s to +50ms")
      );
      announcements.add(0,
          new Announcement(0L, time.plus(Duration.ofMillis(50)), time.plus(Duration.ofMillis(300)),
              "An announcement +50ms to +100ms")
      );
      announcements.add(0,
          new Announcement(0L, time.plus(Duration.ofMillis(50)), time.plus(Duration.ofMillis(300)),
              "An announcement +50ms to +200ms")
      );
    }

    @Override
    public List<Announcement> getAnnouncementByDisplayStartTimeBeforeAndDisplayEndTimeAfterOrderByDisplayStartTimeAsc(
        Instant min, Instant max) {
      return announcements.stream()
          .filter(it -> it.getDisplayStartTime().isBefore(min))
          .filter(it -> it.getDisplayEndTime().isAfter(max))
          .toList();
    }

  }


  @Test
  public void testHotAnnouncementFlux() throws InterruptedException {
    AnnouncementRepository repo = new MockRepo();
    AnnouncementServiceImpl announcementService = new AnnouncementServiceImpl(
        repo,
        Duration.ZERO,
        Duration.ofMillis(15)
    );
    System.out.println("starting subscription");
    var s1 = announcementService.activeAnnouncements().subscribe(
        it -> System.out.println("s1: " + it));
    System.out.println("subscribed s1");
    sleep(15);
    var s2 = announcementService.activeAnnouncements().subscribe(
        it -> System.out.println("s2: " + it));
    System.out.println("subscribed s2");
    sleep(50);
    s1.dispose();
    System.out.println("unsubscribed s1");
    sleep(500);
    s2.dispose();

    System.out.println("unsubscribed s2");
  }

}
