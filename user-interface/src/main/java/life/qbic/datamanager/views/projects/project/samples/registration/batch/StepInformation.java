package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;


record StepInformation(Component title, Component body, boolean highlighted) {

  StepInformation {
    title.addClassName("title");
    body.addClassName("body");
  }

  public Component asComponent() {
    Div div = new Div(title, body);
    div.addClassName("content-part");
    div.getElement().setAttribute("highlighted", highlighted);
    return div;
  }
}
