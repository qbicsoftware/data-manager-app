package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.datamanager.views.layouts.CardLayout;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
@SpringComponent
@UIScope
public class ProjectNavigationBarComponent extends Composite<CardLayout> {

  private static final long serialVersionUID = 2246439877362853798L;
  private final transient Handler handler;
  private HorizontalLayout navigationBarLayout;
  private Button projectInformationButton;
  private Button experimentalDesignButton;
  private Button samplesButton;
  private Button rawDataButton;
  private Button resultsButton;

  public ProjectNavigationBarComponent() {
    this.handler = new Handler();
    initNavigationBar();
  }

  private void initNavigationBar() {
    navigationBarLayout = new HorizontalLayout();
    projectInformationButton = new Button(VaadinIcon.CLIPBOARD_CHECK.create());
    experimentalDesignButton = new Button(VaadinIcon.TREE_TABLE.create());
    samplesButton = new Button(VaadinIcon.SITEMAP.create());
    rawDataButton = new Button(VaadinIcon.CLOUD_DOWNLOAD.create());
    resultsButton = new Button(VaadinIcon.SEARCH.create());
    navigationBarLayout.add(projectInformationButton, experimentalDesignButton, samplesButton,
        rawDataButton, resultsButton);
  }


  //ToDo Initialize Transition between Different Subpages of ProjectViewPage
  private final class Handler {

  }
}
