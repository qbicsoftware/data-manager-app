package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.Objects;
import life.qbic.datamanager.export.TempDirectory;
import life.qbic.datamanager.export.rocrate.ROCreateBuilder;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.projectmanagement.application.ContactRepository;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@UIScope
@SpringComponent
public class ProjectSummaryNewComponent extends PageArea {

  private final ProjectInformationService projectInformationService;
  private Context context;

  @Autowired
  public ProjectSummaryNewComponent(ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService,
      ContactRepository contactRepository,
      UserPermissions userPermissions,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      ROCreateBuilder rOCreateBuilder, TempDirectory tempDirectory) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
  }

  public void setContext(Context context) {
    this.context = Objects.requireNonNull(context);
  }
}
