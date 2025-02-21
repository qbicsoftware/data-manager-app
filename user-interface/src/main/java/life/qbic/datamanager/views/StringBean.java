package life.qbic.datamanager.views;

public class StringBean {

  private String value;

  public StringBean() {
    this.value = "";
  }

  public StringBean(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
