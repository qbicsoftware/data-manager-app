package life.qbic.domain.user;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FullName {

  private String fullName;

  public static FullName from(String s) {
    var fullName = new FullName();
    fullName.setFullName(s);
    return fullName;
  }

  private FullName() {
    super();
  }

  private void setFullName(String s) {
    this.fullName = s;
  }

  public String name() {
    return fullName;
  }

}
