INSERT INTO roles(id, name, description)
VALUES (1, 'ADMIN', 'Full administration of the application'),
       (2, 'USER', 'Standard user of the application'),
       (3, 'PROJECT_MANAGER', 'Manages projects at QBiC')
;

INSERT INTO permissions(id, name, description)
VALUES (1, 'acl:change-owner',
        ' Permits to change the owner of an entity in the access control list'),
       (2, 'acl:change-audit', 'Permits changing the auditing of an access control entry'),
       (3, 'acl:change-access',
        'Permits creating and modifying and removing access control entries'),
       (4, 'project:create', ' Permits creating of projects')
;

INSERT INTO role_permission(userRoleId, permissionId)
VALUES (1, 1), # admin can change ownership on entities
       (1, 2), # admin can change audit setting on entities
       (1, 3), # admin can create, modify and remove access control entries
       (1, 4), # admin can create projects
       (2, 3), # user can create, modify and remove access control entries
       (3, 3), # project manager can create, modify and remove access control entries
       (3, 4) # project manager can create projects
;
