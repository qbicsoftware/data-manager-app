package life.qbic.datamanager.announcements;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import life.qbic.datamanager.announcements.AnnouncementRepository.Announcement;
import org.springframework.data.repository.Repository;

@org.springframework.stereotype.Repository
public interface AnnouncementRepository extends Repository<Announcement, Long> {

  @Entity
  @Table(name = "announcements")
  class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "start_time")
    private Instant displayStartTime;
    @Column(name = "end_time")
    private Instant displayEndTime;
    @Column(name = "message")
    private String message;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getMessage() {
      return message;
    }
  }

  List<Announcement> getAnnouncementByDisplayStartTimeBeforeAndDisplayEndTimeAfterOrderByDisplayStartTimeAsc(
      Instant min,
      Instant max);


}
