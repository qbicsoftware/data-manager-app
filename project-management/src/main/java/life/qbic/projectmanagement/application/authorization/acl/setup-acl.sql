CREATE TABLE acl_sid
(
    id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    principal BOOLEAN         NOT NULL,
    sid       VARCHAR(100)    NOT NULL,
    UNIQUE KEY unique_acl_sid (sid, principal)
) ENGINE = InnoDB;

CREATE TABLE acl_class
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class         VARCHAR(100)    NOT NULL,
    class_id_type VARCHAR(100)    NOT NULL,
    UNIQUE KEY uk_acl_class (class)
) ENGINE = InnoDB;

CREATE TABLE acl_object_identity
(
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    object_id_class    BIGINT UNSIGNED NOT NULL,
    object_id_identity VARCHAR(36)     NOT NULL,
    parent_object      BIGINT UNSIGNED,
    owner_sid          BIGINT UNSIGNED,
    entries_inheriting BOOLEAN         NOT NULL,
    UNIQUE KEY uk_acl_object_identity (object_id_class, object_id_identity),
    CONSTRAINT fk_acl_object_identity_parent FOREIGN KEY (parent_object) REFERENCES acl_object_identity (id),
    CONSTRAINT fk_acl_object_identity_class FOREIGN KEY (object_id_class) REFERENCES acl_class (id),
    CONSTRAINT fk_acl_object_identity_owner FOREIGN KEY (owner_sid) REFERENCES acl_sid (id)
) ENGINE = InnoDB;

CREATE TABLE acl_entry
(
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    acl_object_identity BIGINT UNSIGNED  NOT NULL,
    ace_order           INTEGER          NOT NULL,
    sid                 BIGINT UNSIGNED  NOT NULL,
    mask                INTEGER UNSIGNED NOT NULL,
    granting            BOOLEAN          NOT NULL,
    audit_success BOOLEAN NOT NULL DEFAULT true,
    audit_failure       BOOLEAN          NOT NULL,
    UNIQUE KEY unique_acl_entry (acl_object_identity, ace_order),
    CONSTRAINT fk_acl_entry_object FOREIGN KEY (acl_object_identity) REFERENCES acl_object_identity (id),
    CONSTRAINT fk_acl_entry_acl FOREIGN KEY (sid) REFERENCES acl_sid (id) ON DELETE CASCADE
) ENGINE = InnoDB;

INSERT INTO acl_class(id, class, class_id_type)
VALUES (1, 'life.qbic.projectmanagement.domain.model.project.Project', 'java.lang.String');
