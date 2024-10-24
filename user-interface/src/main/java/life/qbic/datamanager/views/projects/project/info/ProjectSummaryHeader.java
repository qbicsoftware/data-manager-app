package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.html.Div;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import life.qbic.datamanager.views.account.UserAvatar.UserAvatarGroupItem;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.section.ControlElements;
import life.qbic.projectmanagement.application.ProjectOverview.UserInfo;

/**
 * <b>Project Summary Header</b>
 *
 * <p>
 * Holds some high level information about a project such as:
 * <ul>
 *   <li>Project ID and Title</li>
 *   <li>Last modified timestamp</li>
 *   <li>Shared users</li>
 *   <li>Project tags (Genomics, Proteomics, ...)</li>
 * </ul>
 *
 * @since 1.6.0
 */
public class ProjectSummaryHeader extends Div implements ControlElements {

  private static final String DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm";

  private final Div title;

  private final Div lastModified;

  private final AvatarGroup avatarGroup;

  private final Div projectTags;

  public ProjectSummaryHeader() {
    addClassName("project-summary-header");

    title = new Div();
    title.setClassName("title");
    title.setText("Default title");

    lastModified = new Div();
    lastModified.setClassName("last-modified");
    lastModified.setText(formatDate(Instant.now()));

    avatarGroup = new AvatarGroup();
    avatarGroup.setClassName("avatar-group");

    projectTags = new Div();
    projectTags.setClassName("project-tags");

    add(title);
    add(lastModified);
    add(avatarGroup);
    add(projectTags);
  }

  private String formatDate(Instant date) {
    var formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN).withZone(ZoneId.systemDefault());
    return formatter.format(date);
  }

  public void setTitle(String title) {
    this.title.setText(title);
  }

  public void setModificationDate(Instant date) {
    var text = "Last modified on %s".formatted(formatDate(date));
    lastModified.setText(text);
  }


  @Override
  public void enableControls() {

  }

  @Override
  public void disableControls() {

  }

  @Override
  public boolean controlsEnabled() {
    return false;
  }

  @Override
  public boolean controlsDisabled() {
    return false;
  }

  public void setUserAccessInfo(Collection<UserInfo> userInfos) {
    avatarGroup.getItems().forEach(avatarGroup::remove);
    userInfos.forEach(userInfo -> avatarGroup.add(new UserAvatarGroupItem(userInfo.userName(),
        userInfo.userName())));
  }

  public void setTags(List<Tag> tags) {
    projectTags.removeAll();
    tags.forEach(projectTags::add);
  }
}
