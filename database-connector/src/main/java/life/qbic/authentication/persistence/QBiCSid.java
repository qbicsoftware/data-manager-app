package life.qbic.authentication.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Entity
@Table(name = "acl_sid")
public class QBiCSid {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  protected long id;
  @Column(name = "principal")
  protected boolean principal;

  @Column(name = "sid")
  protected String sid;

  public QBiCSid(boolean principal, String sid) {
    this.principal = principal;
    this.sid = sid;
  }

  protected QBiCSid() {

  }

  public String getSid() {
    return sid;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    QBiCSid qBiCSid = (QBiCSid) object;

    if (id != qBiCSid.id) {
      return false;
    }
    if (principal != qBiCSid.principal) {
      return false;
    }
    return sid.equals(qBiCSid.sid);
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (principal ? 1 : 0);
    result = 31 * result + sid.hashCode();
    return result;
  }
}
