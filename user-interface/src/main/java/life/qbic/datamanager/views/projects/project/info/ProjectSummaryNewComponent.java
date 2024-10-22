package life.qbic.datamanager.views.projects.project.info;

import static life.qbic.datamanager.views.MeasurementType.GENOMICS;
import static life.qbic.datamanager.views.MeasurementType.PROTEOMICS;

import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.export.TempDirectory;
import life.qbic.datamanager.export.rocrate.ROCreateBuilder;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.TagFactory;
import life.qbic.datamanager.views.account.UserAvatar.UserAvatarGroupItem;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.section.Header;
import life.qbic.datamanager.views.general.section.Header.Size;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.Section.SectionBuilder;
import life.qbic.datamanager.views.general.section.SectionContent;
import life.qbic.datamanager.views.general.section.SubHeader;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.projectmanagement.application.ContactRepository;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
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

  private static final String DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm";

  private final ProjectInformationService projectInformationService;
  private final ProjectSummaryHeader projectSummaryHeader;
  private Section headerSection;
  private Context context;

  @Autowired
  public ProjectSummaryNewComponent(ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService,
      ContactRepository contactRepository,
      UserPermissions userPermissions,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      ROCreateBuilder rOCreateBuilder, TempDirectory tempDirectory) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.projectSummaryHeader = new ProjectSummaryHeader();
    this.headerSection = new SectionBuilder().build();
    add(projectSummaryHeader);
    add(headerSection);
  }

  private String formatDate(Instant date) {
    var formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN).withZone(ZoneId.systemDefault());
    return formatter.format(date);
  }

  public void setContext(Context context) {
    this.context = Objects.requireNonNull(context);
    var projectId = context.projectId()
        .orElseThrow(() -> new ApplicationException("No project id provided"));
    var project = projectInformationService.findOverview(projectId)
        .orElseThrow(() -> new ApplicationException("No project with given ID found"));
    setContent(project);
  }

  private void setContent(ProjectOverview projectInformation) {
    Objects.requireNonNull(projectInformation);
    projectSummaryHeader.setTitle("%s - %s".formatted(projectInformation.projectCode(),
        projectInformation.projectTitle()));
    projectSummaryHeader.setModificationDate(projectInformation.lastModified());
    //projectSummaryHeader.setUserAccessInfo(projectInformation.collaboratorUserInfos());
    buildHeader(projectInformation);

  }

  private void buildHeader(ProjectOverview projectOverview) {
    Objects.requireNonNull(projectOverview);
    headerSection.setHeader(new Header("%s - %s".formatted(projectOverview.projectCode(),
        projectOverview.projectTitle()), Size.LARGE));
    headerSection.setSubHeader(new SubHeader("Last modified on %s".formatted(formatDate(projectOverview.lastModified()))));
    var sectionContent = new SectionContent();
    sectionContent.add(userInfoTagLine(projectOverview));
    headerSection.setContent(sectionContent);
  }

  private Div userInfoTagLine(ProjectOverview projectOverview) {
    var content = new Div();
    content.addClassName("inline-elements");
    var avatarGroup = new AvatarGroup();
    var userInfos = projectOverview.collaboratorUserInfos();
    avatarGroup.getItems().forEach(avatarGroup::remove);
    userInfos.forEach(userInfo -> avatarGroup.add(new UserAvatarGroupItem(userInfo.userName(),
        userInfo.userName())));
    avatarGroup.setMaxItemsVisible(3);
    content.add(avatarGroup);
    buildTags(projectOverview).forEach(content::add);
    return content;
  }

  private List<Tag> buildTags(ProjectOverview projectInformation) {
    var tags = new ArrayList<Tag>();
    if (projectInformation.ngsMeasurementCount() != null) {
      tags.add(TagFactory.forMeasurement(GENOMICS));
    }
    if (projectInformation.pxpMeasurementCount() != null) {
      tags.add(TagFactory.forMeasurement(PROTEOMICS));
    }
    return tags;
  }
}
