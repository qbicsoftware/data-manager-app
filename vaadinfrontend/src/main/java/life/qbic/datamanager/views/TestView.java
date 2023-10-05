package life.qbic.datamanager.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Objects;
import java.util.StringJoiner;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@AnonymousAllowed
@Route("test")
public class TestView extends Div {

  public TestView() {

    MyBean bean1 = new MyBean();
    bean1.setName("tom");
    bean1.setEmail("test@test.de");
    MyBean bean2 = new MyBean();
    bean2.setName("jerry");
    bean2.setEmail("mampf@mouse.de");

    Spreadsheet<MyBean> spreadsheet = new Spreadsheet<>();
    spreadsheet.addColumn("value", MyBean::getName, MyBean::setName);
    spreadsheet.addRow(bean1);
    spreadsheet.addColumn("email", MyBean::getEmail, MyBean::setEmail);
    spreadsheet.addRow(bean2);
    add(spreadsheet, new Button("get rows", click -> System.out.println(spreadsheet.getRows())));
  }

  public static class MyBean {

    private String name = "";
    private String email = "";

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", MyBean.class.getSimpleName() + "[", "]")
          .add("name='" + name + "'")
          .add("email='" + email + "'")
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      MyBean myBean = (MyBean) o;

      if (!Objects.equals(name, myBean.name)) {
        return false;
      }
      return Objects.equals(email, myBean.email);
    }

    @Override
    public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (email != null ? email.hashCode() : 0);
      return result;
    }
  }

}
