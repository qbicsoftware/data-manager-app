package life.qbic.usergroups.api;

import java.io.Serializable;

/**
 * <b>Group information DTO</b>
 *
 * <p>Public-directory projection of a user group for cross-context clients. By design exposes
 * <b>only</b> the group's identity, name, description and type — never membership information
 * (visibility policy: group members are never exposed to non-members).</p>
 *
 * @param id          the group id (stable, never reused across soft-dissolved groups)
 * @param name        the group's display name
 * @param description the group's description, may be {@code null} if none was set
 * @param type        the group type (ORG / ADHOC)
 * @since 1.17.0
 */
public record GroupInfo(String id, String name, String description, GroupType type) implements
    Serializable {

}