package life.qbic.datamanager.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet.Column.ColumnValidator.ValidationResult;

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

  enum EMAIL {
    T_KOCH("tkoch-email"),
    QBIC_SOFTWARE("qbic-email");

    private final String address;

    EMAIL(String address) {
      this.address = address;
    }

    public String getAddress() {
      return address;
    }
  }

  public TestView() {
    MyBean bean1 = new MyBean();
    bean1.setName("tom");
    bean1.setEmail("test@test.de");
    MyBean bean2 = new MyBean();
    bean2.setName("jerry");
    bean2.setEmail("mampf@mouse.de");

    Spreadsheet<MyBean> spreadsheet = new Spreadsheet<>();
    spreadsheet.addColumn("value", MyBean::getName, MyBean::setName);
    spreadsheet.addColumn("email", MyBean::getEmail, MyBean::setEmail)
        .selectFrom(Arrays.stream(EMAIL.values()).toList(), EMAIL::getAddress)
        .setRequired(true);

    spreadsheet.addRow(bean1);
    spreadsheet.addRow(bean2);
    for (int i = 0; i < 50; i++) {
      spreadsheet.addRow(new MyBean());
    }
    Text text = new Text("");
    add(spreadsheet);
    add(new Button("get rows", click -> System.out.println(spreadsheet.getRows())));
    add(new Button("validates?", click -> {
      ValidationResult validationResult = spreadsheet.validate();
      text.setText(validationResult.isValid() ? "Good job!" : validationResult.errorMessage());
    }));
    add(text);
  }

  public static class MyBean {

    private String name;
    private String email;

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
