package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Span;
import java.util.Objects;

/**
 * A compact, monospace badge for displaying a project code alongside a project title or in other
 * contexts where the code needs to be visually distinguished from normal text without competing for
 * attention.
 *
 * <p>The badge uses a monospace font and a subtle contrast background so users learn to scan for it
 * as an identifier — the same convention used by GitHub, Jira and GitLab for commit hashes, issue
 * keys and similar codes. It is intentionally less prominent than a {@link Tag} so it does not
 * compete with measurement-type or status tags on the same card.
 *
 * @since 1.19.0
 */
public class ProjectCodeBadge extends Span {

  public ProjectCodeBadge(String projectCode) {
    super(Objects.requireNonNull(projectCode, "projectCode cannot be null"));
    addClassName("project-code-badge");
  }
}
