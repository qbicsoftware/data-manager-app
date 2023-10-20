package life.qbic.datamanager.views.projects.project;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.MainComponent;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.controlling.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

/**
 * ProjectNavigationBarComponent
 * <p>
 * Allows the user to switch between the components shown in each {@link MainComponent} by clicking
 * on the corresponding button within the Navigation Bar which routes the user to the respective
 * route defined in {@link life.qbic.datamanager.views.AppRoutes} for the component in question
 */
@SpringComponent
@UIScope
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProjectNavigationBarComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = 2246439877362853798L;
  final UserPermissions userPermissions;

  NavigationButton projectInformationButton = NavigationButton.create(
      VaadinIcon.CLIPBOARD_CHECK.create(),
      "Project Information");
  NavigationButton experimentsButton = NavigationButton.create(VaadinIcon.SITEMAP.create(),
      "Experiments");
  NavigationButton samplesButton = NavigationButton.create(VaadinIcon.FILE_TABLE.create(),
      "Samples");
  NavigationButton dataButton = NavigationButton.create(VaadinIcon.CLOUD_DOWNLOAD.create(),
      "Raw Data");
  NavigationButton resultButton = NavigationButton.create(VaadinIcon.SEARCH.create(),
      "Results");
  NavigationButton accessButton = NavigationButton.create(VaadinIcon.GROUP.create(),
      "Access");


  public ProjectNavigationBarComponent(@Autowired UserPermissions userPermissions) {
    this.addClassName("navbar");
    this.add(projectInformationButton, experimentsButton, samplesButton, dataButton,
        resultButton);
    this.userPermissions = userPermissions;
  }

  public void setContext(Context context) {
    ProjectId projectId = context.projectId().orElseThrow();
    projectInformationButton.setButtonRoute(
        String.format(Projects.PROJECT_INFO, projectId.value()));
    context.experimentId().ifPresentOrElse(expId1 -> experimentsButton.setButtonRoute(
            String.format(Projects.EXPERIMENT, projectId.value(), expId1.value())),
        () -> experimentsButton.setButtonRoute(
            String.format(Projects.EXPERIMENTS, projectId.value())));
    samplesButton.setButtonRoute(String.format(Projects.SAMPLES, projectId.value()));
    if (userPermissions.changeProjectAccess(projectId)) {
      accessButton.setButtonRoute(String.format(Projects.ACCESS, projectId.value()));
      add(accessButton);
    } else {
      accessButton.removeFromParent();
    }
  }

  private static class NavigationButton extends Div {

    private final Div labelDiv = new Div();
    private final Button button = new Button();
    private String route = "";

    private NavigationButton(Icon icon, String label) {
      this.addClassName("navigation-button");
      labelDiv.add(label);
      labelDiv.addClassName("label");
      button.setIcon(icon);
      button.addClassName("button");
      routeOnButtonClick();
      this.add(button, labelDiv);
    }

    public static NavigationButton create(Icon icon, String label) {
      Objects.requireNonNull(icon);
      Objects.requireNonNull(label);
      return new NavigationButton(icon, label);
    }

    private void routeOnButtonClick() {
      button.addClickListener(
          ((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> getUI().ifPresentOrElse(
              it -> it.navigate(route),
              () -> {
                throw new ApplicationException(
                    String.format("Could not navigate to the defined route %s",
                        route));
              })));
      enableButtonForDefinedRoute(route);
    }

    public void setButtonRoute(String route) {
      this.route = route;
      enableButtonForDefinedRoute(route);
    }

    private void enableButtonForDefinedRoute(String route) {
      button.setEnabled(!route.isEmpty());
    }
  }
}
