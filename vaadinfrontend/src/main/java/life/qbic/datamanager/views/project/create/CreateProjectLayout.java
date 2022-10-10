package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Create Project")
@Route(value = "projects/create", layout = MainLayout.class)
@PermitAll
@Tag("create-project")
@CssImport("./styles/components/create-project.css")
public class CreateProjectLayout extends Composite<HorizontalLayout> implements
    HasUrlParameter<String> {

  final ProjectInformationLayout projectInformationLayout;
  final ProjectLinksLayout projectLinksLayout;

  final CreateProjectHandlerInterface handler;

  public CreateProjectLayout(@Autowired ProjectInformationLayout projectInformationLayout,
      @Autowired ProjectLinksLayout projectLinksLayout,
      @Autowired CreateProjectHandlerInterface handler) {
    Objects.requireNonNull(handler);
    Objects.requireNonNull(projectInformationLayout);
    Objects.requireNonNull(projectLinksLayout);

    this.projectInformationLayout = projectInformationLayout;
    this.projectLinksLayout = projectLinksLayout;
    getContent().add(projectInformationLayout, projectLinksLayout);
    this.handler = handler;
    this.handler.handle(this);
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
    handler.handleEvent(beforeEvent);
  }
}
